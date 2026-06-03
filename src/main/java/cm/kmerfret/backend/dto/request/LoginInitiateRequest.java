package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginInitiateRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}
