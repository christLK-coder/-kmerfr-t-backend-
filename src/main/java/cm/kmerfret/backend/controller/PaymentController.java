package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        return ResponseEntity.ok(ApiResponse.ok(null, "Fonds libérés au transporteur."));
    }

    @PostMapping("/mission/{id}/second/stripe/intent")
    public ResponseEntity<ApiResponse<Map<String, String>>> secondStripeIntent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.initiateSecondPaymentStripe(id)));
    }

    @PostMapping("/mission/{id}/second/monetbill/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> secondMonetbillInit(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.initiateSecondPaymentMonetbill(id, body.get("phoneNumber"))));
    }

    @PostMapping("/mission/{id}/second/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmSecondPayment(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String ref = body != null ? body.getOrDefault("ref", null) : null;
        String method = body != null ? body.getOrDefault("method", "MTN_MOMO") : "MTN_MOMO";
        paymentService.confirmSecondPayment(id, ref, method);
        return ResponseEntity.ok(ApiResponse.ok(null, "Paiement du solde confirmé."));
    }

    @PostMapping("/admin/mission/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> partialRefund(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        java.math.BigDecimal amount = new java.math.BigDecimal(body.getOrDefault("amount", "0"));
        String reason = body.getOrDefault("reason", "Litige");
        paymentService.partialRefund(id, amount, reason);
        return ResponseEntity.ok(ApiResponse.ok(null, "Remboursement effectué."));
    }

    @PostMapping("/admin/mission/{id}/confirm-manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> confirmManual(
            @PathVariable UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        paymentService.confirmPaymentManual(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Paiement confirmé manuellement."));
    }
}
