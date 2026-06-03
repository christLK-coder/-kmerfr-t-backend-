package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.PlatformWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, UUID> {
    List<PlatformWallet> findByMissionId(UUID missionId);
    Optional<PlatformWallet> findByExternalRef(String externalRef);

    @Query("SELECT COALESCE(SUM(w.amount),0) FROM PlatformWallet w WHERE w.status = 'CONFIRMED' AND w.transactionType = 'DEPOSIT'")
    java.math.BigDecimal totalEscrowed();

    @Query("SELECT COALESCE(SUM(w.amount),0) FROM PlatformWallet w WHERE w.status = 'CONFIRMED' AND w.transactionType = 'COMMISSION'")
    java.math.BigDecimal totalCommissions();
}
