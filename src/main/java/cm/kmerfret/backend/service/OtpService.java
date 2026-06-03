package cm.kmerfret.backend.service;

import cm.kmerfret.backend.model.EmailOtp;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiration-minutes:10}")
    private int expirationMinutes;

    @Value("${app.otp.length:6}")
    private int otpLength;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public OtpResult generate(User user, EmailOtp.Purpose purpose) {
        // Invalider les anciens OTP non utilisés pour ce user/purpose
        otpRepository.invalidatePreviousOtps(user.getId(), purpose);

        String rawCode = generateNumericCode(otpLength);
        String sessionToken = generateSessionToken();

        EmailOtp otp = EmailOtp.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(rawCode))
                .sessionToken(sessionToken)
                .purpose(purpose)
                .expiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes))
                .build();

        otpRepository.save(otp);
        // Visible dans les logs pour les tests — à ne pas laisser en prod
        log.info("╔══════════════════════════════╗");
        log.info("║  OTP {} → {} ║", user.getEmail(), rawCode);
        log.info("╚══════════════════════════════╝");
        return new OtpResult(rawCode, sessionToken, expirationMinutes * 60);
    }

    @Transactional
    public Optional<User> validate(String sessionToken, String rawCode) {
        return otpRepository.findBySessionToken(sessionToken)
                .filter(otp -> !otp.isExpired())
                .filter(otp -> !otp.isUsed())
                .filter(otp -> passwordEncoder.matches(rawCode, otp.getCodeHash()))
                .map(otp -> {
                    otp.setUsedAt(OffsetDateTime.now());
                    otpRepository.save(otp);
                    return otp.getUser();
                });
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpired() {
        otpRepository.deleteExpiredAndUsed(OffsetDateTime.now());
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record OtpResult(String rawCode, String sessionToken, int expiresIn) {}
}
