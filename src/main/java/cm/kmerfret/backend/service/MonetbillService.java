package cm.kmerfret.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonetbillService {

    private final RestTemplate restTemplate;

    @Value("${app.monetbill.service-key:}")
    private String serviceKey;

    @Value("${app.monetbill.service-secret:}")
    private String serviceSecret;

    @Value("${app.monetbill.payment-url:https://api.monetbill.com/v2.1/payment/pay}")
    private String paymentUrl;

    @Value("${app.monetbill.check-url:https://api.monetbill.com/v2.1/payment/checkPayment}")
    private String checkUrl;

    @Value("${app.monetbill.notify-url:http://localhost:8081/api/payments/webhook/monetbill}")
    private String notifyUrl;

    public MonetbillInitResult initiatePayment(String phoneNumber, BigDecimal amount, String missionId, String description) {
        if (serviceKey.isBlank()) {
            throw new RuntimeException("Monetbill non configuré — service_key manquant");
        }

        try {
            String ref = "KMF_" + missionId.substring(0, 8).toUpperCase() + "_" + System.currentTimeMillis();
            String sign = sign(serviceKey + amount.intValue() + phoneNumber + ref);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("service_key", serviceKey);
            form.add("amount", String.valueOf(amount.intValue()));
            form.add("phone", phoneNumber);
            form.add("payment_ref", ref);
            form.add("notify_url", notifyUrl);
            form.add("description", description);
            form.add("sign", sign);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> resp = restTemplate.exchange(
                    paymentUrl, HttpMethod.POST,
                    new HttpEntity<>(form, headers), Map.class);

            if (resp.getBody() != null) {
                String paymentUrl = (String) resp.getBody().getOrDefault("payment_url", "");
                String status = (String) resp.getBody().getOrDefault("status", "PENDING");
                return new MonetbillInitResult(ref, paymentUrl, status);
            }
        } catch (Exception e) {
            log.error("Erreur Monetbill : {}", e.getMessage());
        }
        return new MonetbillInitResult(null, null, "ERROR");
    }

    public String checkPayment(String paymentRef) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("service_key", serviceKey);
            form.add("payment_ref", paymentRef);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    checkUrl, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
            if (resp.getBody() != null) {
                return (String) resp.getBody().getOrDefault("status", "PENDING");
            }
        } catch (Exception e) {
            log.error("Erreur vérif Monetbill : {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    public boolean validateWebhook(String payload, String signature) {
        try {
            String expected = sign(serviceSecret + payload);
            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(serviceSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }

    public record MonetbillInitResult(String paymentRef, String paymentUrl, String status) {}
}
