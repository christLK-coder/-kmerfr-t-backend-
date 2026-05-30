package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.CreateMissionRequest;
import cm.kmerfret.backend.dto.response.MissionResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.exception.UnauthorizedException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.Truck;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.TruckRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final TruckRepository truckRepository;

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    private User currentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private Point point(double lng, double lat) {
        return GEO.createPoint(new Coordinate(lng, lat));
    }

    @Transactional
    public MissionResponse createMission(CreateMissionRequest req) {
        User importer = currentUser();
        if (importer.getRole() != User.Role.IMPORTER) {
            throw new UnauthorizedException("Seul un importateur peut créer une mission");
        }

        Mission.MissionBuilder builder = Mission.builder()
                .importer(importer)
                .originPoint(point(req.getOriginLng(), req.getOriginLat()))
                .destinationPoint(point(req.getDestinationLng(), req.getDestinationLat()))
                .originLabel(req.getOriginLabel())
                .destinationLabel(req.getDestinationLabel())
                .cargoDescription(req.getCargoDescription())
                .cargoWeightTons(req.getCargoWeightTons())
                .cargoType(Mission.CargoType.valueOf(req.getCargoType()))
                .specialInstructions(req.getSpecialInstructions())
                .totalPrice(req.getTotalPrice())
                .pickupScheduledAt(req.getPickupScheduledAt())
                .qrDeliveryToken(UUID.randomUUID().toString());

        if (req.getPaymentMethod() != null) {
            builder.paymentMethod(Mission.PaymentMethod.valueOf(req.getPaymentMethod()));
        }
        if (req.getTruckId() != null) {
            Truck truck = truckRepository.findById(req.getTruckId())
                    .orElseThrow(() -> new ResourceNotFoundException("Camion introuvable"));
            builder.truck(truck);
        }

        return MissionResponse.from(missionRepository.save(builder.build()));
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listMissions(String statusFilter) {
        User user = currentUser();
        List<Mission> missions;

        if (user.getRole() == User.Role.IMPORTER) {
            missions = missionRepository.findByImporterId(user.getId());
        } else if (user.getRole() == User.Role.DRIVER) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                missions = missionRepository.findByStatus(Mission.MissionStatus.valueOf(statusFilter));
            } else {
                missions = missionRepository.findAll();
            }
        } else {
            missions = statusFilter != null && !statusFilter.isBlank()
                    ? missionRepository.findByStatus(Mission.MissionStatus.valueOf(statusFilter))
                    : missionRepository.findAll();
        }

        return missions.stream().map(MissionResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MissionResponse getMission(UUID id) {
        Mission m = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
        return MissionResponse.from(m);
    }

    @Transactional
    public MissionResponse assignMission(UUID id) {
        User driver = currentUser();
        if (driver.getRole() != User.Role.DRIVER) {
            throw new UnauthorizedException("Seul un chauffeur peut accepter une mission");
        }
        Mission m = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
        if (m.getStatus() != Mission.MissionStatus.OPEN) {
            throw new IllegalStateException("La mission n'est pas disponible (statut : " + m.getStatus() + ")");
        }
        m.setDriver(driver);
        m.setStatus(Mission.MissionStatus.ASSIGNED);
        return MissionResponse.from(missionRepository.save(m));
    }

    @Transactional
    public MissionResponse startMission(UUID id) {
        User driver = currentUser();
        Mission m = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
        if (!driver.getId().equals(m.getDriver() != null ? m.getDriver().getId() : null)) {
            throw new UnauthorizedException("Vous n'êtes pas le chauffeur assigné à cette mission");
        }
        if (m.getStatus() != Mission.MissionStatus.ASSIGNED) {
            throw new IllegalStateException("La mission doit être en statut ASSIGNED pour démarrer");
        }
        m.setStatus(Mission.MissionStatus.IN_TRANSIT);
        m.setStartedAt(OffsetDateTime.now());
        return MissionResponse.from(missionRepository.save(m));
    }

    @Transactional
    public MissionResponse deliverMission(UUID id, String qrToken) {
        Mission m = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
        if (m.getStatus() != Mission.MissionStatus.IN_TRANSIT) {
            throw new IllegalStateException("La mission n'est pas en transit");
        }
        if (!m.getQrDeliveryToken().equals(qrToken)) {
            throw new UnauthorizedException("Token QR invalide");
        }
        m.setStatus(Mission.MissionStatus.DELIVERED);
        m.setDeliveredAt(OffsetDateTime.now());
        m.setQrScannedAt(OffsetDateTime.now());
        m.setPaymentStatus(Mission.PaymentStatus.RELEASED);
        return MissionResponse.from(missionRepository.save(m));
    }

    @Transactional(readOnly = true)
    public String getQrToken(UUID id) {
        Mission m = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
        User user = currentUser();
        boolean isImporter = user.getId().equals(m.getImporter().getId());
        boolean isDriver = m.getDriver() != null && user.getId().equals(m.getDriver().getId());
        if (!isImporter && !isDriver && user.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Accès non autorisé au token QR");
        }
        return m.getQrDeliveryToken();
    }
}
