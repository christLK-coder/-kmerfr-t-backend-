package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DriverApplicationRequest {

    @NotBlank @Size(min = 3, max = 150)
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank @Pattern(regexp = "^\\+?[0-9]{9,15}$")
    private String phone;

    @Size(max = 100)
    private String city;

    @Size(max = 1000)
    private String motivation;
}
