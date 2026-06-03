package cm.kmerfret.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginInitiateResponse {
    private String sessionToken;
    private int expiresIn;
    private String maskedEmail;
}
