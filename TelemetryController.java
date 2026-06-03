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
}
