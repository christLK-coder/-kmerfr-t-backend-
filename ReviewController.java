package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.CreateReviewRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.dto.response.ReviewResponse;
import cm.kmerfret.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reviewService.createReview(req)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getUserReviews(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getUserReviews(userId)));
    }

    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRatingStats(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getUserRatingStats(userId)));
    }
}
