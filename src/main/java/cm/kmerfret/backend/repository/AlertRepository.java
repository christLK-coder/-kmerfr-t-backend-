package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByMissionIdOrderByCreatedAtDesc(UUID missionId);
    List<Alert> findBySenderIdOrderByCreatedAtDesc(UUID senderId);
}
