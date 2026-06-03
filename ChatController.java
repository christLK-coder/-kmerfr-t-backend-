package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.service.ChatService;
import cm.kmerfret.backend.service.ChatService.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messaging;

    // REST — historique des messages (pagination)
    @GetMapping("/api/chat/mission/{missionId}")
    public ApiResponse<List<ChatMessageDto>> history(
            @PathVariable UUID missionId,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(chatService.getHistory(missionId, page));
    }

    // REST — marquer comme lu
    @PutMapping("/api/chat/mission/{missionId}/read")
    public ApiResponse<Void> markRead(
            @PathVariable UUID missionId,
            @AuthenticationPrincipal UserDetails principal) {
        // resolve userId from email principal
        return ApiResponse.ok(null);
    }

    // WebSocket STOMP — envoyer un message
    @MessageMapping("/mission/{missionId}/chat")
    public void sendMessage(
            @DestinationVariable String missionId,
            @Payload Map<String, String> payload,
            @Header("senderId") String senderId) {

        ChatMessageDto dto = chatService.save(
                UUID.fromString(missionId),
                UUID.fromString(senderId),
                payload.get("content"),
                payload.getOrDefault("msgType", "TEXT"));

        // Broadcaster vers tous les abonnes du topic
        messaging.convertAndSend("/topic/mission/" + missionId + "/chat", dto);
    }

    // WebSocket — position en temps reel (chauffeur -> importer)
    @MessageMapping("/mission/{missionId}/position")
    @SendTo("/topic/mission/{missionId}/position")
    public Map<String, Object> broadcastPosition(
            @DestinationVariable String missionId,
            @Payload Map<String, Object> position) {
        position.put("missionId", missionId);
        return position;
    }
}
