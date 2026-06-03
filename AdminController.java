package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.model.*;
import cm.kmerfret.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final MissionRepository missionRepo;
    private final UserRepository userRepo;
    private final PlatformWalletRepository walletRepo;
    private final CommissionPolicyRepository policyRepo;

    // ─── Missions ────────────────────────────────────────────────────────────────

    @GetMapping("/missions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listMissions(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<Mission> missions;
        if ("ALL".equals(status)) {
            Page<Mission> p = missionRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
            missions = p.getContent();
        } else {
            missions = missionRepo.findByStatus(Mission.MissionStatus.valueOf(status));
        }
        return ResponseEntity.ok(ApiResponse.ok(missions.stream().map(this::missionToMap).collect(Collectors.toList())));
    }

    @GetMapping("/missions/disputed")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listDisputed() {
        List<Mission> disputed = missionRepo.findByStatus(Mission.MissionStatus.DISPUTED);
        return ResponseEntity.ok(ApiResponse.ok(disputed.stream().map(this::missionToMap).collect(Collectors.toList())));
    }

    // ─── Comptes ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUsers(
            @RequestParam(defaultValue = "ALL") String role) {
        List<User> users;
        if ("ALL".equals(role)) {
            users = userRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            users = userRepo.findActiveByRole(User.Role.valueOf(role));
        }
        return ResponseEntity.ok(ApiResponse.ok(users.stream().map(this::userToMap).collect(Collectors.toList())));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable UUID id) {
        User u = userRepo.findById(id).orElseThrow();
        u.setAccountStatus(User.AccountStatus.SUSPENDED);
        u.setIsActive(false);
        userRepo.save(u);
        return ResponseEntity.ok(ApiResponse.ok(null, "Compte suspendu."));
    }

    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(@PathVariable UUID id) {
        User u = userRepo.findById(id).orElseThrow();
        u.setAccountStatus(User.AccountStatus.ACTIVE);
        u.setIsActive(true);
        userRepo.save(u);
        return ResponseEntity.ok(ApiResponse.ok(null, "Compte reactivé."));
    }

    // ─── Finances ────────────────────────────────────────────────────────────────

    @GetMapping("/wallet/transactions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<PlatformWallet> txPage = walletRepo.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Map<String, Object>> list = txPage.getContent().stream().map(w -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("missionId", w.getMission().getId());
            m.put("amount", w.getAmount());
            m.put("transactionType", w.getTransactionType().name());
            m.put("paymentMethod", w.getPaymentMethod());
            m.put("status", w.getStatus().name());
            m.put("externalRef", w.getExternalRef());
            m.put("createdAt", w.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/wallet/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> walletSummary() {
        BigDecimal escrowed = walletRepo.totalEscrowed();
        BigDecimal commissions = walletRepo.totalCommissions();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEscrowed", escrowed != null ? escrowed : BigDecimal.ZERO);
        summary.put("totalCommissions", commissions != null ? commissions : BigDecimal.ZERO);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    // ─── Politique tarifaire ─────────────────────────────────────────────────────

    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPolicy() {
        CommissionPolicy p = policyRepo.findFirstByOrderByUpdatedAtDesc()
                .orElse(CommissionPolicy.builder().build());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("normalRate", p.getNormalRate());
        m.put("expressRate", p.getExpressRate());
        m.put("expressSurcharge", p.getExpressSurcharge());
        m.put("firstDepositPct", p.getFirstDepositPct());
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @PutMapping("/policy")
    public ResponseEntity<ApiResponse<Void>> updatePolicy(@RequestBody Map<String, Object> body) {
        CommissionPolicy p = policyRepo.findFirstByOrderByUpdatedAtDesc()
                .orElse(CommissionPolicy.builder().build());
        if (body.containsKey("normalRate"))
            p.setNormalRate(new BigDecimal(body.get("normalRate").toString()));
        if (body.containsKey("expressRate"))
            p.setExpressRate(new BigDecimal(body.get("expressRate").toString()));
        if (body.containsKey("expressSurcharge"))
            p.setExpressSurcharge(new BigDecimal(body.get("expressSurcharge").toString()));
        if (body.containsKey("firstDepositPct"))
            p.setFirstDepositPct(new BigDecimal(body.get("firstDepositPct").toString()));
        policyRepo.save(p);
        return ResponseEntity.ok(ApiResponse.ok(null, "Politique tarifaire mise à jour."));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private Map<String, Object> missionToMap(Mission m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("status", m.getStatus().name());
        map.put("missionType", m.getMissionType().name());
        map.put("paymentStatus", m.getPaymentStatus().name());
        map.put("originLabel", m.getOriginLabel());
        map.put("destinationLabel", m.getDestinationLabel());
        map.put("cargoDescription", m.getCargoDescription());
        map.put("cargoType", m.getCargoType().name());
        map.put("totalPrice", m.getTotalPrice());
        map.put("commissionAmount", m.getCommissionAmount());
        map.put("driverPayout", m.getDriverPayout());
        map.put("importerName", m.getImporter() != null ? m.getImporter().getFullName() : null);
        map.put("driverName", m.getDriver() != null ? m.getDriver().getFullName() : null);
        map.put("createdAt", m.getCreatedAt());
        map.put("pickupScheduledAt", m.getPickupScheduledAt());
        return map;
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("fullName", u.getFullName());
        map.put("email", u.getEmail());
        map.put("phone", u.getPhone());
        map.put("role", u.getRole().name());
        map.put("accountStatus", u.getAccountStatus().name());
        map.put("isActive", u.getIsActive());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }
}
