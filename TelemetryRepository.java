package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.TelemetryPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TelemetryRepository extends JpaRepository<TelemetryPosition, Long> {
    List<TelemetryPosition> findByMissionIdOrderByRecordedAtDesc(UUID missionId);
}
