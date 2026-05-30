package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.SyncPositionRequest;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.TelemetryPosition;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.TelemetryRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    private User currentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public int syncBatch(List<SyncPositionRequest> requests) {
        User driver = currentUser();
        List<TelemetryPosition> positions = requests.stream().map(req -> {
            Mission mission = missionRepository.findById(req.getMissionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Mission introuvable : " + req.getMissionId()));
            return TelemetryPosition.builder()
                    .mission(mission)
                    .driver(driver)
                    .position(GEO.createPoint(new Coordinate(req.getLongitude(), req.getLatitude())))
                    .speedKmh(req.getSpeedKmh())
                    .headingDeg(req.getHeadingDeg())
                    .accuracyM(req.getAccuracyM())
                    .recordedAt(req.getRecordedAt())
                    .wasOffline(req.isWasOffline())
                    .build();
        }).collect(Collectors.toList());
        return telemetryRepository.saveAll(positions).size();
    }
}
