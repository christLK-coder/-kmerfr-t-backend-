package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TruckRepository extends JpaRepository<Truck, UUID> {
    List<Truck> findByDriverId(UUID driverId);
}
