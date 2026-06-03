package cm.kmerfret.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final RestTemplate restTemplate;
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    public void sendToToken(String expoPushToken, String title, String body, Map<String, Object> data) {
        if (expoPushToken == null || !expoPushToken.startsWith("ExponentPushToken[")) {
            log.debug("Token push invalide ou absent : {}", expoPushToken);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Encoding", "gzip, deflate");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", expoPushToken);
            payload.put("sound", "default");
            payload.put("title", title);
            payload.put("body", body);
            payload.put("priority", "high");
            payload.put("channelId", "kmerfret");
            if (data != null) payload.put("data", data);

            restTemplate.exchange(EXPO_PUSH_URL, HttpMethod.POST,
                    new HttpEntity<>(payload, headers), String.class);
            log.info("Push envoyé → {} : {}", expoPushToken.substring(0, 30), title);
        } catch (Exception e) {
            log.warn("Erreur envoi push : {}", e.getMessage());
        }
    }

    public void sendToTokens(List<String> tokens, String title, String body, Map<String, Object> data) {
        tokens.forEach(t -> sendToToken(t, title, body, data));
    }
}
