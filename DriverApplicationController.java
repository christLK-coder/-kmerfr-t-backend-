package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.DriverApplicationRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.model.ApplicationMessage;
import cm.kmerfret.backend.model.DriverApplication;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.ApplicationMessageRepository;
import cm.kmerfret.backend.repository.DriverApplicationRepository;
import cm.kmerfret.backend.repository.UserRepository;
import cm.kmerfret.backend.service.AuthService;
import cm.kmerfret.backend.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DriverApplicationController {

    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads", "applications");

    private final DriverApplicationRepository appRepo;
    private final ApplicationMessageRepository msgRepo;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // ─── Candidat : soumettre une candidature ─────────────────────────────────

    @PostMapping("/auth/apply/driver")
    public ResponseEntity<ApiResponse<Map<String, String>>> apply(
            @Valid @RequestBody DriverApplicationRequest req) {

        if (appRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Une candidature existe déjà pour cet email.", List.of()));
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Un compte existe déjà avec cet email.", List.of()));
        }

        DriverApplication application = DriverApplication.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .city(req.getCity())
                .motivation(req.getMotivation())
                .status(DriverApplication.Status.PENDING)
                .build();

        DriverApplication saved = appRepo.save(application);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("applicationId", saved.getId().toString()), "Candidature soumise."));
    }

    // ─── Candidat : consulter sa candidature + messages ─────────────────────

    @GetMapping("/auth/apply/driver/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApplication(@PathVariable UUID id) {
        DriverApplication app = appRepo.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();

        List<ApplicationMessage> messages = msgRepo.findByApplicationIdOrderByCreatedAtAsc(id);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", app.getId());
        result.put("fullName", app.getFullName());
        result.put("email", app.getEmail());
        result.put("phone", app.getPhone());
        result.put("city", app.getCity());
        result.put("motivation", app.getMotivation());
        result.put("status", app.getStatus().name());
        result.put("adminNotes", app.getAdminNotes());
        result.put("submittedAt", app.getSubmittedAt());
        result.put("reviewedAt", app.getReviewedAt());
        result.put("messages", messages.stream().map(m -> {
            Map<String, Object> msg = new java.util.LinkedHashMap<>();
            msg.put("id", m.getId());
            msg.put("senderRole", m.getSenderRole().name());
            msg.put("content", m.getContent());
            msg.put("attachmentUrl", m.getAttachmentUrl());
            msg.put("createdAt", m.getCreatedAt());
            return msg;
        }).toList());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ─── Candidat : uploader un fichier ──────────────────────────────────────

    @PostMapping(value = "/auth/apply/driver/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {

        appRepo.findById(id).orElseThrow(() -> new RuntimeException("Candidature introuvable."));

        try {
            Path dir = UPLOAD_DIR.resolve(id.toString());
            Files.createDirectories(dir);
            String ext = "";
            String orig = file.getOriginalFilename();
            if (orig != null && orig.contains(".")) ext = orig.substring(orig.lastIndexOf('.'));
            String fileName = UUID.randomUUID().toString().substring(0, 8) + ext;
            Path dest = dir.resolve(fileName);
            Files.copy(file.getInputStream(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String url = "/api/auth/apply/driver/" + id + "/files/" + fileName;
            log.info("Fichier uploadé: {}", dest);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("url", url, "fileName", orig != null ? orig : fileName)));
        } catch (IOException e) {
            log.error("Erreur upload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur upload: " + e.getMessage(), List.of()));
        }
    }

    // ─── Servir les fichiers uploadés ─────────────────────────────────────────

    @GetMapping("/auth/apply/driver/{id}/files/{fileName}")
    public ResponseEntity<byte[]> serveFile(@PathVariable UUID id, @PathVariable String fileName) {
        try {
            Path file = UPLOAD_DIR.resolve(id.toString()).resolve(fileName);
            if (!Files.exists(file)) return ResponseEntity.notFound().build();
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Cache-Control", "public, max-age=86400")
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── Candidat : envoyer un message ────────────────────────────────────────

    @PostMapping("/auth/apply/driver/{id}/message")
    public ResponseEntity<ApiResponse<Void>> addMessage(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        DriverApplication app = appRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature introuvable."));

        String content = body.get("content");
        String attachmentUrl = body.get("attachmentUrl");

        ApplicationMessage message = ApplicationMessage.builder()
                .application(app)
                .senderRole(ApplicationMessage.SenderRole.APPLICANT)
                .content(content != null ? content : "")
                .attachmentUrl(attachmentUrl)
                .build();

        msgRepo.save(message);
        return ResponseEntity.ok(ApiResponse.ok(null, "Message envoyé."));
    }

    // ─── Admin : envoyer un message au candidat ─────────────────────────────────

    @PostMapping("/admin/applications/{id}/message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addAdminMessage(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        DriverApplication app = appRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature introuvable."));

        String content = body.get("content");
        ApplicationMessage message = ApplicationMessage.builder()
                .application(app)
                .senderRole(ApplicationMessage.SenderRole.ADMIN)
                .content(content != null ? content : "")
                .build();

        msgRepo.save(message);

        if (app.getStatus() == DriverApplication.Status.PENDING) {
            app.setStatus(DriverApplication.Status.UNDER_REVIEW);
            appRepo.save(app);
        }

        return ResponseEntity.ok(ApiResponse.ok(null, "Message envoyé."));
    }

    // ─── Admin : lister toutes les candidatures ───────────────────────────────

    @GetMapping("/admin/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DriverApplication>>> listApplications(
            @RequestParam(required = false) DriverApplication.Status status) {
        List<DriverApplication> apps = status != null
                ? appRepo.findByStatusOrderBySubmittedAtDesc(status)
                : appRepo.findAllByOrderBySubmittedAtDesc();
        return ResponseEntity.ok(ApiResponse.ok(apps));
    }

    // ─── Admin : approuver une candidature ────────────────────────────────────

    @PutMapping("/admin/applications/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody(required = false) Map<String, String> body) {

        DriverApplication app = appRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature introuvable."));

        // Créer le compte chauffeur
        String tempPassword = generateTempPassword();
        User driver = User.builder()
                .fullName(app.getFullName())
                .email(app.getEmail())
                .phone(app.getPhone())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(User.Role.DRIVER)
                .isActive(true)
                .emailVerified(true)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        User savedDriver = userRepository.save(driver);

        app.setStatus(DriverApplication.Status.APPROVED);
        app.setCreatedUser(savedDriver);
        app.setReviewedAt(OffsetDateTime.now());
        app.setReviewedBy(authService.loadUserByEmail(principal.getUsername()));
        appRepo.save(app);

        emailService.sendDriverApprovalEmail(app.getEmail(), app.getFullName(), tempPassword);
        return ResponseEntity.ok(ApiResponse.ok(null, "Candidature approuvée, compte créé."));
    }

    // ─── Admin : rejeter une candidature ──────────────────────────────────────

    @PutMapping("/admin/applications/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {

        DriverApplication app = appRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature introuvable."));

        app.setStatus(DriverApplication.Status.REJECTED);
        app.setAdminNotes(body.get("reason"));
        app.setReviewedAt(OffsetDateTime.now());
        app.setReviewedBy(authService.loadUserByEmail(principal.getUsername()));
        appRepo.save(app);

        emailService.sendDriverRejectionEmail(app.getEmail(), app.getFullName(), body.get("reason"));
        return ResponseEntity.ok(ApiResponse.ok(null, "Candidature rejetée."));
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#";
        StringBuilder sb = new StringBuilder(12);
        java.util.Random rng = new java.security.SecureRandom();
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }
}
