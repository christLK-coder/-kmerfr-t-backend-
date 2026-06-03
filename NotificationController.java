package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.model.Notification;
import cm.kmerfret.backend.repository.NotificationRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notifRepo;
    private final UserRepository userRepo;

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<Notification>>> mine(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page) {
        UUID userId = userRepo.findByEmail(principal.getUsername()).orElseThrow().getId();
        PageRequest pr = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(notifRepo.findByUserIdOrderByCreatedAtDesc(userId, pr).getContent()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = userRepo.findByEmail(principal.getUsername()).orElseThrow().getId();
        return ResponseEntity.ok(ApiResponse.ok(notifRepo.countUnread(userId)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> readAll(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = userRepo.findByEmail(principal.getUsername()).orElseThrow().getId();
        notifRepo.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
