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
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MissionResponse>>> listMissions(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.listMissions(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MissionResponse>> getMission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.getMission(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MissionResponse>> createMission(
            @Valid @RequestBody CreateMissionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(missionService.createMission(req)));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<MissionResponse>> assignMission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.assignMission(id)));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<MissionResponse>> startMission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.startMission(id)));
    }

    @PutMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<MissionResponse>> deliverMission(
            @PathVariable UUID id,
            @RequestParam String qrToken) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.deliverMission(id, qrToken)));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<ApiResponse<String>> getQrToken(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(missionService.getQrToken(id)));
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMissionDocuments(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getMissionDocuments(id)));
    }
}
