package cm.kmerfret.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telemetry_positions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TelemetryPosition {

    // Clé primaire BIGSERIAL (Long) conforme au schéma init.sql
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "position", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point position;

    @Column(name = "speed_kmh", precision = 5, scale = 2)
    private BigDecimal speedKmh;

    @Column(name = "heading_deg", precision = 6, scale = 2)
    private BigDecimal headingDeg;

    @Column(name = "accuracy_m", precision = 6, scale = 2)
    private BigDecimal accuracyM;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Builder.Default
    @Column(name = "was_offline")
    private Boolean wasOffline = false;
}
