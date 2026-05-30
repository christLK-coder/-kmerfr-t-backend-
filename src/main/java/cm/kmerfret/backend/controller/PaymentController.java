package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/missions/{missionId}/mtn-momo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initMtnMomo(
            @PathVariable UUID missionId,
            @RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.initiateMtnMomoPay(missionId, phone)));
    }

    @PostMapping("/missions/{missionId}/orange-money")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initOrangeMoney(
            @PathVariable UUID missionId,
            @RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.initiateOrangeMoneyPay(missionId, phone)));
    }

    @GetMapping("/missions/{missionId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentStatus(
            @PathVariable UUID missionId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentStatus(missionId)));
    }
}
