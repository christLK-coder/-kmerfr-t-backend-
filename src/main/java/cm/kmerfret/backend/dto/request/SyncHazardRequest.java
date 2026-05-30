package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SyncHazardRequest {

    private UUID missionId;

    @NotNull(message = "La latitude est obligatoire")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    private Double longitude;

    private BigDecimal altitudeM;
    private BigDecimal accuracyM;

    @NotNull(message = "La magnitude du choc est obligatoire")
    private BigDecimal shockMagnitude;

    private String shockAxis = "Z";
    private BigDecimal speedKmh;

    @NotBlank(message = "La sévérité est obligatoire")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "Sévérité invalide")
    private String severity;

    @Pattern(regexp = "^(POTHOLE|CRACK|BUMP|FLOODING|LANDSLIDE|BROKEN_ROAD)$",
             message = "Type de danger invalide")
    private String hazardType = "POTHOLE";

    @NotNull(message = "La date d'enregistrement est obligatoire")
    private OffsetDateTime recordedAt;

    private boolean wasOffline = false;

    public UUID getMissionId() { return missionId; }
    public void setMissionId(UUID missionId) { this.missionId = missionId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public BigDecimal getAltitudeM() { return altitudeM; }
    public void setAltitudeM(BigDecimal altitudeM) { this.altitudeM = altitudeM; }

    public BigDecimal getAccuracyM() { return accuracyM; }
    public void setAccuracyM(BigDecimal accuracyM) { this.accuracyM = accuracyM; }

    public BigDecimal getShockMagnitude() { return shockMagnitude; }
    public void setShockMagnitude(BigDecimal shockMagnitude) { this.shockMagnitude = shockMagnitude; }

    public String getShockAxis() { return shockAxis; }
    public void setShockAxis(String shockAxis) { this.shockAxis = shockAxis; }

    public BigDecimal getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(BigDecimal speedKmh) { this.speedKmh = speedKmh; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getHazardType() { return hazardType; }
    public void setHazardType(String hazardType) { this.hazardType = hazardType; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }

    public boolean isWasOffline() { return wasOffline; }
    public void setWasOffline(boolean wasOffline) { this.wasOffline = wasOffline; }
}
