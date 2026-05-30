package cm.kmerfret.backend.dto.response;

import cm.kmerfret.backend.model.Mission;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class MissionResponse {

    private UUID id;
    private UUID importerId;
    private String importerName;
    private UUID driverId;
    private String driverName;
    private UUID truckId;
    private Double originLat;
    private Double originLng;
    private String originLabel;
    private Double destinationLat;
    private Double destinationLng;
    private String destinationLabel;
    private BigDecimal distanceKm;
    private String cargoDescription;
    private BigDecimal cargoWeightTons;
    private String cargoType;
    private String specialInstructions;
    private BigDecimal totalPrice;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal driverPayout;
    private String paymentStatus;
    private String paymentMethod;
    private String status;
    private String qrDeliveryToken;
    private OffsetDateTime pickupScheduledAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MissionResponse from(Mission m) {
        MissionResponse r = new MissionResponse();
        r.id = m.getId();
        r.importerId = m.getImporter().getId();
        r.importerName = m.getImporter().getFullName();
        if (m.getDriver() != null) {
            r.driverId = m.getDriver().getId();
            r.driverName = m.getDriver().getFullName();
        }
        if (m.getTruck() != null) r.truckId = m.getTruck().getId();
        if (m.getOriginPoint() != null) {
            r.originLng = m.getOriginPoint().getX();
            r.originLat = m.getOriginPoint().getY();
        }
        if (m.getDestinationPoint() != null) {
            r.destinationLng = m.getDestinationPoint().getX();
            r.destinationLat = m.getDestinationPoint().getY();
        }
        r.originLabel = m.getOriginLabel();
        r.destinationLabel = m.getDestinationLabel();
        r.distanceKm = m.getDistanceKm();
        r.cargoDescription = m.getCargoDescription();
        r.cargoWeightTons = m.getCargoWeightTons();
        r.cargoType = m.getCargoType() != null ? m.getCargoType().name() : null;
        r.specialInstructions = m.getSpecialInstructions();
        r.totalPrice = m.getTotalPrice();
        r.commissionRate = m.getCommissionRate();
        r.commissionAmount = m.getCommissionAmount();
        r.driverPayout = m.getDriverPayout();
        r.paymentStatus = m.getPaymentStatus() != null ? m.getPaymentStatus().name() : null;
        r.paymentMethod = m.getPaymentMethod() != null ? m.getPaymentMethod().name() : null;
        r.status = m.getStatus() != null ? m.getStatus().name() : null;
        r.qrDeliveryToken = m.getQrDeliveryToken();
        r.pickupScheduledAt = m.getPickupScheduledAt();
        r.startedAt = m.getStartedAt();
        r.deliveredAt = m.getDeliveredAt();
        r.createdAt = m.getCreatedAt();
        r.updatedAt = m.getUpdatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getImporterId() { return importerId; }
    public String getImporterName() { return importerName; }
    public UUID getDriverId() { return driverId; }
    public String getDriverName() { return driverName; }
    public UUID getTruckId() { return truckId; }
    public Double getOriginLat() { return originLat; }
    public Double getOriginLng() { return originLng; }
    public String getOriginLabel() { return originLabel; }
    public Double getDestinationLat() { return destinationLat; }
    public Double getDestinationLng() { return destinationLng; }
    public String getDestinationLabel() { return destinationLabel; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public String getCargoDescription() { return cargoDescription; }
    public BigDecimal getCargoWeightTons() { return cargoWeightTons; }
    public String getCargoType() { return cargoType; }
    public String getSpecialInstructions() { return specialInstructions; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public BigDecimal getDriverPayout() { return driverPayout; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public String getQrDeliveryToken() { return qrDeliveryToken; }
    public OffsetDateTime getPickupScheduledAt() { return pickupScheduledAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
