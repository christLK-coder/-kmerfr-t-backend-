package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.CreateMissionRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.dto.response.DocumentResponse;
import cm.kmerfret.backend.dto.response.MissionResponse;
import cm.kmerfret.backend.service.DocumentService;
import cm.kmerfret.backend.service.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final DocumentService documentService;

    // ─── Listes ──────────────────────────────────────────────────────────────

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<MissionResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.ok(missionService.listActiveMissions()));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<MissionResponse>>> listHistory() {
        return ResponseEntity.ok(ApiResponse.ok(missionService.listHistoryMissions()));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<MissionResponse>>> listAvailable() {
        return ResponseEntity.ok(ApiResponse.ok(missionService.listAvailableMissions()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MissionResponse>> getMission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.getMission(id)));
    }

    // ─── Création ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<MissionResponse>> createMission(@Valid @RequestBody CreateMissionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(missionService.createMission(req)));
    }

    // ─── Actions chauffeur ────────────────────────────────────────────────────

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<MissionResponse>> assignMission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.assignMission(id)));
    }

    @PutMapping("/{id}/driver-arrived")
    public ResponseEntity<ApiResponse<MissionResponse>> driverArrived(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.markDriverArrived(id)));
    }

    @PutMapping("/{id}/transit-start")
    public ResponseEntity<ApiResponse<MissionResponse>> startTransit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.startTransit(id)));
    }

    @PutMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<MissionResponse>> deliverMission(
            @PathVariable UUID id, @RequestParam String qrToken) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.deliverMission(id, qrToken)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<MissionResponse>> cancelMission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.cancelMission(id)));
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<ApiResponse<MissionResponse>> openDispute(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return ResponseEntity.ok(ApiResponse.ok(missionService.openDispute(id, reason)));
    }

    @PostMapping("/{id}/route-changed")
    public ResponseEntity<ApiResponse<Void>> routeChanged(@PathVariable UUID id) {
        missionService.notifyRouteChanged(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── QR + Documents ───────────────────────────────────────────────────────

    @GetMapping("/{id}/qr")
    public ResponseEntity<ApiResponse<String>> getQrToken(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.getQrToken(id)));
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMissionDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getMissionDocuments(id)));
    }
}
