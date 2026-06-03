package cm.kmerfret.backend.dto.response;

import cm.kmerfret.backend.model.Mission;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class MissionResponse {

    private UUID id;
    private String status;
    private String missionType;
    private String paymentStatus;
    private String paymentMethod;

    private UUID importerId;
    private String importerName;
    private String importerEmail;
    private String importerPhone;

    private UUID driverId;
    private String driverName;
    private String driverEmail;
    private String driverPhone;
    private Double driverAvgRating;

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
    private BigDecimal firstPaymentAmount;

    private String qrDeliveryToken;
    private OffsetDateTime pickupScheduledAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime transitStartedAt;
    private OffsetDateTime driverArrivedAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime firstPaymentAt;
    private OffsetDateTime secondPaymentAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MissionResponse from(Mission m) {
        MissionResponse r = new MissionResponse();
        r.id             = m.getId();
        r.status         = m.getStatus() != null ? m.getStatus().name() : null;
        r.missionType    = m.getMissionType() != null ? m.getMissionType().name() : "NORMAL";
        r.paymentStatus  = m.getPaymentStatus() != null ? m.getPaymentStatus().name() : null;
        r.paymentMethod  = m.getPaymentMethod() != null ? m.getPaymentMethod().name() : null;

        r.importerId    = m.getImporter().getId();
        r.importerName  = m.getImporter().getFullName();
        r.importerEmail = m.getImporter().getEmail();
        r.importerPhone = m.getImporter().getPhone();

        if (m.getDriver() != null) {
            r.driverId    = m.getDriver().getId();
            r.driverName  = m.getDriver().getFullName();
            r.driverEmail = m.getDriver().getEmail();
            r.driverPhone = m.getDriver().getPhone();
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

        r.originLabel       = m.getOriginLabel();
        r.destinationLabel  = m.getDestinationLabel();
        r.distanceKm        = m.getDistanceKm();
        r.cargoDescription  = m.getCargoDescription();
        r.cargoWeightTons   = m.getCargoWeightTons();
        r.cargoType         = m.getCargoType() != null ? m.getCargoType().name() : null;
        r.specialInstructions = m.getSpecialInstructions();

        r.totalPrice        = m.getTotalPrice();
        r.commissionRate    = m.getCommissionRate();
        r.commissionAmount  = m.getCommissionAmount();
        r.driverPayout      = m.getDriverPayout();
        r.firstPaymentAmount = m.getFirstPaymentAmount();

        r.qrDeliveryToken   = m.getQrDeliveryToken();
        r.pickupScheduledAt = m.getPickupScheduledAt();
        r.startedAt         = m.getStartedAt();
        r.transitStartedAt  = m.getTransitStartedAt();
        r.driverArrivedAt   = m.getDriverArrivedAt();
        r.deliveredAt       = m.getDeliveredAt();
        r.firstPaymentAt    = m.getFirstPaymentAt();
        r.secondPaymentAt   = m.getSecondPaymentAt();
        r.createdAt         = m.getCreatedAt();
        r.updatedAt         = m.getUpdatedAt();
        return r;
    }
}
