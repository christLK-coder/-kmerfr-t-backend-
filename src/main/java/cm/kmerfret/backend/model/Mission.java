package cm.kmerfret.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "missions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importer_id", nullable = false)
    private User importer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id")
    private Truck truck;

    @Column(name = "origin_point", columnDefinition = "geography(Point,4326)")
    private Point originPoint;

    @Column(name = "destination_point", columnDefinition = "geography(Point,4326)")
    private Point destinationPoint;

    @Column(name = "origin_label", nullable = false, length = 255)
    private String originLabel;

    @Column(name = "destination_label", nullable = false, length = 255)
    private String destinationLabel;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "cargo_description", nullable = false)
    private String cargoDescription;

    @Column(name = "cargo_weight_tons", nullable = false, precision = 6, scale = 2)
    private BigDecimal cargoWeightTons;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_type", nullable = false, length = 40)
    private CargoType cargoType;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Builder.Default
    @Column(name = "commission_rate", precision = 4, scale = 3)
    private BigDecimal commissionRate = new BigDecimal("0.075");

    // Colonnes GENERATED ALWAYS AS STORED — lecture seule côté Hibernate
    @Column(name = "commission_amount", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal commissionAmount;

    @Column(name = "driver_payout", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal driverPayout;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MissionStatus status = MissionStatus.OPEN;

    @Column(name = "qr_delivery_token", unique = true, length = 255)
    private String qrDeliveryToken;

    @Column(name = "qr_scanned_at")
    private OffsetDateTime qrScannedAt;

    @Column(name = "pickup_scheduled_at")
    private OffsetDateTime pickupScheduledAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum MissionStatus { OPEN, ASSIGNED, IN_TRANSIT, DELIVERED, CANCELLED, DISPUTED }
    public enum PaymentStatus { PENDING, ESCROWED, RELEASED, REFUNDED, DISPUTED }
    public enum PaymentMethod { MTN_MOMO, ORANGE_MONEY, STRIPE, PAYPAL }
    public enum CargoType { GENERAL, DANGEROUS, PERISHABLE, OVERSIZED, LIQUID, CONTAINER }
}
