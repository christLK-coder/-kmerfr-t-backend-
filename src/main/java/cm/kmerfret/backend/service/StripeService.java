package cm.kmerfret.backend.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class StripeService {

    @Value("${app.stripe.secret-key}")
    private String secretKey;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.stripe.currency:XAF}")
    private String currency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public StripePaymentResult createPaymentIntent(BigDecimal amountFcfa, String missionId) {
        try {
            // Stripe XAF is zero-decimal — pass amount as-is (XAF has no subunit)
            long amount = amountFcfa.longValue();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(currency.toLowerCase())
                    .putMetadata("missionId", missionId)
                    .addPaymentMethodType("card")
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            return new StripePaymentResult(intent.getId(), intent.getClientSecret(), "PENDING");
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent error : {}", e.getMessage());
            throw new RuntimeException("Erreur paiement Stripe : " + e.getMessage());
        }
    }

    public Event validateWebhook(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Signature webhook Stripe invalide");
        }
    }

    public record StripePaymentResult(String paymentIntentId, String clientSecret, String status) {}
}
