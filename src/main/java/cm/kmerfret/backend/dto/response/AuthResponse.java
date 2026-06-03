package cm.kmerfret.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private long expiresIn;
    private String biometricExpiresAt;
}
