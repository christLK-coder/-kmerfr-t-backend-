package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.MissionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MissionDocumentRepository extends JpaRepository<MissionDocument, UUID> {
    List<MissionDocument> findByMissionId(UUID missionId);
}
