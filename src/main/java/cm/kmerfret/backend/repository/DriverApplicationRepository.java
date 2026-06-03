package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.DriverApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverApplicationRepository extends JpaRepository<DriverApplication, UUID> {
    boolean existsByEmail(String email);
    Optional<DriverApplication> findByEmail(String email);
    List<DriverApplication> findByStatusOrderBySubmittedAtDesc(DriverApplication.Status status);
    List<DriverApplication> findAllByOrderBySubmittedAtDesc();
}
