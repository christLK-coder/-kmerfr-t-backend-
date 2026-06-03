package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/mission/{id}/stripe/intent")
    public ResponseEntity<ApiResponse<Map<String, String>>> stripeIntent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.initiateStripePayment(id)));
    }

    @PostMapping("/mission/{id}/monetbill/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> monetbillInit(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.initiateMonetbillPayment(id, body.get("phoneNumber"))));
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sig) {
        paymentService.handleStripeWebhook(payload, sig);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook/monetbill")
    public ResponseEntity<Void> monetbillWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Monetbill-Signature", defaultValue = "") String sig,
            @RequestParam Map<String, String> params) {
        paymentService.handleMonetbillWebhook(payload, sig, params);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mission/{id}/status")
    public ResponseEntity<ApiResponse<String>> paymentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentStatus(id)));
    }

    @PostMapping("/admin/mission/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> releaseFunds(@PathVariable UUID id) {
        paymentService.releaseFunds(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Fonds liberes au chauffeur."));
    }
}
