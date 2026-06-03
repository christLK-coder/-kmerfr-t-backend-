package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = 'DRIVER' AND (u.accountStatus = 'ACTIVE' OR u.accountStatus IS NULL) AND u.isActive = true AND u.pushToken IS NOT NULL")
    List<User> findActiveDriversWithPushToken();

    @Query("SELECT u FROM User u WHERE u.role = :role AND (u.accountStatus = 'ACTIVE' OR u.accountStatus IS NULL) AND u.isActive = true")
    List<User> findActiveByRole(User.Role role);
}
