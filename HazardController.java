package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.SyncHazardRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.dto.response.HazardResponse;
import cm.kmerfret.backend.service.HazardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hazards")
@RequiredArgsConstructor
public class HazardController {

    private final HazardService hazardService;

    @PostMapping
    public ResponseEntity<ApiResponse<HazardResponse>> createHazard(
            @Valid @RequestBody SyncHazardRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(hazardService.createHazard(req)));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<HazardResponse>>> createBatch(
            @Valid @RequestBody List<SyncHazardRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(hazardService.createBatch(requests)));
    }

    // Route publique — pas de JWT requis (configuré dans SecurityConfig)
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<List<HazardResponse>>> getMap(
            @RequestParam double lat1,
            @RequestParam double lng1,
            @RequestParam double lat2,
            @RequestParam double lng2) {
        return ResponseEntity.ok(ApiResponse.ok(
                hazardService.getInBoundingBox(lat1, lng1, lat2, lng2)));
    }
}
