package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByMissionIdOrderByCreatedAtDesc(UUID missionId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.mission.id = :missionId AND m.sender.id != :userId AND m.isRead = false")
    long countUnread(UUID missionId, UUID userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.mission.id = :missionId AND m.sender.id != :userId")
    void markAllReadForUser(UUID missionId, UUID userId);
}
