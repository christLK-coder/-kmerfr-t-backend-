package cm.kmerfret.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "commission_policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommissionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Builder.Default
    @Column(name = "normal_rate", precision = 4, scale = 3)
    private BigDecimal normalRate = new BigDecimal("0.075");

    @Builder.Default
    @Column(name = "express_rate", precision = 4, scale = 3)
    private BigDecimal expressRate = new BigDecimal("0.100");

    @Builder.Default
    @Column(name = "express_surcharge", precision = 4, scale = 3)
    private BigDecimal expressSurcharge = new BigDecimal("0.300");

    @Builder.Default
    @Column(name = "first_deposit_pct", precision = 4, scale = 3)
    private BigDecimal firstDepositPct = new BigDecimal("0.500");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
