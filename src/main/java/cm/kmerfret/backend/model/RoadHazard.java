package cm.kmerfret.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "road_hazards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoadHazard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Column(name = "location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    @Column(name = "altitude_m", precision = 8, scale = 2)
    private BigDecimal altitudeM;

    @Column(name = "accuracy_m", precision = 6, scale = 2)
    private BigDecimal accuracyM;

    @Column(name = "shock_magnitude", nullable = false, precision = 8, scale = 4)
    private BigDecimal shockMagnitude;

    @Builder.Default
    @Column(name = "shock_axis", length = 5)
    private String shockAxis = "Z";

    @Column(name = "speed_kmh", precision = 5, scale = 2)
    private BigDecimal speedKmh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "hazard_type", length = 30)
    private HazardType hazardType = HazardType.POTHOLE;

    @Builder.Default
    @Column(name = "confirmation_count")
    private Integer confirmationCount = 1;

    @Builder.Default
    @Column(name = "is_validated")
    private Boolean isValidated = false;

    @Builder.Default
    @Column(name = "is_repaired")
    private Boolean isRepaired = false;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    @Builder.Default
    @Column(name = "was_offline")
    private Boolean wasOffline = false;

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum HazardType { POTHOLE, CRACK, BUMP, FLOODING, LANDSLIDE, BROKEN_ROAD }
}
