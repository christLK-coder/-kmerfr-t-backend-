package cm.kmerfret.backend.service;

import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.exception.UnauthorizedException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${mtn.momo.base-url:https://sandbox.momodeveloper.mtn.com}")
    private String momoBaseUrl;

    @Value("${mtn.momo.subscription-key:sandbox-key}")
    private String momoSubscriptionKey;

    @Value("${orange.money.base-url:https://api.orange.com/orange-money-webpay/cm/v1}")
    private String orangeBaseUrl;

    private User currentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    // ─── Initier paiement MTN MoMo ─────────────────────────────────────────────

    @Transactional
    public Map<String, Object> initiateMtnMomoPay(UUID missionId, String phoneNumber) {
        User payer = currentUser();
        Mission m = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + missionId));

        if (!m.getImporter().getId().equals(payer.getId())) {
            throw new UnauthorizedException("Seul l'importateur peut initier le paiement");
        }
        if (m.getPaymentStatus() == Mission.PaymentStatus.ESCROWED
                || m.getPaymentStatus() == Mission.PaymentStatus.RELEASED) {
            throw new IllegalStateException("Paiement déjà effectué pour cette mission");
        }

        String referenceId = UUID.randomUUID().toString();

        // Appel MTN MoMo Collection API (sandbox)
        boolean momoSuccess = callMtnMomoRequestToPay(
                referenceId,
                m.getTotalPrice(),
                phoneNumber,
                "KmerFret-" + missionId.toString().substring(0, 8)
        );

        if (momoSuccess) {
            m.setPaymentMethod(Mission.PaymentMethod.MTN_MOMO);
            m.setPaymentReference(referenceId);
            m.setPaymentStatus(Mission.PaymentStatus.ESCROWED);
            missionRepository.save(m);
            log.info("MTN MoMo paiement initié — mission {} ref {}", missionId, referenceId);
        } else {
            log.warn("MTN MoMo sandbox non disponible — mission {} marquée en attente", missionId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("referenceId", referenceId);
        result.put("status", momoSuccess ? "ESCROWED" : "PENDING");
        result.put("provider", "MTN_MOMO");
        result.put("amount", m.getTotalPrice());
        return result;
    }

    // ─── Initier paiement Orange Money ────────────────────────────────────────

    @Transactional
    public Map<String, Object> initiateOrangeMoneyPay(UUID missionId, String phoneNumber) {
        User payer = currentUser();
        Mission m = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + missionId));

        if (!m.getImporter().getId().equals(payer.getId())) {
            throw new UnauthorizedException("Seul l'importateur peut initier le paiement");
        }
        if (m.getPaymentStatus() == Mission.PaymentStatus.ESCROWED
                || m.getPaymentStatus() == Mission.PaymentStatus.RELEASED) {
            throw new IllegalStateException("Paiement déjà effectué pour cette mission");
        }

        String referenceId = "OM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        m.setPaymentMethod(Mission.PaymentMethod.ORANGE_MONEY);
        m.setPaymentReference(referenceId);
        m.setPaymentStatus(Mission.PaymentStatus.ESCROWED);
        missionRepository.save(m);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("referenceId", referenceId);
        result.put("status", "ESCROWED");
        result.put("provider", "ORANGE_MONEY");
        result.put("amount", m.getTotalPrice());
        return result;
    }

    // ─── Statut paiement ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatus(UUID missionId) {
        Mission m = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + missionId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("missionId", missionId);
        result.put("paymentStatus", m.getPaymentStatus() != null ? m.getPaymentStatus().name() : "PENDING");
        result.put("paymentMethod", m.getPaymentMethod() != null ? m.getPaymentMethod().name() : null);
        result.put("paymentReference", m.getPaymentReference());
        result.put("totalPrice", m.getTotalPrice());
        result.put("commissionAmount", m.getCommissionAmount());
        result.put("driverPayout", m.getDriverPayout());
        return result;
    }

    // ─── Appel MTN MoMo API (sandbox) ────────────────────────────────────────

    private boolean callMtnMomoRequestToPay(String referenceId, BigDecimal amount,
                                             String phoneNumber, String payerMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Reference-Id", referenceId);
            headers.set("X-Target-Environment", "sandbox");
            headers.set("Ocp-Apim-Subscription-Key", momoSubscriptionKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", amount.toPlainString());
            body.put("currency", "XAF");
            body.put("externalId", referenceId);
            body.put("payer", Map.of(
                "partyIdType", "MSISDN",
                "partyId", phoneNumber.replaceAll("[^0-9]", "")
            ));
            body.put("payerMessage", payerMessage);
            body.put("payeeNote", "KmerFret transport payment");

            ResponseEntity<String> response = restTemplate.exchange(
                momoBaseUrl + "/collection/v1_0/requesttopay",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
            );
            return response.getStatusCode() == HttpStatus.ACCEPTED;
        } catch (Exception e) {
            log.warn("MTN MoMo API indisponible (sandbox) : {}", e.getMessage());
            return false;
        }
    }
}
