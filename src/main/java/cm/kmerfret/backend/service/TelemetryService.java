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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    // Le sujet JWT est l'email (cf. UserDetailsServiceImpl)
    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    /** Retourne la dernière position connue du chauffeur pour une mission. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getLatestPosition(UUID missionId) {
        List<TelemetryPosition> positions =
                telemetryRepository.findByMissionIdOrderByRecordedAtDesc(missionId);
        if (positions.isEmpty()) return Optional.empty();
        TelemetryPosition p = positions.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("lat",     p.getPosition().getY());
        result.put("lng",     p.getPosition().getX());
        result.put("speed",   p.getSpeedKmh()   != null ? p.getSpeedKmh().doubleValue()   : 0.0);
        result.put("heading", p.getHeadingDeg() != null ? p.getHeadingDeg().doubleValue() : 0.0);
        return Optional.of(result);
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
