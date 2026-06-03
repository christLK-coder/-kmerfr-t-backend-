package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.*;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.dto.response.AuthResponse;
import cm.kmerfret.backend.dto.response.LoginInitiateResponse;
import cm.kmerfret.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(null, "Compte créé. Vérifiez vos emails."));
    }

    @PostMapping("/login/initiate")
    public ResponseEntity<ApiResponse<LoginInitiateResponse>> initiateLogin(
            @Valid @RequestBody LoginInitiateRequest req) {
        LoginInitiateResponse resp = authService.initiateLogin(req);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PostMapping("/login/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLogin(
            @Valid @RequestBody LoginVerifyRequest req) {
        AuthResponse auth = authService.verifyLogin(req);
        return ResponseEntity.ok(ApiResponse.ok(auth));
    }

    @PostMapping("/biometric/login")
    public ResponseEntity<ApiResponse<AuthResponse>> biometricLogin(
            @Valid @RequestBody BiometricLoginRequest req) {
        AuthResponse auth = authService.biometricLogin(req);
        return ResponseEntity.ok(ApiResponse.ok(auth));
    }

    @PostMapping("/biometric/enable")
    public ResponseEntity<ApiResponse<Map<String, String>>> enableBiometric(
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId = resolveUserId(principal);
        AuthService.BiometricEnableResult result = authService.enableBiometric(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "biometricToken", result.biometricToken(),
                "expiresAt", result.expiresAt()
        )));
    }

    @PostMapping("/biometric/disable")
    public ResponseEntity<ApiResponse<Void>> disableBiometric(
            @AuthenticationPrincipal UserDetails principal) {
        authService.disableBiometric(resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.ok(null, "Biométrie désactivée."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {
        UUID userId = resolveUserId(principal);
        String currentPwd = body.get("currentPassword");
        String newPwd = body.get("newPassword");
        if (currentPwd == null || newPwd == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("currentPassword et newPassword requis", java.util.List.of()));
        }
        authService.changePassword(userId, currentPwd, newPwd);
        return ResponseEntity.ok(ApiResponse.ok(null, "Mot de passe modifié avec succès."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {
        AuthResponse auth = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(auth));
    }

    @PutMapping("/push-token")
    public ResponseEntity<ApiResponse<Void>> updatePushToken(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {
        authService.updatePushToken(resolveUserId(principal), body.get("pushToken"));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private UUID resolveUserId(UserDetails principal) {
        // Username is email — load user to get UUID
        return authService.loadUserByEmail(principal.getUsername()).getId();
    }
}
