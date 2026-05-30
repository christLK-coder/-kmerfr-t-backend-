package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.CreateTruckRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.dto.response.TruckResponse;
import cm.kmerfret.backend.service.TruckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trucks")
@RequiredArgsConstructor
public class TruckController {

    private final TruckService truckService;

    @PostMapping
    public ResponseEntity<ApiResponse<TruckResponse>> createTruck(
            @Valid @RequestBody CreateTruckRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(truckService.createTruck(req)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<TruckResponse>>> getMyTrucks() {
        return ResponseEntity.ok(ApiResponse.ok(truckService.getMyTrucks()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TruckResponse>> updateTruck(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTruckRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(truckService.updateTruck(id, req)));
    }
}
