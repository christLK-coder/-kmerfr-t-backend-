package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateTruckRequest {

    @NotBlank(message = "La plaque d'immatriculation est obligatoire")
    private String plateNumber;

    private String brand;
    private String model;

    @NotNull(message = "La capacité en tonnes est obligatoire")
    @DecimalMin(value = "0.1", message = "La capacité doit être positive")
    private BigDecimal capacityTons;

    @NotBlank(message = "Le type de camion est obligatoire")
    @Pattern(regexp = "^(FLATBED|TANKER|REFRIGERATED|CONTAINER_20|CONTAINER_40|TIPPER)$",
             message = "Type de camion invalide")
    private String truckType;

    private LocalDate insuranceExpiry;

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public BigDecimal getCapacityTons() { return capacityTons; }
    public void setCapacityTons(BigDecimal capacityTons) { this.capacityTons = capacityTons; }

    public String getTruckType() { return truckType; }
    public void setTruckType(String truckType) { this.truckType = truckType; }

    public LocalDate getInsuranceExpiry() { return insuranceExpiry; }
    public void setInsuranceExpiry(LocalDate insuranceExpiry) { this.insuranceExpiry = insuranceExpiry; }
}
