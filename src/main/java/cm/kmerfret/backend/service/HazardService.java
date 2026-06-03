package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.SyncHazardRequest;
import cm.kmerfret.backend.dto.response.HazardResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.RoadHazard;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.HazardRepository;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HazardService {

    private final HazardRepository hazardRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    private User currentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private Point point(double lng, double lat) {
        return GEO.createPoint(new Coordinate(lng, lat));
    }

    private RoadHazard buildHazard(SyncHazardRequest req, User reporter) {
        RoadHazard.RoadHazardBuilder builder = RoadHazard.builder()
                .reportedBy(reporter)
                .location(point(req.getLongitude(), req.getLatitude()))
                .altitudeM(req.getAltitudeM())
                .accuracyM(req.getAccuracyM())
                .shockMagnitude(req.getShockMagnitude())
                .shockAxis(req.getShockAxis() != null ? req.getShockAxis() : "Z")
                .speedKmh(req.getSpeedKmh())
                .severity(RoadHazard.Severity.valueOf(req.getSeverity()))
                .hazardType(req.getHazardType() != null
                        ? RoadHazard.HazardType.valueOf(req.getHazardType())
                        : RoadHazard.HazardType.POTHOLE)
                .recordedAt(req.getRecordedAt())
                .syncedAt(OffsetDateTime.now())
                .wasOffline(req.isWasOffline());

        if (req.getMissionId() != null) {
            Mission mission = missionRepository.findById(req.getMissionId()).orElse(null);
            builder.mission(mission);
        }
        return builder.build();
    }

    @Transactional
    public HazardResponse createHazard(SyncHazardRequest req) {
        User reporter = currentUser();
        return HazardResponse.from(hazardRepository.save(buildHazard(req, reporter)));
    }

    @Transactional
    public List<HazardResponse> createBatch(List<SyncHazardRequest> requests) {
        User reporter = currentUser();
        List<RoadHazard> hazards = requests.stream()
                .map(req -> buildHazard(req, reporter))
                .collect(Collectors.toList());
        return hazardRepository.saveAll(hazards).stream()
                .map(HazardResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HazardResponse> getInBoundingBox(double lat1, double lng1, double lat2, double lng2) {
        return hazardRepository.findInBoundingBox(lat1, lng1, lat2, lng2)
                .stream().map(HazardResponse::from).collect(Collectors.toList());
    }
}
