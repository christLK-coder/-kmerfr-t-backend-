package cm.kmerfret.backend.config;

import cm.kmerfret.backend.model.*;
import cm.kmerfret.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Insère des données de démonstration couvrant TOUTES les fonctionnalités :
 * missions (tous statuts), paiements, chat, avis, litiges, alertes,
 * hazards, télémétrie, candidatures, politique tarifaire.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository               userRepo;
    private final MissionRepository            missionRepo;
    private final TruckRepository              truckRepo;
    private final PlatformWalletRepository     walletRepo;
    private final ChatMessageRepository        chatRepo;
    private final ReviewRepository             reviewRepo;
    private final HazardRepository             hazardRepo;
    private final TelemetryRepository          telemetryRepo;
    private final NotificationRepository       notifRepo;
    private final DriverApplicationRepository  driverAppRepo;
    private final ApplicationMessageRepository appMsgRepo;
    private final AlertRepository              alertRepo;
    private final CommissionPolicyRepository   policyRepo;
    private final PasswordEncoder              encoder;
    private final JdbcTemplate                 jdbc;

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    // ── Coordonnées géographiques ──────────────────────────────────────────────
    // Douala
    private static final double DLA_LNG = 9.7043, DLA_LAT = 4.0511;
    // Yaoundé
    private static final double YDE_LNG = 11.5021, YDE_LAT = 3.8480;
    // Bafoussam
    private static final double BFS_LNG = 10.4195, BFS_LAT = 5.4737;
    // Kribi
    private static final double KRI_LNG = 9.9121, KRI_LAT = 2.9388;
    // Ngaoundéré
    private static final double NGD_LNG = 13.5840, NGD_LAT = 7.3297;
    // Garoua
    private static final double GRW_LNG = 13.3980, GRW_LAT = 9.3017;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // Supprimer la contrainte CHECK cargo_type si elle bloque les nouvelles valeurs de l'enum
        try {
            jdbc.execute("ALTER TABLE missions DROP CONSTRAINT IF EXISTS missions_cargo_type_check");
        } catch (Exception ex) {
            log.warn("[SEEDER] Contrainte cargo_type : {}", ex.getMessage());
        }

        // Lancer uniquement si la BD est vide ou incomplète
        if (missionRepo.count() >= 8) {
            log.info("[SEEDER] Données déjà présentes ({} missions) — skip", missionRepo.count());
            return;
        }

        log.info("[SEEDER] ══════════════════════════════════════════════════");
        log.info("[SEEDER]  Insertion des données de démonstration complètes");
        log.info("[SEEDER] ══════════════════════════════════════════════════");

        // ── 1. Politique tarifaire ─────────────────────────────────────────────
        if (policyRepo.count() == 0) {
            policyRepo.save(CommissionPolicy.builder().build());
            log.info("[SEEDER] ✅ Politique tarifaire créée (7.5%/10%/+30%/50%)");
        }

        // ── 2. Utilisateurs ───────────────────────────────────────────────────
        User admin    = upsert("admin@kmerfret.cm",          "Admin KmerFret",        "+237690000001", User.Role.ADMIN);
        User imp1     = upsert("importer@kmerfret.cm",       "Jean-Paul Mbarga",      "+237691000002", User.Role.IMPORTER);
        User imp2     = upsert("marie.belle@kmerfret.cm",    "Marie Belle Ngo",       "+237692000003", User.Role.IMPORTER);
        User drv1     = upsert("chauffeur@kmerfret.cm",      "André Nkolo",           "+237699000004", User.Role.DRIVER);
        User drv2     = upsert("paul.voiturier@kmerfret.cm", "Paul Voiturier",        "+237698000005", User.Role.DRIVER);
        User drv3     = upsert("alice.transport@kmerfret.cm","Alice Ndoumbe Transport","+237697000006", User.Role.DRIVER);

        log.info("[SEEDER] ✅ 6 utilisateurs (1 admin, 2 importateurs, 3 chauffeurs)");
        log.info("[SEEDER]    Mot de passe commun : Kmerfret2024!");

        // ── 3. Camions ─────────────────────────────────────────────────────────
        Truck truck1 = truckOrCreate("LT-3892-A", drv1, "Mercedes", "Actros 2545", new BigDecimal("25.0"),
                Truck.TruckType.FLATBED, LocalDate.now().plusMonths(8), true);
        Truck truck2 = truckOrCreate("LT-5547-B", drv2, "Scania", "R500 HL", new BigDecimal("30.0"),
                Truck.TruckType.CONTAINER_20, LocalDate.now().plusMonths(3), true);
        Truck truck3 = truckOrCreate("LT-7731-C", drv3, "Renault", "T520", new BigDecimal("15.0"),
                Truck.TruckType.REFRIGERATED, LocalDate.now().plusDays(20), false);

        log.info("[SEEDER] ✅ 3 camions (FLATBED, CONTAINER_20, REFRIGERATED)");

        // ── 4. MISSIONS ────────────────────────────────────────────────────────

        // M1 — OPEN, paiement PENDING
        Mission m1 = saveMission(Mission.builder()
            .importer(imp1)
            .originPoint(pt(DLA_LNG, DLA_LAT)).originLabel("Port de Douala, Akwa")
            .destinationPoint(pt(BFS_LNG, BFS_LAT)).destinationLabel("Marché Central, Bafoussam")
            .cargoDescription("600 sacs de café arabica — 60 kg/sac")
            .cargoWeightTons(bd("15.0")).cargoType(Mission.CargoType.COFFEE_COCOA)
            .totalPrice(bd("210000")).firstPaymentAmount(bd("105000"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.OPEN).paymentStatus(Mission.PaymentStatus.PENDING)
            .pickupScheduledAt(OffsetDateTime.now().plusDays(2))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        // M2 — OPEN EXPRESS, paiement PENDING
        Mission m2 = saveMission(Mission.builder()
            .importer(imp2)
            .originPoint(pt(YDE_LNG, YDE_LAT)).originLabel("Marché du Mfoundi, Yaoundé")
            .destinationPoint(pt(DLA_LNG, DLA_LAT)).destinationLabel("Zone Franche Bonabéri, Douala")
            .cargoDescription("Régimes de plantains + bananes douces — lot urgent")
            .cargoWeightTons(bd("8.0")).cargoType(Mission.CargoType.PERISHABLE)
            .totalPrice(bd("143000")).firstPaymentAmount(bd("71500"))
            .commissionRate(bd("0.100")).missionType(Mission.MissionType.EXPRESS)
            .expressSurcharge(bd("0.300"))
            .status(Mission.MissionStatus.OPEN).paymentStatus(Mission.PaymentStatus.PENDING)
            .pickupScheduledAt(OffsetDateTime.now().plusHours(6))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        // M3 — ASSIGNED, paiement PENDING (importateur doit payer)
        Mission m3 = saveMission(Mission.builder()
            .importer(imp1).driver(drv1).truck(truck1)
            .originPoint(pt(DLA_LNG, DLA_LAT)).originLabel("Entrepôt Bonabéri, Douala")
            .destinationPoint(pt(NGD_LNG, NGD_LAT)).destinationLabel("Marché Bini, Ngaoundéré")
            .cargoDescription("Maïs en sacs de 50 kg — 400 sacs")
            .cargoWeightTons(bd("20.0")).cargoType(Mission.CargoType.CEREALS)
            .totalPrice(bd("290000")).firstPaymentAmount(bd("145000"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.ASSIGNED).paymentStatus(Mission.PaymentStatus.PENDING)
            .pickupScheduledAt(OffsetDateTime.now().plusHours(24))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        // M4 — ASSIGNED, paiement ESCROWED (chauffeur peut démarrer)
        Mission m4 = saveMission(Mission.builder()
            .importer(imp2).driver(drv2).truck(truck2)
            .originPoint(pt(KRI_LNG, KRI_LAT)).originLabel("Port de Kribi — Terminal Bois")
            .destinationPoint(pt(DLA_LNG, DLA_LAT)).destinationLabel("Entrepôt Logpom, Douala")
            .cargoDescription("Grumes de bois d'okoumé — 18 fûts")
            .cargoWeightTons(bd("18.0")).cargoType(Mission.CargoType.TIMBER)
            .totalPrice(bd("260000")).firstPaymentAmount(bd("130000"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.ASSIGNED).paymentStatus(Mission.PaymentStatus.ESCROWED)
            .firstPaymentAt(OffsetDateTime.now().minusHours(2))
            .pickupScheduledAt(OffsetDateTime.now().plusHours(4))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        wallet(m4, imp2, bd("130000"), PlatformWallet.TransactionType.DEPOSIT, "MTN_MOMO", "REF-M4-001", PlatformWallet.TxStatus.CONFIRMED);

        // M5 — IN_TRANSIT, ESCROWED (avec chat + positions + hazard + alerte)
        Mission m5 = saveMission(Mission.builder()
            .importer(imp1).driver(drv1).truck(truck1)
            .originPoint(pt(DLA_LNG, DLA_LAT)).originLabel("Ferme Nkolo, Douala — PK 10")
            .destinationPoint(pt(BFS_LNG, BFS_LAT)).destinationLabel("Coopérative Agricole, Bafoussam")
            .cargoDescription("Bovins vivants — 12 têtes de bœufs")
            .cargoWeightTons(bd("12.0")).cargoType(Mission.CargoType.LIVESTOCK)
            .totalPrice(bd("185000")).firstPaymentAmount(bd("92500"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.IN_TRANSIT).paymentStatus(Mission.PaymentStatus.ESCROWED)
            .firstPaymentAt(OffsetDateTime.now().minusHours(5))
            .startedAt(OffsetDateTime.now().minusHours(3))
            .transitStartedAt(OffsetDateTime.now().minusHours(3))
            .driverArrivedAt(OffsetDateTime.now().minusHours(4))
            .pickupScheduledAt(OffsetDateTime.now().minusHours(4))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        wallet(m5, imp1, bd("92500"), PlatformWallet.TransactionType.DEPOSIT, "ORANGE_MONEY", "REF-M5-001", PlatformWallet.TxStatus.CONFIRMED);

        // Positions télémétrie M5 (trajet Douala → Bafoussam)
        double[][] m5track = {
            {9.7100, 4.0600, 48.0, 180.0},
            {9.7800, 4.2200, 62.0, 175.0},
            {9.9200, 4.5500, 71.0, 170.0},
            {10.0800, 4.8900, 55.0, 165.0},
            {10.2100, 5.1200, 68.0, 160.0},
            {10.3400, 5.3200, 45.0, 158.0},
        };
        for (double[] p : m5track) {
            telemetryRepo.save(TelemetryPosition.builder()
                .mission(m5).driver(drv1)
                .position(pt(p[0], p[1]))
                .speedKmh(bd(String.valueOf(p[2])))
                .headingDeg(bd(String.valueOf(p[3])))
                .accuracyM(bd("8.5"))
                .recordedAt(OffsetDateTime.now().minusMinutes(180 - (int)(p[2] * 0.5)))
                .wasOffline(false).build());
        }

        // Chat M5 — importer ↔ chauffeur
        chat(m5, imp1, "Bonjour André, est-ce que vous avez chargé tous les animaux ?");
        chat(m5, drv1, "Oui Monsieur, tous les 12 bœufs sont chargés et sécurisés. On est en route.");
        chat(m5, imp1, "Merci ! Faites attention sur la route Bafoussam, il y a des nids-de-poule signalés.");
        chat(m5, drv1, "Compris, je suis au courant. ETA : environ 4h. Je vous tiens informé.");

        // Alerte panne M5
        alertRepo.save(Alert.builder()
            .sender(drv1).mission(m5)
            .alertType(Alert.AlertType.BREAKDOWN)
            .location(pt(10.05, 4.78))
            .message("Crevaison pneu arrière droit — résolu, reprise dans 30 min")
            .smsSent(true).isResolved(true).build());

        // M6 — IN_TRANSIT EXPRESS (avec chat)
        Mission m6 = saveMission(Mission.builder()
            .importer(imp2).driver(drv2).truck(truck2)
            .originPoint(pt(YDE_LNG, YDE_LAT)).originLabel("Zone Industrielle de Biyem-Assi, Yaoundé")
            .destinationPoint(pt(DLA_LNG, DLA_LAT)).destinationLabel("Terminal à Conteneurs, Douala")
            .cargoDescription("Conteneur 20 pieds — pièces automobiles importées")
            .cargoWeightTons(bd("22.0")).cargoType(Mission.CargoType.CONTAINER)
            .totalPrice(bd("338000")).firstPaymentAmount(bd("169000"))
            .commissionRate(bd("0.100")).missionType(Mission.MissionType.EXPRESS)
            .expressSurcharge(bd("0.300"))
            .status(Mission.MissionStatus.IN_TRANSIT).paymentStatus(Mission.PaymentStatus.ESCROWED)
            .firstPaymentAt(OffsetDateTime.now().minusHours(2))
            .startedAt(OffsetDateTime.now().minusHours(1))
            .transitStartedAt(OffsetDateTime.now().minusHours(1))
            .driverArrivedAt(OffsetDateTime.now().minusHours(2))
            .pickupScheduledAt(OffsetDateTime.now().minusHours(3))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        wallet(m6, imp2, bd("169000"), PlatformWallet.TransactionType.DEPOSIT, "STRIPE", "pi_3Abc_test_M6", PlatformWallet.TxStatus.CONFIRMED);

        chat(m6, imp2, "Paul, le conteneur est bien fermé et scellé ?");
        chat(m6, drv2, "Absolument Madame, j'ai vérifié les scellés avant de partir. Tout est bon.");
        chat(m6, imp2, "Super. Livraison impérative avant 18h — c'est pour un départ bateau.");

        // Alerte retard M6
        alertRepo.save(Alert.builder()
            .sender(drv2).mission(m6)
            .alertType(Alert.AlertType.DELAY)
            .location(pt(10.8200, 3.9500))
            .message("Bouchon à l'entrée de Douala sur la nationale — retard estimé 45 min")
            .smsSent(false).isResolved(false).build());

        // M7 — DELIVERED, 1ère tranche reçue — solde à payer par l'importateur
        Mission m7 = saveMission(Mission.builder()
            .importer(imp1).driver(drv3).truck(truck3)
            .originPoint(pt(BFS_LNG, BFS_LAT)).originLabel("Ferme laitière, Bafoussam")
            .destinationPoint(pt(DLA_LNG, DLA_LAT)).destinationLabel("Supermarché Mahima, Douala")
            .cargoDescription("Lait frais pasteurisé en bidons de 20L — 50 bidons")
            .cargoWeightTons(bd("10.0")).cargoType(Mission.CargoType.AGRICULTURAL)
            .totalPrice(bd("148000")).firstPaymentAmount(bd("74000"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.DELIVERED).paymentStatus(Mission.PaymentStatus.ESCROWED)
            .firstPaymentAt(OffsetDateTime.now().minusHours(10))
            .startedAt(OffsetDateTime.now().minusHours(9))
            .transitStartedAt(OffsetDateTime.now().minusHours(9))
            .driverArrivedAt(OffsetDateTime.now().minusHours(10))
            .deliveredAt(OffsetDateTime.now().minusHours(2))
            .qrScannedAt(OffsetDateTime.now().minusHours(2))
            .pickupScheduledAt(OffsetDateTime.now().minusHours(12))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        wallet(m7, imp1, bd("74000"), PlatformWallet.TransactionType.DEPOSIT, "MTN_MOMO", "REF-M7-001", PlatformWallet.TxStatus.CONFIRMED);

        // M8 — DELIVERED + 2ème paiement + RELEASED (mission complète)
        Mission m8 = saveMission(Mission.builder()
            .importer(imp2).driver(drv1).truck(truck1)
            .originPoint(pt(DLA_LNG, DLA_LAT)).originLabel("Dépôt SCDP, Douala-Bassa")
            .destinationPoint(pt(YDE_LNG, YDE_LAT)).destinationLabel("Dépôt Central, Yaoundé")
            .cargoDescription("Marchandises générales — électroménager emballé")
            .cargoWeightTons(bd("6.0")).cargoType(Mission.CargoType.GENERAL)
            .totalPrice(bd("110000")).firstPaymentAmount(bd("55000"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.DELIVERED).paymentStatus(Mission.PaymentStatus.RELEASED)
            .firstPaymentAt(OffsetDateTime.now().minusDays(2))
            .secondPaymentAt(OffsetDateTime.now().minusHours(20))
            .startedAt(OffsetDateTime.now().minusDays(2).plusHours(2))
            .transitStartedAt(OffsetDateTime.now().minusDays(2).plusHours(2))
            .driverArrivedAt(OffsetDateTime.now().minusDays(2).plusHours(1))
            .deliveredAt(OffsetDateTime.now().minusDays(1))
            .qrScannedAt(OffsetDateTime.now().minusDays(1))
            .pickupScheduledAt(OffsetDateTime.now().minusDays(2))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        wallet(m8, imp2, bd("55000"),  PlatformWallet.TransactionType.DEPOSIT,    "MTN_MOMO", "REF-M8-DEP", PlatformWallet.TxStatus.CONFIRMED);
        wallet(m8, imp2, bd("55000"),  PlatformWallet.TransactionType.DEPOSIT,    "MTN_MOMO", "REF-M8-SOL", PlatformWallet.TxStatus.CONFIRMED);
        wallet(m8, null, bd("8250"),   PlatformWallet.TransactionType.COMMISSION,  null,       null,          PlatformWallet.TxStatus.CONFIRMED);
        wallet(m8, null, bd("101750"), PlatformWallet.TransactionType.RELEASE,     null,       null,          PlatformWallet.TxStatus.CONFIRMED);

        // Avis mission 8
        reviewRepo.save(Review.builder()
            .mission(m8).reviewer(imp2).reviewed(drv1)
            .rating((short)5)
            .comment("Chauffeur très professionnel, livraison dans les temps. Marchandises intactes. Je recommande vivement !")
            .build());
        reviewRepo.save(Review.builder()
            .mission(m8).reviewer(drv1).reviewed(imp2)
            .rating((short)4)
            .comment("Cliente sérieuse, marchandises bien emballées. Quelques minutes de retard au chargement mais rien de grave.")
            .build());

        // M9 — CANCELLED
        Mission m9 = saveMission(Mission.builder()
            .importer(imp1)
            .originPoint(pt(NGD_LNG, NGD_LAT)).originLabel("Entrepôt CMDT, Ngaoundéré")
            .destinationPoint(pt(DLA_LNG, DLA_LAT)).destinationLabel("Port de Douala — Quai Export")
            .cargoDescription("Engrais NPK — 100 sacs de 50 kg")
            .cargoWeightTons(bd("5.0")).cargoType(Mission.CargoType.FERTILIZER)
            .totalPrice(bd("85000")).firstPaymentAmount(bd("42500"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.CANCELLED).paymentStatus(Mission.PaymentStatus.PENDING)
            .pickupScheduledAt(OffsetDateTime.now().minusDays(3))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        // M10 — DISPUTED (litige en cours)
        Mission m10 = saveMission(Mission.builder()
            .importer(imp2).driver(drv3).truck(truck3)
            .originPoint(pt(DLA_LNG, DLA_LAT)).originLabel("Dépôt pétrolier, Douala-Bonabéri")
            .destinationPoint(pt(KRI_LNG, KRI_LAT)).destinationLabel("Station-Service, Kribi-Centre")
            .cargoDescription("Huile de palme brute en fûts de 200L — 40 fûts")
            .cargoWeightTons(bd("8.0")).cargoType(Mission.CargoType.LIQUID)
            .totalPrice(bd("128000")).firstPaymentAmount(bd("64000"))
            .commissionRate(bd("0.075")).missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.DISPUTED).paymentStatus(Mission.PaymentStatus.DISPUTED)
            .firstPaymentAt(OffsetDateTime.now().minusDays(1))
            .startedAt(OffsetDateTime.now().minusDays(1).plusHours(2))
            .transitStartedAt(OffsetDateTime.now().minusDays(1).plusHours(2))
            .driverArrivedAt(OffsetDateTime.now().minusDays(1).plusHours(1))
            .pickupScheduledAt(OffsetDateTime.now().minusDays(1))
            .qrDeliveryToken(UUID.randomUUID().toString()).build());

        wallet(m10, imp2, bd("64000"), PlatformWallet.TransactionType.DEPOSIT, "ORANGE_MONEY", "REF-M10-001", PlatformWallet.TxStatus.CONFIRMED);

        log.info("[SEEDER] ✅ 10 missions créées (OPEN×2, ASSIGNED×2, IN_TRANSIT×2, DELIVERED×2, CANCELLED, DISPUTED)");

        // ── 5. Nids-de-poule sur les routes ──────────────────────────────────
        hazard(drv1, m5, 9.8200, 4.3100, "2.85", RoadHazard.Severity.HIGH,    RoadHazard.HazardType.POTHOLE,  "72.0",  3, true,  false);
        hazard(drv1, m5, 10.050, 4.7800, "4.12", RoadHazard.Severity.CRITICAL, RoadHazard.HazardType.POTHOLE,  "55.0",  5, true,  false);
        hazard(drv2, m6, 10.820, 3.9800, "2.10", RoadHazard.Severity.MEDIUM,  RoadHazard.HazardType.CRACK,    "88.0",  2, false, false);
        hazard(drv1, m8, 9.9500, 4.1200, "1.75", RoadHazard.Severity.LOW,     RoadHazard.HazardType.BUMP,     "40.0",  1, false, true);
        hazard(drv2, m4, 9.9800, 3.2100, "3.40", RoadHazard.Severity.HIGH,    RoadHazard.HazardType.POTHOLE,  "60.0",  4, true,  false);
        hazard(drv3, m7, 10.310, 5.1000, "1.50", RoadHazard.Severity.MEDIUM,  RoadHazard.HazardType.BROKEN_ROAD,"80.0",2, false, false);

        log.info("[SEEDER] ✅ 6 nids-de-poule/dégradations insérés (2 validés, 1 réparé)");

        // ── 6. Notifications ──────────────────────────────────────────────────
        notif(admin, "KmerFret — Nouveau litige",     "Litige signalé sur la mission Douala → Kribi. Intervention requise.", "DISPUTE_OPENED",      m10.getId().toString());
        notif(admin, "KmerFret — Candidature reçue",  "Nouveau dossier chauffeur en attente de révision.",                   "NEW_APPLICATION",     null);
        notif(imp1,  "Mission livrée",                "Votre marchandise est arrivée à Yaoundé. Paiement du solde requis.",  "DELIVERED",           m8.getId().toString());
        notif(imp1,  "Paiement requis",               "Versez 145 000 FCFA pour confirmer la mission Douala → Ngaoundéré.", "PAYMENT_REMINDER",    m3.getId().toString());
        notif(imp2,  "Chauffeur en route",            "Paul Voiturier a démarré la mission vers Douala — ETA : 4h.",        "TRANSIT_STARTED",     m6.getId().toString());
        notif(imp2,  "Litige ouvert",                 "Votre litige sur la mission Douala → Kribi est en cours d'examen.",  "DISPUTE_OPENED",      m10.getId().toString());
        notif(drv1,  "Paiement reçu",                 "Jean-Paul Mbarga a versé la 1ère tranche — vous pouvez démarrer.",   "PAYMENT_CONFIRMED",   m5.getId().toString());
        notif(drv1,  "Fonds virés",                   "101 750 FCFA ont été transférés sur votre compte.",                  "PAYMENT_RELEASED",    m8.getId().toString());
        notif(drv2,  "Nouvelle mission EXPRESS",       "Mission conteneur urgente Yaoundé → Douala disponible !",            "NEW_MISSION",         m6.getId().toString());
        notif(drv3,  "Litige — fonds bloqués",        "Un litige a été signalé. Les fonds restent bloqués jusqu'à résolution.", "DISPUTE_OPENED",  m10.getId().toString());

        log.info("[SEEDER] ✅ 10 notifications créées");

        // ── 7. Candidatures chauffeur ─────────────────────────────────────────

        // Candidature PENDING
        DriverApplication appPending = driverAppRepo.save(DriverApplication.builder()
            .fullName("Éric Kamgue Nguemo").email("eric.kamgue@gmail.com")
            .phone("+237677000010").city("Douala")
            .motivation("Chauffeur professionnel avec 8 ans d'expérience. Permis C + D. Disponible immédiatement.")
            .status(DriverApplication.Status.PENDING).build());

        appMsgRepo.save(ApplicationMessage.builder().application(appPending)
            .senderRole(ApplicationMessage.SenderRole.APPLICANT)
            .content("Bonjour, je souhaite rejoindre KmerFret. J'ai tous mes documents en règle.")
            .build());

        // Candidature UNDER_REVIEW
        DriverApplication appReview = driverAppRepo.save(DriverApplication.builder()
            .fullName("Bertrand Fouda").email("bertrand.fouda@yahoo.fr")
            .phone("+237688000011").city("Yaoundé")
            .motivation("5 ans de transport agricole. Je connais les axes Yaoundé-Nord et Centre.")
            .status(DriverApplication.Status.UNDER_REVIEW)
            .adminNotes("Documents CNI et permis reçus. En attente de vérification assurance.")
            .build());

        appMsgRepo.save(ApplicationMessage.builder().application(appReview)
            .senderRole(ApplicationMessage.SenderRole.APPLICANT)
            .content("Bonjour, voici ma CNI et mon permis C en pièces jointes.")
            .build());
        appMsgRepo.save(ApplicationMessage.builder().application(appReview)
            .senderRole(ApplicationMessage.SenderRole.ADMIN)
            .content("Merci Bertrand. Votre CNI et permis ont été reçus. Il nous manque votre attestation d'assurance véhicule. Merci de l'envoyer.")
            .build());
        appMsgRepo.save(ApplicationMessage.builder().application(appReview)
            .senderRole(ApplicationMessage.SenderRole.APPLICANT)
            .content("Je vous envoie l'attestation dès demain matin. Merci pour le retour rapide.")
            .build());

        // Candidature APPROVED (liée au chauffeur Alice)
        DriverApplication appApproved = driverAppRepo.save(DriverApplication.builder()
            .fullName("Alice Ndoumbe Transport").email("alice.application@gmail.com")
            .phone("+237697000006").city("Bafoussam")
            .motivation("Transporteuse certifiée, spécialisée en produits frais et réfrigérés depuis 6 ans.")
            .status(DriverApplication.Status.APPROVED)
            .adminNotes("Dossier complet. Expérience vérifiée. Approuvée pour le transport réfrigéré.")
            .createdUser(drv3)
            .reviewedBy(admin)
            .reviewedAt(OffsetDateTime.now().minusDays(5))
            .build());

        appMsgRepo.save(ApplicationMessage.builder().application(appApproved)
            .senderRole(ApplicationMessage.SenderRole.ADMIN)
            .content("Félicitations Alice ! Votre dossier a été validé. Bienvenue dans la flotte KmerFret. Vous pouvez dès maintenant accéder à votre espace chauffeur.")
            .build());

        // Candidature REJECTED
        DriverApplication appRejected = driverAppRepo.save(DriverApplication.builder()
            .fullName("Hippolyte Zanga").email("hippolyte.zanga@gmail.com")
            .phone("+237655000012").city("Maroua")
            .motivation("Je veux travailler comme transporteur.")
            .status(DriverApplication.Status.REJECTED)
            .adminNotes("Permis expiré. Dossier incomplet — aucune expérience vérifiable.")
            .reviewedBy(admin)
            .reviewedAt(OffsetDateTime.now().minusDays(3))
            .build());

        appMsgRepo.save(ApplicationMessage.builder().application(appRejected)
            .senderRole(ApplicationMessage.SenderRole.ADMIN)
            .content("Bonjour Hippolyte. Nous avons étudié votre dossier. Malheureusement, votre permis de conduire est expiré et vous n'avez pas fourni de preuves d'expérience. Nous ne pouvons pas traiter votre candidature en l'état. Vous pouvez représenter un dossier complet dans 3 mois.")
            .build());
        appMsgRepo.save(ApplicationMessage.builder().application(appRejected)
            .senderRole(ApplicationMessage.SenderRole.APPLICANT)
            .content("Compris, je vais renouveler mon permis et retenter ma chance. Merci.")
            .build());

        log.info("[SEEDER] ✅ 4 candidatures chauffeur (PENDING, UNDER_REVIEW, APPROVED, REJECTED)");

        // ── 8. Résumé final ───────────────────────────────────────────────────
        log.info("[SEEDER] ══════════════════════════════════════════════════════");
        log.info("[SEEDER]  DONNÉES DE DÉMO INSÉRÉES AVEC SUCCÈS !");
        log.info("[SEEDER] ──────────────────────────────────────────────────────");
        log.info("[SEEDER]  COMPTES (mot de passe : Kmerfret2024!)");
        log.info("[SEEDER]    admin@kmerfret.cm          → ADMIN");
        log.info("[SEEDER]    importer@kmerfret.cm        → IMPORTATEUR (Jean-Paul)");
        log.info("[SEEDER]    marie.belle@kmerfret.cm     → IMPORTATEUR (Marie)");
        log.info("[SEEDER]    chauffeur@kmerfret.cm       → CHAUFFEUR (André)");
        log.info("[SEEDER]    paul.voiturier@kmerfret.cm  → CHAUFFEUR (Paul)");
        log.info("[SEEDER]    alice.transport@kmerfret.cm → CHAUFFEUR (Alice)");
        log.info("[SEEDER] ──────────────────────────────────────────────────────");
        log.info("[SEEDER]  MISSIONS : 10 (OPEN×2, ASSIGNED×2, IN_TRANSIT×2,");
        log.info("[SEEDER]              DELIVERED×2 (dont 1 RELEASED), CANCELLED, DISPUTED)");
        log.info("[SEEDER]  CAMIONS  : 3  (FLATBED, CONTAINER_20, REFRIGERATED)");
        log.info("[SEEDER]  HAZARDS  : 6  (sur routes Douala-Bafoussam, Yaoundé-Douala, Kribi)");
        log.info("[SEEDER]  CHAT     : 7 messages (missions IN_TRANSIT)");
        log.info("[SEEDER]  AVIS     : 2 (mission RELEASED)");
        log.info("[SEEDER]  ALERTES  : 2 (BREAKDOWN résolu, DELAY en cours)");
        log.info("[SEEDER]  NOTIFS   : 10");
        log.info("[SEEDER]  CANDIDATURES : 4 (PENDING, UNDER_REVIEW, APPROVED, REJECTED)");
        log.info("[SEEDER] ══════════════════════════════════════════════════════");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Point pt(double lng, double lat) {
        return GEO.createPoint(new Coordinate(lng, lat));
    }

    private BigDecimal bd(String v) { return new BigDecimal(v); }

    private Mission saveMission(Mission m) { return missionRepo.save(m); }

    private User upsert(String email, String name, String phone, User.Role role) {
        return userRepo.findByEmail(email).orElseGet(() -> userRepo.save(
            User.builder()
                .fullName(name).email(email).phone(phone)
                .passwordHash(encoder.encode("Kmerfret2024!"))
                .role(role).emailVerified(true).isActive(true)
                .accountStatus(User.AccountStatus.ACTIVE).build()
        ));
    }

    private Truck truckOrCreate(String plate, User driver, String brand, String model,
                                BigDecimal cap, Truck.TruckType type,
                                LocalDate expiry, boolean verified) {
        return truckRepo.findAll().stream()
            .filter(t -> t.getPlateNumber().equals(plate))
            .findFirst()
            .orElseGet(() -> truckRepo.save(Truck.builder()
                .driver(driver).plateNumber(plate).brand(brand).model(model)
                .capacityTons(cap).truckType(type)
                .insuranceExpiry(expiry).docsVerified(verified).build()));
    }

    private void wallet(Mission m, User payer, BigDecimal amount,
                        PlatformWallet.TransactionType type, String method,
                        String ref, PlatformWallet.TxStatus status) {
        walletRepo.save(PlatformWallet.builder()
            .mission(m).payer(payer).amount(amount)
            .transactionType(type).paymentMethod(method)
            .externalRef(ref).status(status).build());
    }

    private void chat(Mission m, User sender, String content) {
        chatRepo.save(ChatMessage.builder()
            .mission(m).sender(sender).content(content)
            .msgType(ChatMessage.MsgType.TEXT).isRead(false).build());
    }

    private void notif(User user, String title, String body, String type, String refId) {
        notifRepo.save(Notification.builder()
            .user(user).title(title).body(body)
            .notifType(type).referenceId(refId).isRead(false).build());
    }

    private void hazard(User reporter, Mission mission,
                        double lng, double lat,
                        String magnitude, RoadHazard.Severity severity,
                        RoadHazard.HazardType hazardType, String speed,
                        int confirmations, boolean validated, boolean repaired) {
        hazardRepo.save(RoadHazard.builder()
            .reportedBy(reporter).mission(mission)
            .location(pt(lng, lat))
            .shockMagnitude(bd(magnitude)).shockAxis("Z")
            .speedKmh(bd(speed))
            .severity(severity).hazardType(hazardType)
            .confirmationCount(confirmations)
            .isValidated(validated).isRepaired(repaired)
            .recordedAt(OffsetDateTime.now().minusHours(2))
            .syncedAt(OffsetDateTime.now().minusHours(1))
            .wasOffline(false).build());
    }
}
