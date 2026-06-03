package cm.kmerfret.backend.dto.response;

import cm.kmerfret.backend.model.Truck;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TruckResponse {

    private UUID id;
    private UUID driverId;
    private String driverName;
    private String plateNumber;
    private String brand;
    private String model;
    private BigDecimal capacityTons;
    private String truckType;
    private LocalDate insuranceExpiry;
    private Boolean docsVerified;
    private OffsetDateTime createdAt;

    public static TruckResponse from(Truck t) {
        TruckResponse r = new TruckResponse();
        r.id = t.getId();
        r.driverId = t.getDriver().getId();
        r.driverName = t.getDriver().getFullName();
        r.plateNumber = t.getPlateNumber();
        r.brand = t.getBrand();
        r.model = t.getModel();
        r.capacityTons = t.getCapacityTons();
        r.truckType = t.getTruckType() != null ? t.getTruckType().name() : null;
        r.insuranceExpiry = t.getInsuranceExpiry();
        r.docsVerified = t.getDocsVerified();
        r.createdAt = t.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getDriverId() { return driverId; }
    public String getDriverName() { return driverName; }
    public String getPlateNumber() { return plateNumber; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public BigDecimal getCapacityTons() { return capacityTons; }
    public String getTruckType() { return truckType; }
    public LocalDate getInsuranceExpiry() { return insuranceExpiry; }
    public Boolean getDocsVerified() { return docsVerified; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
