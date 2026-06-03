package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginVerifyRequest {
    @NotBlank
    private String sessionToken;

    @NotBlank
    @Size(min = 6, max = 6, message = "Le code doit contenir 6 chiffres")
    private String otpCode;
}
