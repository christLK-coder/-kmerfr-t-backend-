package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.ApplicationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ApplicationMessageRepository extends JpaRepository<ApplicationMessage, UUID> {
    List<ApplicationMessage> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
