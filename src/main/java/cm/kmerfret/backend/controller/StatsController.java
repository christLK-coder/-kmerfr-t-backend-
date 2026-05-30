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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final MissionRepository missionRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myStats() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<Mission> missions;
        if (user.getRole() == User.Role.DRIVER) {
            missions = missionRepository.findByDriverId(user.getId());
        } else {
            missions = missionRepository.findByImporterId(user.getId());
        }

        long total      = missions.size();
        long delivered  = missions.stream().filter(m -> m.getStatus() == Mission.MissionStatus.DELIVERED).count();
        long inTransit  = missions.stream().filter(m -> m.getStatus() == Mission.MissionStatus.IN_TRANSIT).count();
        long open       = missions.stream().filter(m -> m.getStatus() == Mission.MissionStatus.OPEN).count();

        BigDecimal totalEarnings = missions.stream()
                .filter(m -> m.getStatus() == Mission.MissionStatus.DELIVERED)
                .map(m -> user.getRole() == User.Role.DRIVER
                        ? (m.getDriverPayout() != null ? m.getDriverPayout() : BigDecimal.ZERO)
                        : (m.getTotalPrice() != null   ? m.getTotalPrice()   : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgRating = reviewRepository.avgRatingByUserId(user.getId()).orElse(0.0);
        long   nbReviews = reviewRepository.countByReviewedId(user.getId());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("role",          user.getRole().name());
        stats.put("totalMissions", total);
        stats.put("delivered",     delivered);
        stats.put("inTransit",     inTransit);
        stats.put("open",          open);
        stats.put("totalAmount",   totalEarnings);
        stats.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        stats.put("totalReviews",  nbReviews);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
