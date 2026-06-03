package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class BiometricLoginRequest {
    @NotNull
    private UUID userId;

    @NotBlank
    private String biometricToken;
}
