package cm.kmerfret.backend.service;

import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.*;
import cm.kmerfret.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MissionRepository    missionRepo;
    private final PlatformWalletRepository walletRepo;
    private final NotificationRepository   notifRepo;
    private final UserRepository       userRepo;
    private final StripeService        stripeService;
    private final MonetbillService     monetbillService;
    private final PushNotificationService  pushService;
    private final EmailService         emailService;

    @org.springframework.beans.factory.annotation.Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Transactional
    public Map<String, String> initiateStripePayment(UUID missionId) {
        Mission m = findMission(missionId);
        BigDecimal amount = m.getFirstPaymentAmount() != null ? m.getFirstPaymentAmount() : m.getTotalPrice();
        StripeService.StripePaymentResult result = stripeService.createPaymentIntent(amount, missionId.toString());

        walletRepo.save(PlatformWallet.builder()
                .mission(m).payer(m.getImporter()).amount(amount)
                .transactionType(PlatformWallet.TransactionType.DEPOSIT)
                .paymentMethod("STRIPE").externalRef(result.paymentIntentId())
                .status(PlatformWallet.TxStatus.PENDING).build());

        // En mode sandbox, auto-confirmer après 5 secondes
        if (isSandboxMode()) {
            final UUID finalMissionId = missionId;
            final String piId = result.paymentIntentId();
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    log.info("[SANDBOX] Auto-confirmation Stripe {} pour mission {}", piId, finalMissionId);
                    confirmPayment(finalMissionId, piId, "STRIPE");
                } catch (Exception e) {
                    log.warn("[SANDBOX] Erreur auto-confirmation Stripe: {}", e.getMessage());
                }
            }).start();
        }

        return Map.of("clientSecret", result.clientSecret(), "paymentIntentId", result.paymentIntentId());
    }

    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        var event = stripeService.validateWebhook(payload, sigHeader);
        if ("payment_intent.succeeded".equals(event.getType())) {
            var intent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
            String missionId = intent.getMetadata().get("missionId");
            if (missionId != null) confirmPayment(UUID.fromString(missionId), intent.getId(), "STRIPE");
        }
    }

    @Transactional
    public Map<String, String> initiateMonetbillPayment(UUID missionId, String phoneNumber) {
        Mission m = findMission(missionId);
        BigDecimal amount = m.getFirstPaymentAmount() != null ? m.getFirstPaymentAmount() : m.getTotalPrice();

        MonetbillService.MonetbillInitResult result = monetbillService.initiatePayment(
                phoneNumber, amount, missionId.toString(),
                "Mission KmerFret " + m.getOriginLabel() + " -> " + m.getDestinationLabel());

        String method = phoneNumber.startsWith("+23769") || phoneNumber.startsWith("23769") ? "MTN_MOMO" : "ORANGE_MONEY";
        String ref = result.paymentRef() != null ? result.paymentRef() : "";

        // En mode sandbox, générer un ref local si Monetbill n'a pas répondu
        if (isSandboxMode() && ref.isEmpty()) {
            ref = "SANDBOX-MOMO-" + System.currentTimeMillis();
            log.info("[SANDBOX] Monetbill indisponible, ref local généré: {}", ref);
        }

        walletRepo.save(PlatformWallet.builder()
                .mission(m).payer(m.getImporter()).amount(amount)
                .transactionType(PlatformWallet.TransactionType.DEPOSIT)
                .paymentMethod(method).externalRef(ref)
                .status(PlatformWallet.TxStatus.PENDING).build());

        // En mode sandbox, auto-confirmer après 5 secondes (garanti même si Monetbill échoue)
        if (isSandboxMode()) {
            final String finalRef = ref;
            final UUID finalMissionId = missionId;
            final String finalMethod = method;
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    log.info("[SANDBOX] Auto-confirmation MoMo {} pour mission {}", finalRef, finalMissionId);
                    confirmPayment(finalMissionId, finalRef, finalMethod);
                } catch (Exception e) {
                    log.warn("[SANDBOX] Erreur auto-confirmation MoMo: {}", e.getMessage());
                }
            }).start();
        }

        if (result.paymentUrl() != null)
            return Map.of("paymentRef", ref, "paymentUrl", result.paymentUrl());
        return Map.of("paymentRef", ref);
    }

    private boolean isSandboxMode() {
        return stripeSecretKey != null && stripeSecretKey.contains("_test_");
    }

    @Transactional
    public void handleMonetbillWebhook(String payload, String signature, Map<String, String> params) {
        if (!monetbillService.validateWebhook(payload, signature)) throw new RuntimeException("Signature invalide");
        String ref = params.get("payment_ref");
        if ("SUCCESSFUL".equals(params.get("status")) && ref != null) {
            walletRepo.findByExternalRef(ref).ifPresent(w ->
                    confirmPayment(w.getMission().getId(), ref, w.getPaymentMethod()));
        }
    }

    // ─── Initiation paiement du solde (2ème tranche) — Stripe ───────────────

    @Transactional
    public Map<String, String> initiateSecondPaymentStripe(UUID missionId) {
        Mission m = findMission(missionId);
        if (m.getStatus() != Mission.MissionStatus.DELIVERED)
            throw new IllegalStateException("La mission doit être LIVRÉE pour payer le solde.");
        if (m.getSecondPaymentAt() != null)
            throw new IllegalStateException("Le solde a déjà été payé.");
        if (m.getPaymentStatus() == Mission.PaymentStatus.RELEASED)
            throw new IllegalStateException("Les fonds ont déjà été libérés.");

        BigDecimal solde = m.getFirstPaymentAmount() != null
                ? m.getTotalPrice().subtract(m.getFirstPaymentAmount())
                : m.getTotalPrice();

        StripeService.StripePaymentResult result = stripeService.createPaymentIntent(solde, missionId.toString() + "_solde");

        walletRepo.save(PlatformWallet.builder()
                .mission(m).payer(m.getImporter()).amount(solde)
                .transactionType(PlatformWallet.TransactionType.DEPOSIT)
                .paymentMethod("STRIPE").externalRef(result.paymentIntentId())
                .status(PlatformWallet.TxStatus.PENDING).build());

        if (isSandboxMode()) {
            final UUID fId = missionId;
            final String piId = result.paymentIntentId();
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    log.info("[SANDBOX] Auto-confirmation Stripe solde {} pour mission {}", piId, fId);
                    confirmSecondPayment(fId, piId, "STRIPE");
                } catch (Exception e) {
                    log.warn("[SANDBOX] Erreur auto-confirmation solde Stripe: {}", e.getMessage());
                }
            }).start();
        }

        return Map.of("clientSecret", result.clientSecret(), "paymentIntentId", result.paymentIntentId());
    }

    // ─── Initiation paiement du solde (2ème tranche) — Mobile Money ─────────

    @Transactional
    public Map<String, String> initiateSecondPaymentMonetbill(UUID missionId, String phoneNumber) {
        Mission m = findMission(missionId);
        if (m.getStatus() != Mission.MissionStatus.DELIVERED)
            throw new IllegalStateException("La mission doit être LIVRÉE pour payer le solde.");
        if (m.getSecondPaymentAt() != null)
            throw new IllegalStateException("Le solde a déjà été payé.");

        BigDecimal solde = m.getFirstPaymentAmount() != null
                ? m.getTotalPrice().subtract(m.getFirstPaymentAmount())
                : m.getTotalPrice();

        MonetbillService.MonetbillInitResult result = monetbillService.initiatePayment(
                phoneNumber, solde, missionId.toString() + "_solde",
                "Solde mission KmerFret " + m.getOriginLabel() + " -> " + m.getDestinationLabel());

        String method = phoneNumber.startsWith("+23769") || phoneNumber.startsWith("23769") ? "MTN_MOMO" : "ORANGE_MONEY";
        String ref = result.paymentRef() != null ? result.paymentRef() : "";

        // Sandbox : générer un ref local si Monetbill indisponible
        if (isSandboxMode() && ref.isEmpty()) {
            ref = "SANDBOX-SOLDE-" + System.currentTimeMillis();
            log.info("[SANDBOX] Monetbill indisponible pour le solde, ref local: {}", ref);
        }

        walletRepo.save(PlatformWallet.builder()
                .mission(m).payer(m.getImporter()).amount(solde)
                .transactionType(PlatformWallet.TransactionType.DEPOSIT)
                .paymentMethod(method).externalRef(ref)
                .status(PlatformWallet.TxStatus.PENDING).build());

        if (isSandboxMode()) {
            final String finalRef = ref;
            final UUID fId = missionId;
            final String finalMethod = method;
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    log.info("[SANDBOX] Auto-confirmation solde {} pour mission {}", finalRef, fId);
                    confirmSecondPayment(fId, finalRef, finalMethod);
                } catch (Exception e) {
                    log.warn("[SANDBOX] Erreur auto-confirmation solde: {}", e.getMessage());
                }
            }).start();
        }

        if (result.paymentUrl() != null)
            return Map.of("paymentRef", ref, "paymentUrl", result.paymentUrl());
        return Map.of("paymentRef", ref);
    }

    // ─── Confirmation paiement du solde (2ème tranche) ───────────────────────

    @Transactional
    public void confirmSecondPayment(UUID missionId, String ref, String method) {
        Mission m = findMission(missionId);
        if (m.getStatus() != Mission.MissionStatus.DELIVERED)
            throw new IllegalStateException("La mission doit être LIVRÉE pour payer le solde.");
        if (m.getPaymentStatus() == Mission.PaymentStatus.RELEASED)
            throw new IllegalStateException("Les fonds ont déjà été libérés.");

        m.setPaymentStatus(Mission.PaymentStatus.ESCROWED);
        m.setSecondPaymentAt(OffsetDateTime.now());
        if (ref != null) m.setPaymentReference(m.getPaymentReference() + "|solde:" + ref);
        missionRepo.save(m);

        BigDecimal solde = m.getFirstPaymentAmount() != null
                ? m.getTotalPrice().subtract(m.getFirstPaymentAmount())
                : m.getTotalPrice();
        walletRepo.save(PlatformWallet.builder()
                .mission(m).payer(m.getImporter()).amount(solde)
                .transactionType(PlatformWallet.TransactionType.DEPOSIT)
                .paymentMethod(method).externalRef(ref)
                .status(PlatformWallet.TxStatus.CONFIRMED).build());

        notify(m.getImporter(), "KmerFret — Solde confirmé",
                "Votre paiement du solde a été reçu. Les fonds seront versés au transporteur sous 24h.",
                "SECOND_PAYMENT_CONFIRMED", missionId.toString());
        if (m.getDriver() != null)
            notify(m.getDriver(), "KmerFret — Paiement complet reçu",
                    "Le client a payé le solde. Vous recevrez votre paiement complet sous 24h.",
                    "SECOND_PAYMENT_CONFIRMED", missionId.toString());

        log.info("[PAYMENT] Solde confirmé pour mission {} ref={}", missionId, ref);
    }

    // ─── Libération des fonds au transporteur (admin, dans les 24h) ──────────

    @Transactional
    public void releaseFunds(UUID missionId) {
        Mission m = findMission(missionId);
        if (m.getPaymentStatus() == Mission.PaymentStatus.RELEASED)
            throw new IllegalStateException("Les fonds ont déjà été libérés.");
        if (m.getPaymentStatus() == Mission.PaymentStatus.DISPUTED)
            throw new IllegalStateException("Résolvez d'abord le litige avant de libérer les fonds.");
        if (m.getStatus() != Mission.MissionStatus.DELIVERED)
            throw new IllegalStateException("La mission doit être LIVRÉE.");
        if (m.getDeliveredAt() == null)
            throw new IllegalStateException("Date de livraison manquante.");
        if (m.getSecondPaymentAt() == null && m.getPaymentStatus() == Mission.PaymentStatus.ESCROWED)
            throw new IllegalStateException("Le paiement du solde n'a pas encore été reçu.");

        // Règle métier : libérer dans les 24h suivant la livraison
        if (OffsetDateTime.now().isAfter(m.getDeliveredAt().plusHours(24))) {
            log.warn("[PAYMENT] Libération avec retard (>24h) pour mission {}", missionId);
        }

        // Calcul du payout net après commission
        BigDecimal total = m.getTotalPrice() != null ? m.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal commRate = m.getCommissionRate() != null ? m.getCommissionRate() : new BigDecimal("0.075");
        BigDecimal commission = total.multiply(commRate).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal payout = total.subtract(commission);

        m.setPaymentStatus(Mission.PaymentStatus.RELEASED);
        missionRepo.save(m);

        // Enregistrer la commission
        walletRepo.save(PlatformWallet.builder().mission(m).amount(commission)
                .transactionType(PlatformWallet.TransactionType.COMMISSION)
                .status(PlatformWallet.TxStatus.CONFIRMED)
                .notes("Commission " + (commRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros()) + "%")
                .build());

        // Enregistrer le virement au transporteur
        walletRepo.save(PlatformWallet.builder().mission(m).amount(payout)
                .transactionType(PlatformWallet.TransactionType.RELEASE)
                .status(PlatformWallet.TxStatus.CONFIRMED).build());

        if (m.getDriver() != null) {
            notify(m.getDriver(),
                    "KmerFret — Paiement viré",
                    String.format("Votre paiement de %,.0f FCFA (après commission %.1f%%) a été transféré.",
                            payout, commRate.multiply(BigDecimal.valueOf(100)).doubleValue()),
                    "PAYMENT_RELEASED", missionId.toString());
        }
        notify(m.getImporter(), "KmerFret — Transaction finalisée",
                "Les fonds ont été versés au transporteur. Mission terminée.",
                "PAYMENT_RELEASED", missionId.toString());

        log.info("[PAYMENT] Fonds libérés: mission={} payout={} commission={}", missionId, payout, commission);
    }

    // ─── Remboursement partiel (litige) ──────────────────────────────────────

    @Transactional
    public void partialRefund(UUID missionId, BigDecimal refundAmount, String reason) {
        Mission m = findMission(missionId);
        if (m.getPaymentStatus() != Mission.PaymentStatus.DISPUTED &&
            m.getPaymentStatus() != Mission.PaymentStatus.ESCROWED)
            throw new IllegalStateException("Remboursement possible uniquement sur mission séquestrée ou en litige.");

        BigDecimal total = m.getTotalPrice() != null ? m.getTotalPrice() : BigDecimal.ZERO;
        if (refundAmount.compareTo(total) > 0)
            throw new IllegalArgumentException("Le montant remboursé dépasse le total.");

        BigDecimal remainingForDriver = total.subtract(refundAmount);
        BigDecimal commRate = m.getCommissionRate() != null ? m.getCommissionRate() : new BigDecimal("0.075");
        BigDecimal commission = remainingForDriver.multiply(commRate).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal payout = remainingForDriver.subtract(commission);

        // Enregistrer le remboursement
        walletRepo.save(PlatformWallet.builder().mission(m).payer(m.getImporter()).amount(refundAmount)
                .transactionType(PlatformWallet.TransactionType.REFUND)
                .status(PlatformWallet.TxStatus.CONFIRMED)
                .notes("Remboursement litige: " + reason).build());

        if (payout.compareTo(BigDecimal.ZERO) > 0) {
            walletRepo.save(PlatformWallet.builder().mission(m).amount(payout)
                    .transactionType(PlatformWallet.TransactionType.RELEASE)
                    .status(PlatformWallet.TxStatus.CONFIRMED).build());
        }

        m.setPaymentStatus(Mission.PaymentStatus.REFUNDED);
        missionRepo.save(m);

        notify(m.getImporter(), "KmerFret — Remboursement effectué",
                String.format("Remboursement de %,.0f FCFA traité. Motif: %s", refundAmount, reason),
                "REFUND_PROCESSED", missionId.toString());
        if (m.getDriver() != null)
            notify(m.getDriver(), "KmerFret — Litige résolu",
                    String.format("Résolution du litige: vous recevez %,.0f FCFA.", payout),
                    "REFUND_PROCESSED", missionId.toString());

        log.info("[PAYMENT] Remboursement partiel: mission={} refund={} payout={}", missionId, refundAmount, payout);
    }

    // ─── Confirmation manuelle admin (si webhook échoue) ────────────────────

    @Transactional
    public void confirmPaymentManual(UUID missionId, String adminEmail) {
        Mission m = findMission(missionId);
        if (m.getPaymentStatus() == Mission.PaymentStatus.ESCROWED)
            throw new IllegalStateException("Paiement déjà confirmé.");
        String ref = "MANUAL-" + adminEmail.split("@")[0] + "-" + System.currentTimeMillis();
        confirmPayment(missionId, ref, m.getPaymentMethod() != null ? m.getPaymentMethod().name() : "MANUAL");
        log.info("[PAYMENT] Confirmation manuelle par {} pour mission {}", adminEmail, missionId);
    }

    @Transactional
    public String getPaymentStatus(UUID missionId) {
        Mission m = findMission(missionId);
        return m.getPaymentStatus() != null ? m.getPaymentStatus().name() : "UNKNOWN";
    }

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void sendPaymentReminders() {
        OffsetDateTime threshold = OffsetDateTime.now().plusHours(72);
        List<Mission> unpaid = missionRepo.findAssignedUnpaidBefore(threshold);
        unpaid.forEach(m -> {
            try {
                notify(m.getImporter(), "Paiement requis",
                        "Votre mission part bientot, versez " + m.getFirstPaymentAmount() + " FCFA",
                        "PAYMENT_REMINDER", m.getId().toString());
                if (m.getImporter().getEmail() != null)
                    emailService.sendPaymentReminderEmail(m.getImporter().getEmail(),
                            m.getImporter().getFullName(), m.getId().toString().substring(0, 8), m.getFirstPaymentAmount());
            } catch (Exception e) { log.warn("Reminder error: {}", e.getMessage()); }
        });
    }

    @Transactional
    public void confirmPayment(UUID missionId, String ref, String method) {
        Mission m = findMission(missionId);
        m.setPaymentStatus(Mission.PaymentStatus.ESCROWED);
        m.setPaymentReference(ref);
        m.setFirstPaymentAt(OffsetDateTime.now());
        try { m.setPaymentMethod(Mission.PaymentMethod.valueOf(method)); } catch (Exception ignored) {}
        missionRepo.save(m);
        walletRepo.findByExternalRef(ref).ifPresent(w -> { w.setStatus(PlatformWallet.TxStatus.CONFIRMED); walletRepo.save(w); });
        notify(m.getImporter(), "Paiement confirme", "Versement recu. La mission peut demarrer.", "PAYMENT_CONFIRMED", missionId.toString());
        if (m.getDriver() != null)
            notify(m.getDriver(), "Paiement client confirme", "Le client a paye. Vous pouvez prendre en charge la mission.", "PAYMENT_CONFIRMED", missionId.toString());
    }

    private Mission findMission(UUID id) {
        return missionRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
    }

    private void notify(User user, String title, String body, String type, String refId) {
        if (user.getPushToken() != null)
            pushService.sendToToken(user.getPushToken(), title, body, Map.of("type", type, "referenceId", refId));
        notifRepo.save(Notification.builder().user(user).title(title).body(body).notifType(type).referenceId(refId).build());
    }
}
