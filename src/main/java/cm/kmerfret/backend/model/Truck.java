package cm.kmerfret.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trucks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(length = 80)
    private String brand;

    @Column(length = 80)
    private String model;

    @Column(name = "capacity_tons", nullable = false, precision = 6, scale = 2)
    private BigDecimal capacityTons;

    @Enumerated(EnumType.STRING)
    @Column(name = "truck_type", nullable = false, length = 40)
    private TruckType truckType;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Builder.Default
    @Column(name = "docs_verified")
    private Boolean docsVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum TruckType {
        FLATBED, TANKER, REFRIGERATED, CONTAINER_20, CONTAINER_40, TIPPER
    }
}
