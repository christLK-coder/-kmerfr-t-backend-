package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.CommissionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, UUID> {
    Optional<CommissionPolicy> findFirstByOrderByUpdatedAtDesc();
}
