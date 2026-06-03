package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.CreateMissionRequest;
import cm.kmerfret.backend.dto.response.MissionResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.exception.UnauthorizedException;
import cm.kmerfret.backend.model.*;
import cm.kmerfret.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository   missionRepo;
    private final UserRepository      userRepo;
    private final TruckRepository     truckRepo;
    private final CommissionPolicyRepository policyRepo;
    private final PushNotificationService pushService;
    private final NotificationRepository notifRepo;

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    // Charge l'utilisateur courant par EMAIL (sujet JWT = email en V3)
    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private Point point(double lng, double lat) {
        return GEO.createPoint(new Coordinate(lng, lat));
    }

    private CommissionPolicy policy() {
        return policyRepo.findFirstByOrderByUpdatedAtDesc()
                .orElseGet(() -> CommissionPolicy.builder().build());
    }

    // ─── Créer une mission ────────────────────────────────────────────────────

    @Transactional
    public MissionResponse createMission(CreateMissionRequest req) {
        User importer = currentUser();
        if (importer.getRole() != User.Role.IMPORTER) {
            throw new UnauthorizedException("Seul un importateur peut créer une mission");
        }

        CommissionPolicy pol = policy();
        Mission.MissionType type = "EXPRESS".equalsIgnoreCase(req.getMissionType())
                ? Mission.MissionType.EXPRESS : Mission.MissionType.NORMAL;

        BigDecimal surcharge = type == Mission.MissionType.EXPRESS
                ? (pol.getExpressSurcharge() != null ? pol.getExpressSurcharge() : new BigDecimal("0.30"))
                : BigDecimal.ZERO;
        BigDecimal basePrice = req.getTotalPrice();
        BigDecimal finalPrice = type == Mission.MissionType.EXPRESS
                ? basePrice.multiply(BigDecimal.ONE.add(surcharge)).setScale(2, RoundingMode.HALF_UP)
                : basePrice;

        BigDecimal commRate = type == Mission.MissionType.EXPRESS
                ? (pol.getExpressRate() != null ? pol.getExpressRate() : new BigDecimal("0.10"))
                : (pol.getNormalRate() != null ? pol.getNormalRate() : new BigDecimal("0.075"));
        BigDecimal firstDepPct = pol.getFirstDepositPct() != null ? pol.getFirstDepositPct() : new BigDecimal("0.50");
        BigDecimal depositAmt = finalPrice.multiply(firstDepPct).setScale(2, RoundingMode.HALF_UP);

        // Valider le cargoType
        Mission.CargoType cargoType;
        try {
            cargoType = Mission.CargoType.valueOf(req.getCargoType());
        } catch (IllegalArgumentException e) {
            cargoType = Mission.CargoType.GENERAL;
            log.warn("CargoType inconnu '{}', fallback GENERAL", req.getCargoType());
        }

        Mission mission = Mission.builder()
                .importer(importer)
                .originPoint(point(req.getOriginLng(), req.getOriginLat()))
                .destinationPoint(point(req.getDestinationLng(), req.getDestinationLat()))
                .originLabel(req.getOriginLabel())
                .destinationLabel(req.getDestinationLabel())
                .cargoDescription(req.getCargoDescription())
                .cargoWeightTons(req.getCargoWeightTons())
                .cargoType(cargoType)
                .specialInstructions(req.getSpecialInstructions())
                .totalPrice(finalPrice)
                .commissionRate(commRate)
                .missionType(type)
                .expressSurcharge(surcharge)
                .firstPaymentAmount(depositAmt)
                .pickupScheduledAt(parsePickupDate(req.getPickupScheduledAt()))
                .qrDeliveryToken(UUID.randomUUID().toString())
                .build();

        if (req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank()) {
            try {
                mission.setPaymentMethod(Mission.PaymentMethod.valueOf(req.getPaymentMethod()));
            } catch (IllegalArgumentException e) {
                log.warn("PaymentMethod inconnu '{}', ignoré", req.getPaymentMethod());
            }
        }
        if (req.getTruckId() != null)
            mission.setTruck(truckRepo.findById(req.getTruckId())
                    .orElseThrow(() -> new ResourceNotFoundException("Camion introuvable")));

        log.info("Création mission: {} → {}, prix={}, type={}", req.getOriginLabel(), req.getDestinationLabel(), finalPrice, type);
        Mission saved = missionRepo.save(mission);
        log.info("Mission créée avec ID: {}", saved.getId());

        // Notifications (ne doivent jamais faire planter la création)
        try {
            notify(importer, "KmerFret — Mission créée",
                    "Votre mission " + saved.getOriginLabel() + " → " + saved.getDestinationLabel() + " a été publiée.",
                    "MISSION_CREATED", saved.getId().toString());
            notifyNearbyDrivers(saved);
        } catch (Exception e) {
            log.warn("Erreur notifications post-création: {}", e.getMessage());
        }

        return MissionResponse.from(saved);
    }

    // ─── Lister les missions ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MissionResponse> listActiveMissions() {
        User user = currentUser();
        List<Mission.MissionStatus> active = List.of(
                Mission.MissionStatus.OPEN, Mission.MissionStatus.ASSIGNED, Mission.MissionStatus.IN_TRANSIT);

        if (user.getRole() == User.Role.IMPORTER) {
            return missionRepo.findByImporterIdAndStatusIn(user.getId(), active)
                    .stream().map(MissionResponse::from).collect(Collectors.toList());
        } else if (user.getRole() == User.Role.DRIVER) {
            return missionRepo.findByDriverIdAndStatusIn(user.getId(), active)
                    .stream().map(MissionResponse::from).collect(Collectors.toList());
        }
        return missionRepo.findByStatus(Mission.MissionStatus.OPEN)
                .stream().map(MissionResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listHistoryMissions() {
        User user = currentUser();
        List<Mission.MissionStatus> done = List.of(
                Mission.MissionStatus.DELIVERED, Mission.MissionStatus.CANCELLED, Mission.MissionStatus.DISPUTED);

        if (user.getRole() == User.Role.IMPORTER) {
            return missionRepo.findByImporterIdAndStatusInHistory(user.getId(), done)
                    .stream().map(MissionResponse::from).collect(Collectors.toList());
        }
        return missionRepo.findByDriverIdAndStatusIn(user.getId(), done)
                .stream().map(MissionResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listAvailableMissions() {
        return missionRepo.findByStatus(Mission.MissionStatus.OPEN)
                .stream().map(MissionResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MissionResponse getMission(UUID id) {
        return MissionResponse.from(findById(id));
    }

    // ─── Actions chauffeur ────────────────────────────────────────────────────

    @Transactional
    public MissionResponse assignMission(UUID id) {
        User driver = currentUser();
        if (driver.getRole() != User.Role.DRIVER) throw new UnauthorizedException("Réservé aux chauffeurs");
        Mission m = findById(id);
        if (m.getStatus() != Mission.MissionStatus.OPEN)
            throw new IllegalStateException("Mission non disponible (statut : " + m.getStatus() + ")");

        m.setDriver(driver);
        m.setStatus(Mission.MissionStatus.ASSIGNED);
        Mission saved = missionRepo.save(m);

        // Notifier l'importateur
        notify(m.getImporter(), "KmerFret — Mission acceptée",
                driver.getFullName() + " a accepté votre mission " + m.getOriginLabel() + " → " + m.getDestinationLabel(),
                "DRIVER_ASSIGNED", id.toString());

        return MissionResponse.from(saved);
    }

    @Transactional
    public MissionResponse markDriverArrived(UUID id) {
        User driver = currentUser();
        Mission m = findById(id);
        checkIsAssignedDriver(driver, m);
        m.setDriverArrivedAt(OffsetDateTime.now());
        Mission saved = missionRepo.save(m);

        notify(m.getImporter(), "KmerFret — Chauffeur arrivé",
                m.getDriver().getFullName() + " est arrivé au point de collecte",
                "DRIVER_ARRIVED", id.toString());
        return MissionResponse.from(saved);
    }

    @Transactional
    public MissionResponse startTransit(UUID id) {
        User driver = currentUser();
        Mission m = findById(id);
        checkIsAssignedDriver(driver, m);

        if (m.getStatus() != Mission.MissionStatus.ASSIGNED)
            throw new IllegalStateException("La mission doit être ASSIGNED pour démarrer");
        if (m.getPaymentStatus() != Mission.PaymentStatus.ESCROWED)
            throw new IllegalStateException("Le premier versement doit être confirmé avant le départ");

        m.setStatus(Mission.MissionStatus.IN_TRANSIT);
        m.setStartedAt(OffsetDateTime.now());
        m.setTransitStartedAt(OffsetDateTime.now());
        Mission saved = missionRepo.save(m);

        notify(m.getImporter(), "KmerFret — Transit démarré",
                "Votre marchandise est en route vers " + m.getDestinationLabel(),
                "TRANSIT_STARTED", id.toString());
        return MissionResponse.from(saved);
    }

    // ─── Workflow litige ──────────────────────────────────────────────────────

    @Transactional
    public MissionResponse openDispute(UUID id, String reason) {
        User user = currentUser();
        Mission m = findById(id);
        if (!user.getId().equals(m.getImporter().getId()) &&
            (m.getDriver() == null || !user.getId().equals(m.getDriver().getId())) &&
            user.getRole() != User.Role.ADMIN)
            throw new UnauthorizedException("Vous n'êtes pas partie prenante de cette mission.");

        m.setStatus(Mission.MissionStatus.DISPUTED);
        m.setPaymentStatus(Mission.PaymentStatus.DISPUTED);
        Mission saved = missionRepo.save(m);

        notify(m.getImporter(), "KmerFret — Litige ouvert",
                "Un litige a été ouvert sur votre mission. Les fonds sont bloqués.", "DISPUTE_OPENED", id.toString());
        if (m.getDriver() != null)
            notify(m.getDriver(), "KmerFret — Litige ouvert",
                    "Un litige a été signalé sur votre mission. Les fonds sont bloqués.", "DISPUTE_OPENED", id.toString());
        log.info("[DISPUTE] Ouvert par {} sur mission {} motif={}", user.getEmail(), id, reason);
        return MissionResponse.from(saved);
    }

    @Transactional
    public MissionResponse deliverMission(UUID id, String qrToken) {
        User scanner = currentUser();
        Mission m = findById(id);
        if (m.getStatus() != Mission.MissionStatus.IN_TRANSIT)
            throw new IllegalStateException("La mission n'est pas en transit.");
        if (!m.getQrDeliveryToken().equals(qrToken))
            throw new UnauthorizedException("Token QR invalide.");
        // Seul le chauffeur assigné peut scanner
        if (m.getDriver() == null || !scanner.getId().equals(m.getDriver().getId()))
            throw new UnauthorizedException("Seul le chauffeur assigné peut confirmer la livraison.");

        m.setStatus(Mission.MissionStatus.DELIVERED);
        m.setDeliveredAt(OffsetDateTime.now());
        m.setQrScannedAt(OffsetDateTime.now());
        Mission saved = missionRepo.save(m);

        notify(m.getImporter(), "KmerFret — Livraison confirmée",
                "Votre marchandise est arrivée à " + m.getDestinationLabel() + ". Finalisez le paiement du solde.",
                "DELIVERED", id.toString());
        if (m.getDriver() != null) {
            notify(m.getDriver(), "KmerFret — Mission livrée",
                    "Livraison confirmée à " + m.getDestinationLabel() + ". En attente du paiement du solde.",
                    "DELIVERED", id.toString());
        }
        return MissionResponse.from(saved);
    }

    // ─── Annulation ─────────────────────────────────────────────────────────────

    @Transactional
    public MissionResponse cancelMission(UUID id) {
        User user = currentUser();
        Mission m = findById(id);

        if (user.getRole() != User.Role.IMPORTER && user.getRole() != User.Role.ADMIN)
            throw new UnauthorizedException("Seul l'importateur ou un admin peut annuler une mission");
        if (user.getRole() == User.Role.IMPORTER && !user.getId().equals(m.getImporter().getId()))
            throw new UnauthorizedException("Vous n'êtes pas le propriétaire de cette mission");
        if (m.getStatus() != Mission.MissionStatus.OPEN && m.getStatus() != Mission.MissionStatus.ASSIGNED)
            throw new IllegalStateException("Impossible d'annuler une mission en statut " + m.getStatus());

        m.setStatus(Mission.MissionStatus.CANCELLED);
        Mission saved = missionRepo.save(m);

        if (m.getDriver() != null) {
            notify(m.getDriver(), "KmerFret — Mission annulée",
                    "La mission " + m.getOriginLabel() + " → " + m.getDestinationLabel() + " a été annulée par le client.",
                    "MISSION_CANCELLED", id.toString());
        }
        notify(m.getImporter(), "KmerFret — Annulation confirmée",
                "Votre mission vers " + m.getDestinationLabel() + " a été annulée.",
                "MISSION_CANCELLED", id.toString());

        return MissionResponse.from(saved);
    }

    // ─── Notification recalcul itinéraire ─────────────────────────────────────

    @Transactional
    public void notifyRouteChanged(UUID id) {
        Mission m = findById(id);
        if (m.getStatus() != Mission.MissionStatus.IN_TRANSIT) return;
        notify(m.getImporter(), "KmerFret — Itinéraire modifié",
                "Le transporteur a recalculé l'itinéraire vers " + m.getDestinationLabel() + ".",
                "ROUTE_CHANGED", id.toString());
    }

    // ─── Token QR ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String getQrToken(UUID id) {
        User user = currentUser();
        Mission m = findById(id);
        if (!user.getId().equals(m.getImporter().getId())
                && (m.getDriver() == null || !user.getId().equals(m.getDriver().getId()))
                && user.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Accès non autorisé au token QR");
        }
        return m.getQrDeliveryToken();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Mission findById(UUID id) {
        return missionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
    }

    private OffsetDateTime parsePickupDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {}
        // DD/MM/YYYY
        try {
            LocalDate ld = LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return ld.atTime(8, 0).atZone(ZoneId.of("Africa/Douala")).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {}
        // YYYY-MM-DD
        try {
            LocalDate ld = LocalDate.parse(raw);
            return ld.atTime(8, 0).atZone(ZoneId.of("Africa/Douala")).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {}
        log.warn("Format de date non reconnu: '{}', ignoré", raw);
        return null;
    }

    private void checkIsAssignedDriver(User driver, Mission m) {
        if (driver.getRole() != User.Role.DRIVER)
            throw new UnauthorizedException("Réservé aux chauffeurs");
        if (m.getDriver() == null || !driver.getId().equals(m.getDriver().getId()))
            throw new UnauthorizedException("Vous n'êtes pas le chauffeur assigné à cette mission");
    }

    private void notifyNearbyDrivers(Mission mission) {
        try {
            userRepo.findActiveDriversWithPushToken().forEach(driver -> {
                try {
                    if (driver.getPushToken() != null) {
                        pushService.sendToToken(driver.getPushToken(),
                                "Nouvelle mission " + (mission.getMissionType() == Mission.MissionType.EXPRESS ? "EXPRESS" : ""),
                                mission.getOriginLabel() + " → " + mission.getDestinationLabel(),
                                Map.of("type", "NEW_MISSION", "missionId", mission.getId().toString())
                        );
                    }
                    saveNotification(driver, "Nouvelle mission disponible",
                            mission.getOriginLabel() + " → " + mission.getDestinationLabel(),
                            "NEW_MISSION", mission.getId().toString());
                } catch (Exception e) {
                    log.warn("Erreur notification chauffeur {} : {}", driver.getId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Erreur lors de la notification des chauffeurs : {}", e.getMessage());
        }
    }

    private void notify(User user, String title, String body, String type, String refId) {
        try {
            if (user.getPushToken() != null) {
                pushService.sendToToken(user.getPushToken(), title, body,
                        Map.of("type", type, "referenceId", refId));
            }
            saveNotification(user, title, body, type, refId);
        } catch (Exception e) {
            log.warn("Erreur notification {} : {}", user.getId(), e.getMessage());
        }
    }

    private void saveNotification(User user, String title, String body, String type, String refId) {
        Notification n = Notification.builder()
                .user(user).title(title).body(body)
                .notifType(type).referenceId(refId)
                .build();
        notifRepo.save(n);
    }
}
