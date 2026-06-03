package cm.kmerfret.backend.dto.response;

import cm.kmerfret.backend.model.RoadHazard;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class HazardResponse {

    private UUID id;
    private UUID reportedById;
    private UUID missionId;
    private Double latitude;
    private Double longitude;
    private BigDecimal altitudeM;
    private BigDecimal accuracyM;
    private BigDecimal shockMagnitude;
    private String shockAxis;
    private BigDecimal speedKmh;
    private String severity;
    private String hazardType;
    private Integer confirmationCount;
    private Boolean isValidated;
    private Boolean isRepaired;
    private OffsetDateTime recordedAt;
    private OffsetDateTime syncedAt;
    private Boolean wasOffline;

    public static HazardResponse from(RoadHazard h) {
        HazardResponse r = new HazardResponse();
        r.id = h.getId();
        r.reportedById = h.getReportedBy().getId();
        if (h.getMission() != null) r.missionId = h.getMission().getId();
        if (h.getLocation() != null) {
            r.longitude = h.getLocation().getX();
            r.latitude = h.getLocation().getY();
        }
        r.altitudeM = h.getAltitudeM();
        r.accuracyM = h.getAccuracyM();
        r.shockMagnitude = h.getShockMagnitude();
        r.shockAxis = h.getShockAxis();
        r.speedKmh = h.getSpeedKmh();
        r.severity = h.getSeverity() != null ? h.getSeverity().name() : null;
        r.hazardType = h.getHazardType() != null ? h.getHazardType().name() : null;
        r.confirmationCount = h.getConfirmationCount();
        r.isValidated = h.getIsValidated();
        r.isRepaired = h.getIsRepaired();
        r.recordedAt = h.getRecordedAt();
        r.syncedAt = h.getSyncedAt();
        r.wasOffline = h.getWasOffline();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getReportedById() { return reportedById; }
    public UUID getMissionId() { return missionId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public BigDecimal getAltitudeM() { return altitudeM; }
    public BigDecimal getAccuracyM() { return accuracyM; }
    public BigDecimal getShockMagnitude() { return shockMagnitude; }
    public String getShockAxis() { return shockAxis; }
    public BigDecimal getSpeedKmh() { return speedKmh; }
    public String getSeverity() { return severity; }
    public String getHazardType() { return hazardType; }
    public Integer getConfirmationCount() { return confirmationCount; }
    public Boolean getIsValidated() { return isValidated; }
    public Boolean getIsRepaired() { return isRepaired; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public OffsetDateTime getSyncedAt() { return syncedAt; }
    public Boolean getWasOffline() { return wasOffline; }
}
