package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findBySessionToken(String sessionToken);

    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.expiresAt < :now OR o.usedAt IS NOT NULL")
    void deleteExpiredAndUsed(OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.user.id = :userId AND o.purpose = :purpose AND o.usedAt IS NULL")
    void invalidatePreviousOtps(UUID userId, EmailOtp.Purpose purpose);
}
