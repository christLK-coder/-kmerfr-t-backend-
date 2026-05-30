package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SyncPositionRequest {

    @NotNull(message = "L'identifiant de la mission est obligatoire")
    private UUID missionId;

    @NotNull(message = "La latitude est obligatoire")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    private Double longitude;

    private BigDecimal speedKmh;
    private BigDecimal headingDeg;
    private BigDecimal accuracyM;

    @NotNull(message = "La date d'enregistrement est obligatoire")
    private OffsetDateTime recordedAt;

    private boolean wasOffline = false;

    public UUID getMissionId() { return missionId; }
    public void setMissionId(UUID missionId) { this.missionId = missionId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public BigDecimal getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(BigDecimal speedKmh) { this.speedKmh = speedKmh; }

    public BigDecimal getHeadingDeg() { return headingDeg; }
    public void setHeadingDeg(BigDecimal headingDeg) { this.headingDeg = headingDeg; }

    public BigDecimal getAccuracyM() { return accuracyM; }
    public void setAccuracyM(BigDecimal accuracyM) { this.accuracyM = accuracyM; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }

    public boolean isWasOffline() { return wasOffline; }
    public void setWasOffline(boolean wasOffline) { this.wasOffline = wasOffline; }
}
