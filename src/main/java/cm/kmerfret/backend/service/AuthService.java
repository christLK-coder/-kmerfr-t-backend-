package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.*;
import cm.kmerfret.backend.dto.response.AuthResponse;
import cm.kmerfret.backend.dto.response.LoginInitiateResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.EmailOtp;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.UserRepository;
import cm.kmerfret.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PushNotificationService pushService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.biometric.validity-days:30}")
    private int biometricValidityDays;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ─── Inscription client ───────────────────────────────────────────────────

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone())) {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé.");
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone() != null ? req.getPhone() : generateTempPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.IMPORTER)
                .emailVerified(false)
                .isActive(true)
                .build();

        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
    }

    // ─── Connexion étape 1 : email + mot de passe → envoi OTP ────────────────

    @Transactional
    public LoginInitiateResponse initiateLogin(LoginInitiateRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides."));

        if (!user.getIsActive() || user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            throw new BadCredentialsException("Compte désactivé ou suspendu.");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides.");
        }

        OtpService.OtpResult otp = otpService.generate(user, EmailOtp.Purpose.LOGIN);
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp.rawCode());

        String masked = maskEmail(user.getEmail());
        return new LoginInitiateResponse(otp.sessionToken(), otp.expiresIn(), masked);
    }

    // ─── Connexion étape 2 : vérification OTP → JWT ───────────────────────────

    @Transactional
    public AuthResponse verifyLogin(LoginVerifyRequest req) {
        User user = otpService.validate(req.getSessionToken(), req.getOtpCode())
                .orElseThrow(() -> new BadCredentialsException("Code invalide ou expiré."));

        if (!user.getEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return buildAuthResponse(user);
    }

    // ─── Connexion biométrique ────────────────────────────────────────────────

    @Transactional
    public AuthResponse biometricLogin(BiometricLoginRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        if (!Boolean.TRUE.equals(user.getBiometricEnabled())) {
            throw new BadCredentialsException("Biométrie non activée.");
        }
        if (user.getBiometricExpiresAt() == null || OffsetDateTime.now().isAfter(user.getBiometricExpiresAt())) {
            throw new BadCredentialsException("Session biométrique expirée — reconnectez-vous.");
        }

        String hash = sha256Hex(req.getBiometricToken());
        if (!hash.equals(user.getBiometricTokenHash())) {
            throw new BadCredentialsException("Token biométrique invalide.");
        }

        return buildAuthResponse(user);
    }

    // ─── Activation biométrie ─────────────────────────────────────────────────

    @Transactional
    public BiometricEnableResult enableBiometric(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        user.setBiometricEnabled(true);
        user.setBiometricTokenHash(sha256Hex(rawToken));
        user.setBiometricExpiresAt(OffsetDateTime.now().plusDays(biometricValidityDays));
        userRepository.save(user);

        return new BiometricEnableResult(rawToken, user.getBiometricExpiresAt().toString());
    }

    @Transactional
    public void disableBiometric(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        user.setBiometricEnabled(false);
        user.setBiometricTokenHash(null);
        user.setBiometricExpiresAt(null);
        userRepository.save(user);
    }

    // ─── Changement de mot de passe ──────────────────────────────────────────

    @Transactional
    public void changePassword(java.util.UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Mot de passe actuel incorrect.");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit contenir au moins 8 caractères.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        if (user.getPushToken() != null) {
            pushService.sendToToken(user.getPushToken(),
                    "KmerFret — Sécurité",
                    "Votre mot de passe a été modifié avec succès. Si ce n'est pas vous, contactez le support.",
                    java.util.Map.of("type", "PASSWORD_CHANGED"));
        }
    }

    // ─── Refresh token ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        String email = jwtTokenProvider.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        UserDetails userDetails = toUserDetails(user);
        if (!jwtTokenProvider.isTokenValid(refreshToken, userDetails)) {
            throw new BadCredentialsException("Refresh token invalide ou expiré.");
        }
        return buildAuthResponse(user);
    }

    // ─── Mise à jour push token ───────────────────────────────────────────────

    @Transactional
    public void updatePushToken(java.util.UUID userId, String pushToken) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPushToken(pushToken);
            userRepository.save(user);
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = toUserDetails(user);
        String accessToken  = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId().toString())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .expiresIn(expirationMs / 1000)
                .biometricExpiresAt(user.getBiometricExpiresAt() != null
                        ? user.getBiometricExpiresAt().toString() : null)
                .build();
    }

    UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.getIsActive())
                .build();
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return "***" + email.substring(atIdx);
        return email.charAt(0) + "***" + email.substring(atIdx - 1);
    }

    private String generateTempPhone() {
        return "+237" + String.format("%09d", (long) (Math.random() * 1_000_000_000L));
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    public record BiometricEnableResult(String biometricToken, String expiresAt) {}

    @Transactional(readOnly = true)
    public User loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }
}
