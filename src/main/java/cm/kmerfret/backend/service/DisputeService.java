package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.CreateDisputeRequest;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.exception.UnauthorizedException;
import cm.kmerfret.backend.model.*;
import cm.kmerfret.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final MissionRepository     missionRepository;
    private final UserRepository        userRepository;
    private final PlatformWalletRepository walletRepository;
    private final NotificationRepository notifRepository;
    private final PushNotificationService pushService;

    // Le sujet JWT est l'email (cohérent avec tous les autres services)
    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public Map<String, Object> openDispute(CreateDisputeRequest req) {
        User requester = currentUser();
        Mission m = missionRepository.findById(req.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable"));

        boolean isImporter = m.getImporter().getId().equals(requester.getId());
        boolean isDriver   = m.getDriver() != null && m.getDriver().getId().equals(requester.getId());

        if (!isImporter && !isDriver && requester.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Vous ne participez pas à cette mission");
        }

        m.setStatus(Mission.MissionStatus.DISPUTED);
        m.setPaymentStatus(Mission.PaymentStatus.DISPUTED);
        missionRepository.save(m);

        // Notifier les deux parties + admin
        notify(m.getImporter(), "⚠️ Litige ouvert",
            "Un litige a été signalé sur votre mission. Les fonds sont bloqués.", "DISPUTE_OPENED", m.getId().toString());
        if (m.getDriver() != null)
            notify(m.getDriver(), "⚠️ Litige signalé",
                "Un litige a été ouvert sur votre mission. Les fonds sont gelés.", "DISPUTE_OPENED", m.getId().toString());

        log.info("[DISPUTE] Litige ouvert — mission {} par {} ({})", m.getId(), requester.getFullName(), req.getDisputeType());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("missionId",   m.getId());
        result.put("status",      "DISPUTED");
        result.put("disputeType", req.getDisputeType());
        result.put("description", req.getDescription());
        result.put("openedBy",    requester.getFullName());
        result.put("message",     "Litige ouvert. L'équipe KmerFret vous contactera sous 24h.");
        return result;
    }

    /**
     * Résolution d'un litige par l'admin.
     * resolution = "RELEASE"  → libérer les fonds au chauffeur (déduire commission)
     * resolution = "REFUND"   → rembourser intégralement l'importateur
     * resolution = "PARTIAL:{montantRemboursement}" → remboursement partiel
     */
    @Transactional
    public Map<String, Object> resolveDispute(UUID missionId, String resolution) {
        User admin = currentUser();
        if (admin.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Seul un administrateur peut résoudre un litige");
        }
        Mission m = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable"));

        if (m.getPaymentStatus() != Mission.PaymentStatus.DISPUTED) {
            throw new IllegalStateException("Cette mission n'est pas en litige.");
        }

        BigDecimal total = m.getTotalPrice() != null ? m.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal commRate = m.getCommissionRate() != null ? m.getCommissionRate() : new BigDecimal("0.075");

        if ("RELEASE".equalsIgnoreCase(resolution) || "RELEASE_DRIVER".equalsIgnoreCase(resolution)) {
            // ── Libérer les fonds au chauffeur ────────────────────────────────
            BigDecimal commission = total.multiply(commRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal payout     = total.subtract(commission);

            walletRepository.save(PlatformWallet.builder().mission(m)
                .amount(commission).transactionType(PlatformWallet.TransactionType.COMMISSION)
                .status(PlatformWallet.TxStatus.CONFIRMED)
                .notes("Commission litige résolu — " + (commRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros()) + "%")
                .build());

            walletRepository.save(PlatformWallet.builder().mission(m)
                .amount(payout).transactionType(PlatformWallet.TransactionType.RELEASE)
                .status(PlatformWallet.TxStatus.CONFIRMED)
                .notes("Litige résolu en faveur du chauffeur")
                .build());

            m.setStatus(Mission.MissionStatus.DELIVERED);
            m.setPaymentStatus(Mission.PaymentStatus.RELEASED);

            if (m.getDriver() != null)
                notify(m.getDriver(), "💸 Litige résolu — paiement reçu",
                    String.format("Litige résolu. Votre paiement de %,.0f FCFA a été transféré.", payout),
                    "DISPUTE_RESOLVED", missionId.toString());
            notify(m.getImporter(), "✅ Litige clôturé",
                "Le litige a été résolu. Les fonds ont été versés au transporteur.",
                "DISPUTE_RESOLVED", missionId.toString());

            log.info("[DISPUTE] Résolu RELEASE — mission {} payout={} commission={}", missionId, payout, commission);

        } else if ("REFUND".equalsIgnoreCase(resolution) || "REFUND_CLIENT".equalsIgnoreCase(resolution)) {
            // ── Rembourser intégralement l'importateur ───────────────────────
            walletRepository.save(PlatformWallet.builder().mission(m).payer(m.getImporter())
                .amount(total).transactionType(PlatformWallet.TransactionType.REFUND)
                .status(PlatformWallet.TxStatus.CONFIRMED)
                .notes("Remboursement intégral litige")
                .build());

            m.setPaymentStatus(Mission.PaymentStatus.REFUNDED);

            notify(m.getImporter(), "💰 Remboursement effectué",
                String.format("Litige résolu. Remboursement de %,.0f FCFA traité.", total),
                "REFUND_PROCESSED", missionId.toString());
            if (m.getDriver() != null)
                notify(m.getDriver(), "⚠️ Litige résolu — remboursement client",
                    "Le litige a été résolu en faveur du client. Aucun paiement versé.",
                    "DISPUTE_RESOLVED", missionId.toString());

            log.info("[DISPUTE] Résolu REFUND — mission {} montant={}", missionId, total);

        } else {
            throw new IllegalArgumentException("Résolution inconnue : " + resolution
                + ". Valeurs acceptées : RELEASE, REFUND");
        }

        missionRepository.save(m);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("missionId",  m.getId());
        result.put("resolution", resolution.toUpperCase());
        result.put("status",     m.getStatus().name());
        result.put("paymentStatus", m.getPaymentStatus().name());
        return result;
    }

    private void notify(User user, String title, String body, String type, String refId) {
        try {
            if (user.getPushToken() != null)
                pushService.sendToToken(user.getPushToken(), title, body, Map.of("type", type, "referenceId", refId));
            notifRepository.save(Notification.builder()
                .user(user).title(title).body(body)
                .notifType(type).referenceId(refId).isRead(false).build());
        } catch (Exception e) {
            log.warn("[DISPUTE] Notification error: {}", e.getMessage());
        }
    }
}
