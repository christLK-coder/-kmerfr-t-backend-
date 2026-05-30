package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    List<Mission> findByStatus(Mission.MissionStatus status);
    List<Mission> findByImporterId(UUID importerId);
    List<Mission> findByDriverId(UUID driverId);
    Optional<Mission> findByQrDeliveryToken(String token);
}
