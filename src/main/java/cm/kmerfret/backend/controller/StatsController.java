package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final MissionRepository   missionRepo;
    private final ReviewRepository    reviewRepo;
    private final UserRepository      userRepo;
    private final NotificationRepository notifRepo;
    private final PlatformWalletRepository walletRepo;
    private final DriverApplicationRepository appRepo;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myStats() {
        // Sujet JWT = email depuis V3
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<Mission> missions = user.getRole() == User.Role.DRIVER
                ? missionRepo.findByDriverId(user.getId())
                : missionRepo.findByImporterId(user.getId());

        long total     = missions.size();
        long delivered = missions.stream().filter(m -> m.getStatus() == Mission.MissionStatus.DELIVERED).count();
        long inTransit = missions.stream().filter(m -> m.getStatus() == Mission.MissionStatus.IN_TRANSIT).count();
        long open      = missions.stream().filter(m -> m.getStatus() == Mission.MissionStatus.OPEN).count();

        BigDecimal totalAmount = missions.stream()
                .filter(m -> m.getStatus() == Mission.MissionStatus.DELIVERED)
                .map(m -> user.getRole() == User.Role.DRIVER
                        ? (m.getDriverPayout()  != null ? m.getDriverPayout()  : BigDecimal.ZERO)
                        : (m.getTotalPrice()    != null ? m.getTotalPrice()    : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgRating = reviewRepo.avgRatingByUserId(user.getId()).orElse(0.0);
        long   nbReviews = reviewRepo.countByReviewedId(user.getId());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("role",          user.getRole().name());
        stats.put("totalMissions", total);
        stats.put("delivered",     delivered);
        stats.put("inTransit",     inTransit);
        stats.put("open",          open);
        stats.put("totalAmount",   totalAmount);
        stats.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        stats.put("totalReviews",  nbReviews);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminStats() {
        long activeMissions = missionRepo.findByStatus(Mission.MissionStatus.IN_TRANSIT).size()
                            + missionRepo.findByStatus(Mission.MissionStatus.ASSIGNED).size();
        long openDisputes   = missionRepo.findByStatus(Mission.MissionStatus.DISPUTED).size();
        long newClients     = userRepo.findActiveByRole(User.Role.IMPORTER).size();
        long newDrivers     = userRepo.findActiveByRole(User.Role.DRIVER).size();
        long pendingApps    = appRepo.findByStatusOrderBySubmittedAtDesc(
                cm.kmerfret.backend.model.DriverApplication.Status.PENDING).size();

        BigDecimal escrowed = walletRepo.totalEscrowed();
        BigDecimal commissions = walletRepo.totalCommissions();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeMissions",      activeMissions);
        stats.put("openDisputes",        openDisputes);
        stats.put("newClients",          newClients);
        stats.put("newDrivers",          newDrivers);
        stats.put("pendingApplications", pendingApps);
        stats.put("totalEscrowed",       escrowed != null ? escrowed : BigDecimal.ZERO);
        stats.put("totalCommissions",    commissions != null ? commissions : BigDecimal.ZERO);
        stats.put("revenueDay",          BigDecimal.ZERO);   // à implémenter avec date filter
        stats.put("revenueMonth",        commissions != null ? commissions : BigDecimal.ZERO);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
