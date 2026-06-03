package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

    List<Mission> findByStatus(Mission.MissionStatus status);

    @Query("SELECT m FROM Mission m WHERE m.importer.id = :id ORDER BY m.createdAt DESC")
    List<Mission> findByImporterId(@Param("id") UUID importerId);

    @Query("SELECT m FROM Mission m WHERE m.driver.id = :id ORDER BY m.createdAt DESC")
    List<Mission> findByDriverId(@Param("id") UUID driverId);

    @Query("SELECT m FROM Mission m WHERE m.importer.id = :id AND m.status IN :statuses ORDER BY m.pickupScheduledAt ASC NULLS LAST")
    List<Mission> findByImporterIdAndStatusIn(@Param("id") UUID importerId, @Param("statuses") List<Mission.MissionStatus> statuses);

    @Query("SELECT m FROM Mission m WHERE m.importer.id = :id AND m.status IN :statuses ORDER BY m.deliveredAt DESC, m.createdAt DESC")
    List<Mission> findByImporterIdAndStatusInHistory(@Param("id") UUID importerId, @Param("statuses") List<Mission.MissionStatus> statuses);

    @Query("SELECT m FROM Mission m WHERE m.driver.id = :id AND m.status IN :statuses ORDER BY m.pickupScheduledAt ASC NULLS LAST")
    List<Mission> findByDriverIdAndStatusIn(@Param("id") UUID driverId, @Param("statuses") List<Mission.MissionStatus> statuses);

    Optional<Mission> findByQrDeliveryToken(String token);

    // Missions OPEN avec paiement non effectué et départ imminent — pour relances
    @Query("SELECT m FROM Mission m WHERE m.status = 'ASSIGNED' AND m.paymentStatus = 'PENDING' AND m.pickupScheduledAt < :threshold")
    List<Mission> findAssignedUnpaidBefore(@Param("threshold") OffsetDateTime threshold);

    // Missions actives d'un chauffeur (IN_TRANSIT ou ASSIGNED)
    @Query("SELECT m FROM Mission m WHERE m.driver.id = :driverId AND m.status IN ('ASSIGNED','IN_TRANSIT')")
    Optional<Mission> findActiveByDriverId(@Param("driverId") UUID driverId);

    Page<Mission> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
