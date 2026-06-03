package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.SyncPositionRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> syncBatch(
            @Valid @RequestBody List<SyncPositionRequest> requests) {
        int count = telemetryService.syncBatch(requests);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("synced", count)));
    }

    /** Dernière position connue du chauffeur pour une mission — utilisée par l'importer. */
    @GetMapping("/mission/{id}/latest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> latestPosition(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.ok(telemetryService.getLatestPosition(id).orElse(null)));
    }
}
