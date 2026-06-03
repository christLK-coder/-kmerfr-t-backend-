package cm.kmerfret.backend.repository;

import cm.kmerfret.backend.model.RoadHazard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface HazardRepository extends JpaRepository<RoadHazard, UUID> {

    @Query(value = """
        SELECT * FROM road_hazards
        WHERE ST_Within(location::geometry,
            ST_MakeEnvelope(:lng1, :lat1, :lng2, :lat2, 4326))
        AND is_repaired = false
        ORDER BY recorded_at DESC
        LIMIT 500
        """, nativeQuery = true)
    List<RoadHazard> findInBoundingBox(
        @Param("lat1") double lat1,
        @Param("lng1") double lng1,
        @Param("lat2") double lat2,
        @Param("lng2") double lng2
    );
}
