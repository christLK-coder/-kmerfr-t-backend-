package cm.kmerfret.backend.service;

import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.ChatMessage;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.ChatMessageRepository;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatRepo;
    private final MissionRepository     missionRepo;
    private final UserRepository        userRepo;
    private final PushNotificationService pushService;

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getHistory(UUID missionId, int page) {
        PageRequest pr = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        return chatRepo.findByMissionIdOrderByCreatedAtDesc(missionId, pr)
                .stream().map(ChatMessageDto::from).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto save(UUID missionId, UUID senderId, String content, String msgType) {
        Mission mission = missionRepo.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable"));
        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        ChatMessage msg = ChatMessage.builder()
                .mission(mission).sender(sender).content(content)
                .msgType(ChatMessage.MsgType.valueOf(msgType != null ? msgType : "TEXT"))
                .build();
        ChatMessage saved = chatRepo.save(msg);

        // Push notification a l'autre partie
        User recipient = sender.getId().equals(mission.getImporter().getId())
                ? mission.getDriver() : mission.getImporter();
        if (recipient != null && recipient.getPushToken() != null) {
            pushService.sendToToken(recipient.getPushToken(),
                    sender.getFullName(), content,
                    java.util.Map.of("type", "NEW_CHAT_MESSAGE", "missionId", missionId.toString()));
        }

        return ChatMessageDto.from(saved);
    }

    @Transactional
    public void markRead(UUID missionId, UUID userId) {
        chatRepo.markAllReadForUser(missionId, userId);
    }

    public record ChatMessageDto(
            String id, String missionId, String senderId, String senderName,
            String content, String msgType, boolean isRead, String createdAt) {
        public static ChatMessageDto from(ChatMessage m) {
            return new ChatMessageDto(
                    m.getId().toString(), m.getMission().getId().toString(),
                    m.getSender().getId().toString(), m.getSender().getFullName(),
                    m.getContent(), m.getMsgType().name(), m.getIsRead(),
                    m.getCreatedAt().toString());
        }
    }
}
