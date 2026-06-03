package cm.kmerfret.backend.config;

import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Réinitialise les mots de passe des comptes spécifiés au démarrage.
 * À supprimer après utilisation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetRunner implements ApplicationRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    private static final String NEW_PASSWORD = "Kmerfret2024!";

    private static final List<String> TARGET_EMAILS = List.of(
        "christarmandlemongo@gmail.com",
        "davashopoff@gmail.com"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║           VÉRIFICATION DES COMPTES               ║");
        log.info("╚══════════════════════════════════════════════════╝");

        // Rechercher aussi les emails partiels (ahmaou...)
        List<User> allUsers = userRepo.findAll();

        for (String email : TARGET_EMAILS) {
            Optional<User> opt = allUsers.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();

            if (opt.isPresent()) {
                User u = opt.get();
                u.setPasswordHash(encoder.encode(NEW_PASSWORD));
                u.setEmailVerified(true);
                u.setIsActive(true);
                u.setAccountStatus(User.AccountStatus.ACTIVE);
                userRepo.save(u);
                log.info("✅ EXISTE & MDP RÉINITIALISÉ : {} | {} | {}",
                    u.getEmail(), u.getRole(), u.getFullName());
            } else {
                log.info("❌ N'EXISTE PAS dans la BD   : {}", email);
            }
        }

        // Chercher les comptes contenant "ahmaou" ou "ahmao"
        allUsers.stream()
            .filter(u -> u.getEmail().toLowerCase().contains("ahmaou")
                      || u.getEmail().toLowerCase().contains("ahmao"))
            .forEach(u -> {
                u.setPasswordHash(encoder.encode(NEW_PASSWORD));
                u.setEmailVerified(true);
                u.setIsActive(true);
                u.setAccountStatus(User.AccountStatus.ACTIVE);
                userRepo.save(u);
                log.info("✅ EXISTE & MDP RÉINITIALISÉ : {} | {} | {}",
                    u.getEmail(), u.getRole(), u.getFullName());
            });

        log.info("──────────────────────────────────────────────────");
        log.info("  Nouveau mot de passe pour tous : {}", NEW_PASSWORD);
        log.info("══════════════════════════════════════════════════");
    }
}
