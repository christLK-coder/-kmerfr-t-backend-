package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateMissionRequest {

    @NotNull
    private Double originLat;
    @NotNull
    private Double originLng;
    @NotBlank
    private String originLabel;

    @NotNull
    private Double destinationLat;
    @NotNull
    private Double destinationLng;
    @NotBlank
    private String destinationLabel;

    @NotBlank
    private String cargoDescription;

    @NotNull @DecimalMin("0.01")
    private BigDecimal cargoWeightTons;

    @NotBlank
    private String cargoType;

    private String specialInstructions;

    @NotNull @DecimalMin("0.01")
    private BigDecimal totalPrice;

    @Pattern(regexp = "^(MTN_MOMO|ORANGE_MONEY|STRIPE|PAYPAL)$")
    private String paymentMethod;

    private UUID truckId;

    private String pickupScheduledAt;

    @Pattern(regexp = "^(NORMAL|EXPRESS)$")
    private String missionType = "NORMAL";
}
