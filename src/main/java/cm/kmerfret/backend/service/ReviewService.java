package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.CreateReviewRequest;
import cm.kmerfret.backend.dto.response.ReviewResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.exception.UnauthorizedException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.Review;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.ReviewRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    private User currentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest req) {
        User reviewer = currentUser();
        Mission mission = missionRepository.findById(req.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable"));

        if (mission.getStatus() != Mission.MissionStatus.DELIVERED) {
            throw new IllegalStateException("La notation n'est possible qu'après livraison");
        }
        if (reviewRepository.existsByMissionIdAndReviewerId(mission.getId(), reviewer.getId())) {
            throw new IllegalStateException("Vous avez déjà noté cette mission");
        }

        // L'importateur note le chauffeur et vice-versa
        User reviewed;
        if (reviewer.getId().equals(mission.getImporter().getId())) {
            if (mission.getDriver() == null) throw new IllegalStateException("Aucun chauffeur assigné");
            reviewed = mission.getDriver();
        } else if (mission.getDriver() != null && reviewer.getId().equals(mission.getDriver().getId())) {
            reviewed = mission.getImporter();
        } else {
            throw new UnauthorizedException("Vous ne participez pas à cette mission");
        }

        Review review = Review.builder()
                .mission(mission)
                .reviewer(reviewer)
                .reviewed(reviewed)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews(UUID userId) {
        return reviewRepository.findByReviewedId(userId)
                .stream().map(ReviewResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserRatingStats(UUID userId) {
        double avg = reviewRepository.avgRatingByUserId(userId).orElse(0.0);
        long count = reviewRepository.countByReviewedId(userId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userId", userId);
        stats.put("averageRating", Math.round(avg * 10.0) / 10.0);
        stats.put("totalReviews", count);
        return stats;
    }
}
