package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.ReviewRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final MissionRepository missionRepository;
    private final ReviewRepository  reviewRepository;
    private final UserRepository    userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myLoyalty() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<Mission> missions = missionRepository.findByDriverId(user.getId());
        long delivered    = missions.stream().filter(m -> m.getStatus() == Mission.MissionStatus.DELIVERED).count();
        double avgRating  = reviewRepository.avgRatingByUserId(user.getId()).orElse(0.0);
        long   nbReviews  = reviewRepository.countByReviewedId(user.getId());

        // Calcul points : 100 pts/livraison + 50 pts si note >= 4.5
        long points = delivered * 100L + (avgRating >= 4.5 && nbReviews > 0 ? delivered * 50L : 0);

        // Palier
        String tier;
        int    nextPoints;
        String benefit;
        if      (points >= 5000) { tier = "PLATINE"; nextPoints = 0;    benefit = "Commission 5% — Missions prioritaires VIP"; }
        else if (points >= 2000) { tier = "OR";      nextPoints = (int)(5000 - points); benefit = "Commission 6% — Accès missions premium"; }
        else if (points >= 500)  { tier = "ARGENT";  nextPoints = (int)(2000 - points); benefit = "Commission 6.5% — Badge chauffeur vérifié"; }
        else                     { tier = "BRONZE";  nextPoints = (int)(500  - points); benefit = "Commission standard 7.5%"; }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tier",           tier);
        result.put("points",         points);
        result.put("nextTierPoints", nextPoints);
        result.put("benefit",        benefit);
        result.put("deliveries",     delivered);
        result.put("averageRating",  Math.round(avgRating * 10.0) / 10.0);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
