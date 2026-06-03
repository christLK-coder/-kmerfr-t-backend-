package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.CreateDisputeRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.service.DisputeService;
import cm.kmerfret.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> openDispute(
            @Valid @RequestBody CreateDisputeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(disputeService.openDispute(req)));
    }

    @PutMapping("/{missionId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveDispute(
            @PathVariable UUID missionId,
            @RequestParam String resolution) {
        return ResponseEntity.ok(ApiResponse.ok(disputeService.resolveDispute(missionId, resolution)));
    }
}
