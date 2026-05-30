package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CreateMissionRequest {

    @NotNull(message = "La latitude d'origine est obligatoire")
    private Double originLat;

    @NotNull(message = "La longitude d'origine est obligatoire")
    private Double originLng;

    @NotBlank(message = "Le libellé d'origine est obligatoire")
    private String originLabel;

    @NotNull(message = "La latitude de destination est obligatoire")
    private Double destinationLat;

    @NotNull(message = "La longitude de destination est obligatoire")
    private Double destinationLng;

    @NotBlank(message = "Le libellé de destination est obligatoire")
    private String destinationLabel;

    @NotBlank(message = "La description de la cargaison est obligatoire")
    private String cargoDescription;

    @NotNull(message = "Le poids de la cargaison est obligatoire")
    @DecimalMin(value = "0.01", message = "Le poids doit être positif")
    private BigDecimal cargoWeightTons;

    @NotBlank(message = "Le type de cargaison est obligatoire")
    @Pattern(regexp = "^(GENERAL|DANGEROUS|PERISHABLE|OVERSIZED|LIQUID|CONTAINER)$",
             message = "Type de cargaison invalide")
    private String cargoType;

    private String specialInstructions;

    @NotNull(message = "Le prix total est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix doit être positif")
    private BigDecimal totalPrice;

    @Pattern(regexp = "^(MTN_MOMO|ORANGE_MONEY|STRIPE|PAYPAL)$",
             message = "Méthode de paiement invalide")
    private String paymentMethod;

    private UUID truckId;

    private OffsetDateTime pickupScheduledAt;

    public Double getOriginLat() { return originLat; }
    public void setOriginLat(Double originLat) { this.originLat = originLat; }

    public Double getOriginLng() { return originLng; }
    public void setOriginLng(Double originLng) { this.originLng = originLng; }

    public String getOriginLabel() { return originLabel; }
    public void setOriginLabel(String originLabel) { this.originLabel = originLabel; }

    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }

    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }

    public String getDestinationLabel() { return destinationLabel; }
    public void setDestinationLabel(String destinationLabel) { this.destinationLabel = destinationLabel; }

    public String getCargoDescription() { return cargoDescription; }
    public void setCargoDescription(String cargoDescription) { this.cargoDescription = cargoDescription; }

    public BigDecimal getCargoWeightTons() { return cargoWeightTons; }
    public void setCargoWeightTons(BigDecimal cargoWeightTons) { this.cargoWeightTons = cargoWeightTons; }

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public UUID getTruckId() { return truckId; }
    public void setTruckId(UUID truckId) { this.truckId = truckId; }

    public OffsetDateTime getPickupScheduledAt() { return pickupScheduledAt; }
    public void setPickupScheduledAt(OffsetDateTime pickupScheduledAt) { this.pickupScheduledAt = pickupScheduledAt; }
}
