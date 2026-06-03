package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByReviewedId(UUID reviewedId);

    Optional<Review> findByMissionIdAndReviewerId(UUID missionId, UUID reviewerId);

    boolean existsByMissionIdAndReviewerId(UUID missionId, UUID reviewerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewed.id = :userId")
    Optional<Double> avgRatingByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewed.id = :userId")
    long countByReviewedId(@Param("userId") UUID userId);
}
