package cm.kmerfret.backend.config;

import cm.kmerfret.backend.model.*;
import cm.kmerfret.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Insère les données de test pour les 3 comptes réels de démonstration.
 * Mot de passe commun : Test1234!
 * Exécuté APRÈS DataSeeder (Order 20).
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RealAccountsSeeder {

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
    private final PasswordEncoder              encoder;
    private final JdbcTemplate                 jdbc;

    // Hash BCrypt de "Test1234!" (bcrypt cost 10)
    private static final String PWD_HASH =
        "$2b$10$5fTvMYykuKqvPCiER0/xJulAmZH4hIHoLy.HTj7Cbta42zZY8Bkhi";

    private static final GeometryFactory GEO =
        new GeometryFactory(new PrecisionModel(), 4326);

    // ── Coordonnées (Cameroun) ─────────────────────────────────────────────────
    private static final double DLA_LNG = 9.7043,  DLA_LAT = 4.0511;   // Douala
    private static final double YDE_LNG = 11.5021, YDE_LAT = 3.8480;   // Yaoundé
    private static final double BFS_LNG = 10.4195, BFS_LAT = 5.4737;   // Bafoussam
    private static final double KRI_LNG = 9.9121,  KRI_LAT = 2.9388;   // Kribi
    private static final double NGD_LNG = 13.5840, NGD_LAT = 7.3297;   // Ngaoundéré

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // ── 0. Corriger la contrainte CHECK cargo_type (schéma legacy) ───────────
        // La contrainte a été créée avec les 6 valeurs initiales de l'enum.
        // Hibernate ddl-auto:update ne la met pas à jour. On la supprime proprement.
        try {
            jdbc.execute("ALTER TABLE missions DROP CONSTRAINT IF EXISTS missions_cargo_type_check");
            log.info("[REAL-SEEDER] ✅ Contrainte missions_cargo_type_check supprimée (enum étendu)");
        } catch (Exception ex) {
            log.warn("[REAL-SEEDER] Impossible de supprimer la contrainte cargo_type : {}", ex.getMessage());
        }

        if (userRepo.existsByEmail("christarmandlemongo@gmail.com")
                && userRepo.existsByEmail("lkchrist4@gmail.com")
                && userRepo.existsByEmail("davashopoff@gmail.com")) {
            // Mettre à jour les mots de passe et s'assurer que les comptes sont actifs
            updateExistingAccounts();
            // Ne re-seeder les données que si les missions liées sont absentes
            if (missionRepo.findByImporterId(
                    userRepo.findByEmail("christarmandlemongo@gmail.com").get().getId()).size() >= 4) {
                log.info("[REAL-SEEDER] Données réelles déjà présentes — skip");
                return;
            }
        }

        log.info("[REAL-SEEDER] ═══════════════════════════════════════════════════");
        log.info("[REAL-SEEDER]  Création des comptes et données de test réels");
        log.info("[REAL-SEEDER] ═══════════════════════════════════════════════════");

        // ── 1. Créer / mettre à jour les 3 comptes réels ─────────────────────

        User christ = createOrUpdate(
            "christarmandlemongo@gmail.com",
            "Christ-Armand Le Mongo",
            "+237691234567",
            User.Role.IMPORTER
        );

        User lkchrist = createOrUpdate(
            "lkchrist4@gmail.com",
            "LK Christ — Admin",
            "+237699876543",
            User.Role.ADMIN
        );

        User dava = createOrUpdate(
            "davashopoff@gmail.com",
            "Dava Shopoff",
            "+237677654321",
            User.Role.DRIVER
        );

        log.info("[REAL-SEEDER] ✅ 3 comptes réels créés/mis à jour");
        log.info("[REAL-SEEDER]    christarmandlemongo@gmail.com → IMPORTATEUR");
        log.info("[REAL-SEEDER]    lkchrist4@gmail.com           → ADMIN");
        log.info("[REAL-SEEDER]    davashopoff@gmail.com         → CHAUFFEUR");
        log.info("[REAL-SEEDER]    Mot de passe commun           → Test1234!");

        // ── 2. Camion de davashopoff ──────────────────────────────────────────

        Truck davaTruck = truckOrCreate(
            "LT-DA-2024", dava,
            "Isuzu", "Forward FRR 450",
            new BigDecimal("15.0"),
            Truck.TruckType.FLATBED,
            LocalDate.now().plusMonths(6), true
        );

        // ── 3. MISSIONS ──────────────────────────────────────────────────────

        // M-OPEN : Créée par Christ, personne n'a encore accepté
        Mission mOpen = missionRepo.save(Mission.builder()
            .importer(christ)
            .originPoint(pt(DLA_LNG, DLA_LAT))
            .originLabel("Entrepôt Christ-Armand, Douala Bassa")
            .destinationPoint(pt(BFS_LNG, BFS_LAT))
            .destinationLabel("Marché des Grandes Surfaces, Bafoussam")
            .cargoDescription("500 sacs de cacao fermenté — 64 kg/sac, qualité Grade 1")
            .cargoWeightTons(new BigDecimal("15.5"))
            .cargoType(Mission.CargoType.COFFEE_COCOA)
            .specialInstructions("Marchandise sensible à l'humidité. Bâche obligatoire.")
            .totalPrice(new BigDecimal("228000"))
            .firstPaymentAmount(new BigDecimal("114000"))
            .commissionRate(new BigDecimal("0.075"))
            .missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.OPEN)
            .paymentStatus(Mission.PaymentStatus.PENDING)
            .pickupScheduledAt(OffsetDateTime.now().plusDays(2))
            .qrDeliveryToken(UUID.randomUUID().toString())
            .build());

        notif(christ, "✅ Mission publiée",
            "Votre mission Douala → Bafoussam est en ligne. En attente d'un transporteur.",
            "MISSION_CREATED", mOpen.getId().toString());

        // M-ASSIGNED : Dava a accepté, Christ doit payer la 1ère tranche
        Mission mAssigned = missionRepo.save(Mission.builder()
            .importer(christ)
            .driver(dava)
            .truck(davaTruck)
            .originPoint(pt(YDE_LNG, YDE_LAT))
            .originLabel("Dépôt SODECAO, Yaoundé — Zone Industrielle")
            .destinationPoint(pt(DLA_LNG, DLA_LAT))
            .destinationLabel("Port Autonome de Douala — Terminal Export")
            .cargoDescription("Fèves de cacao séchées — exportation Europe, 300 sacs")
            .cargoWeightTons(new BigDecimal("19.2"))
            .cargoType(Mission.CargoType.COFFEE_COCOA)
            .specialInstructions("Export certifié UTZ. Ne pas mélanger avec d'autres cargaisons.")
            .totalPrice(new BigDecimal("285000"))
            .firstPaymentAmount(new BigDecimal("142500"))
            .commissionRate(new BigDecimal("0.075"))
            .missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.ASSIGNED)
            .paymentStatus(Mission.PaymentStatus.PENDING)
            .pickupScheduledAt(OffsetDateTime.now().plusHours(18))
            .qrDeliveryToken(UUID.randomUUID().toString())
            .build());

        notif(christ, "🚛 Transporteur assigné !",
            "Dava Shopoff a accepté votre mission Yaoundé → Port de Douala. Versez 142 500 FCFA pour confirmer.",
            "DRIVER_ASSIGNED", mAssigned.getId().toString());
        notif(dava, "📦 Mission acceptée",
            "Vous avez accepté la mission SODECAO Yaoundé → Port Douala. En attente du paiement client.",
            "DRIVER_ASSIGNED", mAssigned.getId().toString());

        // M-TRANSIT : En cours — 1ère tranche payée, Dava en route
        Mission mTransit = missionRepo.save(Mission.builder()
            .importer(christ)
            .driver(dava)
            .truck(davaTruck)
            .originPoint(pt(DLA_LNG, DLA_LAT))
            .originLabel("Ferme Le Mongo, PK 12 — Douala")
            .destinationPoint(pt(NGD_LNG, NGD_LAT))
            .destinationLabel("Marché Central, Ngaoundéré")
            .cargoDescription("Maïs jaune séché en sacs de 50 kg — 480 sacs certifiés")
            .cargoWeightTons(new BigDecimal("24.0"))
            .cargoType(Mission.CargoType.CEREALS)
            .specialInstructions("Livraison obligatoire avant 16h. Stocker à l'abri de la pluie.")
            .totalPrice(new BigDecimal("348000"))
            .firstPaymentAmount(new BigDecimal("174000"))
            .commissionRate(new BigDecimal("0.075"))
            .missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.IN_TRANSIT)
            .paymentStatus(Mission.PaymentStatus.ESCROWED)
            .firstPaymentAt(OffsetDateTime.now().minusHours(5))
            .startedAt(OffsetDateTime.now().minusHours(4))
            .transitStartedAt(OffsetDateTime.now().minusHours(4))
            .driverArrivedAt(OffsetDateTime.now().minusHours(5))
            .pickupScheduledAt(OffsetDateTime.now().minusHours(6))
            .qrDeliveryToken(UUID.randomUUID().toString())
            .build());

        // Wallet — dépôt 1ère tranche
        walletRepo.save(PlatformWallet.builder()
            .mission(mTransit).payer(christ)
            .amount(new BigDecimal("174000"))
            .transactionType(PlatformWallet.TransactionType.DEPOSIT)
            .paymentMethod("MTN_MOMO").externalRef("MOMO-TRX-" + mTransit.getId().toString().substring(0,8))
            .status(PlatformWallet.TxStatus.CONFIRMED)
            .notes("Séquestre 1ère tranche — bloqué jusqu'à livraison")
            .build());

        // Positions GPS (trajet Douala → Ngaoundéré via axe Nord)
        double[][] track = {
            {9.7200, 4.1200, 52.0, 15.0},
            {9.8500, 4.5800, 68.0, 18.0},
            {10.1200, 5.2000, 71.0, 22.0},
            {10.6800, 5.9500, 65.0, 20.0},
            {11.2500, 6.5000, 58.0, 15.0},
            {11.9000, 6.9800, 73.0, 18.0},
            {12.5600, 7.1200, 62.0, 12.0},
            {13.1000, 7.2500, 55.0, 8.0},
        };
        int i = 0;
        for (double[] p : track) {
            telemetryRepo.save(TelemetryPosition.builder()
                .mission(mTransit).driver(dava)
                .position(pt(p[0], p[1]))
                .speedKmh(new BigDecimal(String.valueOf(p[2])))
                .headingDeg(new BigDecimal(String.valueOf(p[3])))
                .accuracyM(new BigDecimal("6.5"))
                .recordedAt(OffsetDateTime.now().minusMinutes(240 - i * 28))
                .wasOffline(false).build());
            i++;
        }

        // Chat — échanges réalistes sur la mission en transit
        chatRepo.save(chat(mTransit, christ,
            "Bonjour Dava, avez-vous bien chargé les 480 sacs ? Tout est en ordre ?"));
        chatRepo.save(chat(mTransit, dava,
            "Oui Monsieur Christ-Armand, chargement terminé à 08h15. Tous les sacs sont bien arrimés et bâchés. Je suis en route sur l'axe Nord."));
        chatRepo.save(chat(mTransit, christ,
            "Parfait ! La route Yaoundé-Ngaoundéré peut être difficile après Bafoussam. Faites attention aux nids-de-poule signalés vers Banyo."));
        chatRepo.save(chat(mTransit, dava,
            "Merci pour le conseil. J'ai vu les alertes sur l'app. Ma position actuelle : entre Bafoussam et Tibati. Tout va bien."));
        chatRepo.save(chat(mTransit, christ,
            "👍 Je vois votre position sur la carte. Bon courage ! Le client à Ngaoundéré attend impativement la livraison."));
        chatRepo.save(chat(mTransit, dava,
            "ETA approximatif : 14h30. Je vous confirme à l'arrivée. J'ai une photo du chargement si vous voulez."));
        chatRepo.save(chat(mTransit, christ,
            "Oui envoyez-la, c'est pour mon dossier export. Merci Dava."));
        // Message système — notification automatique
        chatRepo.save(ChatMessage.builder()
            .mission(mTransit).sender(dava)
            .content("📍 Position mise à jour — 62 km/h — 287 km restants vers Ngaoundéré")
            .msgType(ChatMessage.MsgType.SYSTEM).isRead(false).build());

        // Nid-de-poule signalé par Dava sur la route
        hazardRepo.save(RoadHazard.builder()
            .reportedBy(dava).mission(mTransit)
            .location(pt(10.6500, 5.8900))
            .shockMagnitude(new BigDecimal("3.85")).shockAxis("Z")
            .speedKmh(new BigDecimal("58.0"))
            .severity(RoadHazard.Severity.HIGH)
            .hazardType(RoadHazard.HazardType.POTHOLE)
            .confirmationCount(1).isValidated(false).isRepaired(false)
            .recordedAt(OffsetDateTime.now().minusHours(2))
            .syncedAt(OffsetDateTime.now().minusHours(1))
            .wasOffline(false).build());

        hazardRepo.save(RoadHazard.builder()
            .reportedBy(dava).mission(mTransit)
            .location(pt(11.1200, 6.3500))
            .shockMagnitude(new BigDecimal("5.20")).shockAxis("Z")
            .speedKmh(new BigDecimal("45.0"))
            .severity(RoadHazard.Severity.CRITICAL)
            .hazardType(RoadHazard.HazardType.BROKEN_ROAD)
            .confirmationCount(3).isValidated(true).isRepaired(false)
            .recordedAt(OffsetDateTime.now().minusHours(1))
            .syncedAt(OffsetDateTime.now().minusMinutes(30))
            .wasOffline(false).build());

        notif(christ, "🛣️ État route signalé",
            "Dava Shopoff a signalé un nid-de-poule CRITIQUE sur l'axe Bafoussam-Ngaoundéré.",
            "HAZARD_DETECTED", mTransit.getId().toString());
        notif(dava, "📤 Position synchronisée",
            "Vos données de télémétrie ont été envoyées au serveur.",
            "TELEMETRY_SYNC", mTransit.getId().toString());

        // M-DELIVERED : Livraison confirmée, Christ doit payer le solde
        Mission mDelivered = missionRepo.save(Mission.builder()
            .importer(christ)
            .driver(dava)
            .truck(davaTruck)
            .originPoint(pt(KRI_LNG, KRI_LAT))
            .originLabel("Usine Hévéa, Kribi — Zone Industrielle")
            .destinationPoint(pt(DLA_LNG, DLA_LAT))
            .destinationLabel("Dépôt SCDP, Douala-Bassa — Hangar 7")
            .cargoDescription("Caoutchouc naturel en blocs — 200 balles de 35 kg")
            .cargoWeightTons(new BigDecimal("7.0"))
            .cargoType(Mission.CargoType.AGRICULTURAL)
            .totalPrice(new BigDecimal("148000"))
            .firstPaymentAmount(new BigDecimal("74000"))
            .commissionRate(new BigDecimal("0.075"))
            .missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.DELIVERED)
            .paymentStatus(Mission.PaymentStatus.ESCROWED)
            .firstPaymentAt(OffsetDateTime.now().minusDays(1).minusHours(3))
            .startedAt(OffsetDateTime.now().minusDays(1).minusHours(2))
            .transitStartedAt(OffsetDateTime.now().minusDays(1).minusHours(2))
            .driverArrivedAt(OffsetDateTime.now().minusDays(1).minusHours(3))
            .deliveredAt(OffsetDateTime.now().minusHours(4))
            .qrScannedAt(OffsetDateTime.now().minusHours(4))
            .pickupScheduledAt(OffsetDateTime.now().minusDays(1).minusHours(4))
            .qrDeliveryToken(UUID.randomUUID().toString())
            .build());

        walletRepo.save(PlatformWallet.builder()
            .mission(mDelivered).payer(christ)
            .amount(new BigDecimal("74000"))
            .transactionType(PlatformWallet.TransactionType.DEPOSIT)
            .paymentMethod("ORANGE_MONEY").externalRef("OM-TRX-" + mDelivered.getId().toString().substring(0,8))
            .status(PlatformWallet.TxStatus.CONFIRMED)
            .notes("1ère tranche séquestrée — en attente du solde")
            .build());

        notif(christ, "📦 Livraison confirmée — Solde requis",
            "Dava Shopoff a livré votre caoutchouc à Douala. Payez le solde (74 000 FCFA) pour finaliser.",
            "DELIVERED", mDelivered.getId().toString());
        notif(dava, "✅ Livraison confirmée",
            "QR code scanné. Livraison validée. En attente du paiement du solde par le client.",
            "DELIVERED", mDelivered.getId().toString());
        notif(lkchrist, "💰 Séquestre actif",
            "Mission Kribi→Douala livrée. Solde en attente. Libération possible dans les 24h.",
            "DELIVERED", mDelivered.getId().toString());

        // M-CANCELLED : Annulée avant paiement
        Mission mCancelled = missionRepo.save(Mission.builder()
            .importer(christ)
            .originPoint(pt(BFS_LNG, BFS_LAT))
            .originLabel("Dépôt de Bafoussam — Avenue des Artisans")
            .destinationPoint(pt(YDE_LNG, YDE_LAT))
            .destinationLabel("Marché Central, Yaoundé")
            .cargoDescription("Pommes de terre — 150 sacs de 100 kg")
            .cargoWeightTons(new BigDecimal("15.0"))
            .cargoType(Mission.CargoType.AGRICULTURAL)
            .totalPrice(new BigDecimal("118000"))
            .firstPaymentAmount(new BigDecimal("59000"))
            .commissionRate(new BigDecimal("0.075"))
            .missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.CANCELLED)
            .paymentStatus(Mission.PaymentStatus.PENDING)
            .pickupScheduledAt(OffsetDateTime.now().minusDays(3))
            .qrDeliveryToken(UUID.randomUUID().toString())
            .build());

        notif(christ, "❌ Mission annulée",
            "Votre mission Bafoussam → Yaoundé a été annulée. Aucun frais débité.",
            "MISSION_CANCELLED", mCancelled.getId().toString());

        // M-DISPUTED : Litige ouvert
        Mission mDisputed = missionRepo.save(Mission.builder()
            .importer(christ)
            .driver(dava)
            .truck(davaTruck)
            .originPoint(pt(DLA_LNG, DLA_LAT))
            .originLabel("Entrepôt frigorifique, Douala-Akwa")
            .destinationPoint(pt(KRI_LNG, KRI_LAT))
            .destinationLabel("Restaurant Chez Marc, Kribi-Centre")
            .cargoDescription("Poissons fumés et crevettes congelées — 20 caisses isothermes")
            .cargoWeightTons(new BigDecimal("4.5"))
            .cargoType(Mission.CargoType.PERISHABLE)
            .specialInstructions("TEMPÉRATURE MAINTENUE < 4°C OBLIGATOIRE. Chaîne du froid critique.")
            .totalPrice(new BigDecimal("95000"))
            .firstPaymentAmount(new BigDecimal("47500"))
            .commissionRate(new BigDecimal("0.075"))
            .missionType(Mission.MissionType.EXPRESS)
            .expressSurcharge(new BigDecimal("0.300"))
            .status(Mission.MissionStatus.DISPUTED)
            .paymentStatus(Mission.PaymentStatus.DISPUTED)
            .firstPaymentAt(OffsetDateTime.now().minusDays(2))
            .startedAt(OffsetDateTime.now().minusDays(2).plusHours(1))
            .transitStartedAt(OffsetDateTime.now().minusDays(2).plusHours(1))
            .driverArrivedAt(OffsetDateTime.now().minusDays(2))
            .deliveredAt(OffsetDateTime.now().minusDays(1).minusHours(6))
            .pickupScheduledAt(OffsetDateTime.now().minusDays(2))
            .qrDeliveryToken(UUID.randomUUID().toString())
            .build());

        walletRepo.save(PlatformWallet.builder()
            .mission(mDisputed).payer(christ)
            .amount(new BigDecimal("47500"))
            .transactionType(PlatformWallet.TransactionType.DEPOSIT)
            .paymentMethod("MTN_MOMO").externalRef("MOMO-DISP-" + mDisputed.getId().toString().substring(0,8))
            .status(PlatformWallet.TxStatus.CONFIRMED)
            .notes("⚠️ Fonds bloqués — litige en cours")
            .build());

        // Alerte litige
        alertRepo.save(Alert.builder()
            .sender(christ).mission(mDisputed)
            .alertType(Alert.AlertType.DISTRESS)
            .location(pt(9.9300, 3.0200))
            .message("Marchandise avariée à l'arrivée : 8 caisses de poissons décongelées. La chaîne du froid n'a pas été respectée. Pertes estimées : 35 000 FCFA.")
            .smsSent(true).isResolved(false).build());

        notif(christ, "⚠️ Litige ouvert — Fonds bloqués",
            "Votre litige sur la mission frigorifique Douala→Kribi est en cours d'examen. Les 47 500 FCFA sont sécurisés.",
            "DISPUTE_OPENED", mDisputed.getId().toString());
        notif(dava, "⚠️ Litige signalé contre vous",
            "Christ-Armand Le Mongo a ouvert un litige : chaîne du froid non respectée. Les fonds sont bloqués en attente de l'admin.",
            "DISPUTE_OPENED", mDisputed.getId().toString());
        notif(lkchrist, "🚨 LITIGE À TRAITER",
            "Litige prioritaire — poissons avariés. Client : Christ-Armand. Transporteur : Dava Shopoff. Montant bloqué : 47 500 FCFA.",
            "DISPUTE_OPENED", mDisputed.getId().toString());

        log.info("[REAL-SEEDER] ✅ 6 missions créées (OPEN, ASSIGNED, IN_TRANSIT, DELIVERED, CANCELLED, DISPUTED)");

        // ── 4. Mission RELEASED — historique complet (mission terminée) ───────

        Mission mReleased = missionRepo.save(Mission.builder()
            .importer(christ)
            .driver(dava)
            .truck(davaTruck)
            .originPoint(pt(DLA_LNG, DLA_LAT))
            .originLabel("Coopérative cacaoyère, Douala Bangue")
            .destinationPoint(pt(YDE_LNG, YDE_LAT))
            .destinationLabel("CHOCOCAM, Yaoundé — Usine de transformation")
            .cargoDescription("Cacao fermenté et séché Grade A — pour transformation industrielle")
            .cargoWeightTons(new BigDecimal("10.0"))
            .cargoType(Mission.CargoType.COFFEE_COCOA)
            .totalPrice(new BigDecimal("165000"))
            .firstPaymentAmount(new BigDecimal("82500"))
            .commissionRate(new BigDecimal("0.075"))
            .missionType(Mission.MissionType.NORMAL)
            .status(Mission.MissionStatus.DELIVERED)
            .paymentStatus(Mission.PaymentStatus.RELEASED)
            .firstPaymentAt(OffsetDateTime.now().minusDays(5))
            .secondPaymentAt(OffsetDateTime.now().minusDays(3))
            .startedAt(OffsetDateTime.now().minusDays(5).plusHours(2))
            .transitStartedAt(OffsetDateTime.now().minusDays(5).plusHours(2))
            .driverArrivedAt(OffsetDateTime.now().minusDays(5).plusHours(1))
            .deliveredAt(OffsetDateTime.now().minusDays(4))
            .qrScannedAt(OffsetDateTime.now().minusDays(4))
            .pickupScheduledAt(OffsetDateTime.now().minusDays(5))
            .qrDeliveryToken(UUID.randomUUID().toString())
            .build());

        walletRepo.save(PlatformWallet.builder().mission(mReleased).payer(christ)
            .amount(new BigDecimal("82500")).transactionType(PlatformWallet.TransactionType.DEPOSIT)
            .paymentMethod("MTN_MOMO").externalRef("MOMO-REL1-" + mReleased.getId().toString().substring(0,8))
            .status(PlatformWallet.TxStatus.CONFIRMED).notes("1ère tranche").build());
        walletRepo.save(PlatformWallet.builder().mission(mReleased).payer(christ)
            .amount(new BigDecimal("82500")).transactionType(PlatformWallet.TransactionType.DEPOSIT)
            .paymentMethod("MTN_MOMO").externalRef("MOMO-REL2-" + mReleased.getId().toString().substring(0,8))
            .status(PlatformWallet.TxStatus.CONFIRMED).notes("Solde (2ème tranche)").build());
        walletRepo.save(PlatformWallet.builder().mission(mReleased)
            .amount(new BigDecimal("12375")).transactionType(PlatformWallet.TransactionType.COMMISSION)
            .status(PlatformWallet.TxStatus.CONFIRMED).notes("Commission KmerFret 7.5%").build());
        walletRepo.save(PlatformWallet.builder().mission(mReleased)
            .amount(new BigDecimal("152625")).transactionType(PlatformWallet.TransactionType.RELEASE)
            .status(PlatformWallet.TxStatus.CONFIRMED).notes("Virement chauffeur Dava Shopoff").build());

        // Avis croisés sur la mission terminée
        reviewRepo.save(Review.builder()
            .mission(mReleased).reviewer(christ).reviewed(dava)
            .rating((short) 5)
            .comment("Excellent transporteur ! Ponctuel, sérieux, marchandises livrées en parfait état. Je recommande Dava à 100%.")
            .build());
        reviewRepo.save(Review.builder()
            .mission(mReleased).reviewer(dava).reviewed(christ)
            .rating((short) 5)
            .comment("Client très professionnel. Marchandises parfaitement emballées, instructions claires. Un plaisir de travailler ensemble.")
            .build());

        notif(christ, "⭐ Merci pour votre avis !",
            "Votre évaluation de Dava Shopoff a été enregistrée. Merci de contribuer à la qualité du service.",
            "REVIEW_SUBMITTED", mReleased.getId().toString());
        notif(dava, "💸 Paiement reçu — 152 625 FCFA",
            "Votre paiement pour la mission CHOCOCAM a été transféré. Mission terminée avec succès !",
            "PAYMENT_RELEASED", mReleased.getId().toString());
        notif(dava, "⭐ Nouvelle évaluation",
            "Christ-Armand Le Mongo vous a donné 5 étoiles sur la mission Douala → CHOCOCAM.",
            "REVIEW_RECEIVED", mReleased.getId().toString());

        log.info("[REAL-SEEDER] ✅ Mission RELEASED créée (avec wallet complet + avis 5★)");

        // ── 5. Candidatures pour davashopoff ─────────────────────────────────

        // Candidature APPROVED — dossier de Dava lui-même
        DriverApplication davApp = driverAppRepo.save(DriverApplication.builder()
            .fullName("Dava Shopoff")
            .email("dava.shopoff.candidature@gmail.com")
            .phone("+237677654321")
            .city("Douala")
            .motivation("Chauffeur professionnel avec 7 ans d'expérience sur l'axe Douala-Yaoundé-Nord Cameroun. Permis C+D, connaissance approfondie des routes nationales. Spécialisé produits agricoles et vivriers.")
            .status(DriverApplication.Status.APPROVED)
            .adminNotes("Dossier complet et vérifié. Permis valide jusqu'en 2027. Casier judiciaire vierge. Approuvé pour toutes catégories de transport.")
            .createdUser(dava)
            .reviewedBy(lkchrist)
            .reviewedAt(OffsetDateTime.now().minusDays(10))
            .build());

        appMsgRepo.save(ApplicationMessage.builder().application(davApp)
            .senderRole(ApplicationMessage.SenderRole.APPLICANT)
            .content("Bonjour, je souhaite rejoindre la plateforme KmerFret. J'ai joint tous mes documents : CNI, permis C+D, assurance véhicule et attestation d'expérience. Je suis disponible immédiatement.")
            .build());
        appMsgRepo.save(ApplicationMessage.builder().application(davApp)
            .senderRole(ApplicationMessage.SenderRole.ADMIN)
            .content("Bonjour Dava. Nous avons bien reçu votre dossier. Votre permis et vos documents sont en cours de vérification. Nous reviendrons vers vous sous 48h.")
            .build());
        appMsgRepo.save(ApplicationMessage.builder().application(davApp)
            .senderRole(ApplicationMessage.SenderRole.APPLICANT)
            .content("Merci pour la réponse. Je reste disponible pour tout renseignement complémentaire.")
            .build());
        appMsgRepo.save(ApplicationMessage.builder().application(davApp)
            .senderRole(ApplicationMessage.SenderRole.ADMIN)
            .content("🎉 Félicitations Dava ! Votre dossier est approuvé. Vous pouvez dès maintenant accéder à votre espace chauffeur sur KmerFret et accepter des missions. Bienvenue dans la flotte !")
            .build());

        // Candidature PENDING d'un autre candidat
        DriverApplication pendingApp = driverAppRepo.save(DriverApplication.builder()
            .fullName("Rodrigue Tchamba Essomba")
            .email("rodrigue.tchamba@yahoo.fr")
            .phone("+237655112233")
            .city("Yaoundé")
            .motivation("3 ans d'expérience en transport de marchandises sur l'axe Centre-Sud. Camion personnel TIPPER disponible.")
            .status(DriverApplication.Status.PENDING)
            .build());

        appMsgRepo.save(ApplicationMessage.builder().application(pendingApp)
            .senderRole(ApplicationMessage.SenderRole.APPLICANT)
            .content("Bonjour KmerFret, voici ma candidature. Je joins mon permis poids lourd et mon casier judiciaire.")
            .build());

        // Candidature REJECTED
        DriverApplication rejectedApp = driverAppRepo.save(DriverApplication.builder()
            .fullName("Serge Mougang")
            .email("serge.mougang123@gmail.com")
            .phone("+237699887766")
            .city("Bafoussam")
            .motivation("Je veux transporter des marchandises.")
            .status(DriverApplication.Status.REJECTED)
            .adminNotes("Permis de conduire expiré depuis 8 mois. Aucune attestation d'expérience fournie. Dossier incomplet.")
            .reviewedBy(lkchrist)
            .reviewedAt(OffsetDateTime.now().minusDays(2))
            .build());

        appMsgRepo.save(ApplicationMessage.builder().application(rejectedApp)
            .senderRole(ApplicationMessage.SenderRole.ADMIN)
            .content("Bonjour Serge. Après examen de votre dossier, nous ne pouvons pas traiter votre candidature car votre permis est expiré et votre dossier est incomplet. Vous pouvez soumettre un nouveau dossier une fois votre permis renouvelé.")
            .build());

        log.info("[REAL-SEEDER] ✅ 3 candidatures (APPROVED-Dava, PENDING, REJECTED)");

        // ── 6. Notifications supplémentaires ─────────────────────────────────

        notif(christ, "🎉 Bienvenue sur KmerFret !",
            "Votre compte importateur est actif. Créez votre première mission et connectez-vous aux meilleurs transporteurs du Cameroun.",
            "WELCOME", null);
        notif(christ, "📊 Stats du mois",
            "Ce mois-ci : 2 missions livrées, 165 000 FCFA de marchandises transportées. Continuez !",
            "MONTHLY_STATS", null);
        notif(lkchrist, "📈 Rapport plateforme",
            "Aujourd'hui : 7 missions actives, 3 litiges en cours, 2 candidatures à traiter. Connexion requise.",
            "ADMIN_REPORT", null);
        notif(lkchrist, "👤 Nouveau compte importateur",
            "Christ-Armand Le Mongo vient de rejoindre KmerFret.",
            "NEW_USER", null);
        notif(dava, "🏆 Profil complet",
            "Votre profil est complet à 95%. Ajoutez une photo de profil pour augmenter vos chances d'être choisi.",
            "PROFILE_TIP", null);
        notif(dava, "📍 Nid-de-poule validé",
            "Votre signalement sur l'axe Bafoussam-Ngaoundéré a été validé par 3 conducteurs. Merci !",
            "HAZARD_VALIDATED", null);

        // ── 7. Résumé final ───────────────────────────────────────────────────
        log.info("[REAL-SEEDER] ═══════════════════════════════════════════════════════");
        log.info("[REAL-SEEDER]  DONNÉES RÉELLES INSÉRÉES AVEC SUCCÈS");
        log.info("[REAL-SEEDER] ───────────────────────────────────────────────────────");
        log.info("[REAL-SEEDER]  MOT DE PASSE : Test1234! (pour les 3 comptes)");
        log.info("[REAL-SEEDER] ───────────────────────────────────────────────────────");
        log.info("[REAL-SEEDER]  IMPORTATEUR : christarmandlemongo@gmail.com");
        log.info("[REAL-SEEDER]  ADMIN       : lkchrist4@gmail.com");
        log.info("[REAL-SEEDER]  CHAUFFEUR   : davashopoff@gmail.com");
        log.info("[REAL-SEEDER] ───────────────────────────────────────────────────────");
        log.info("[REAL-SEEDER]  MISSIONS (7) :");
        log.info("[REAL-SEEDER]    OPEN      → cacao Douala→Bafoussam");
        log.info("[REAL-SEEDER]    ASSIGNED  → fèves Yaoundé→Port Douala (paiement requis)");
        log.info("[REAL-SEEDER]    IN_TRANSIT→ maïs Douala→Ngaoundéré (GPS actif + chat)");
        log.info("[REAL-SEEDER]    DELIVERED → caoutchouc Kribi→Douala (solde à payer)");
        log.info("[REAL-SEEDER]    RELEASED  → cacao Douala→CHOCOCAM (terminée + avis 5★)");
        log.info("[REAL-SEEDER]    CANCELLED → pommes de terre Bafoussam→Yaoundé");
        log.info("[REAL-SEEDER]    DISPUTED  → poissons Douala→Kribi (litige chaîne froid)");
        log.info("[REAL-SEEDER]  HAZARDS     : 2 nids-de-poule (HIGH + CRITICAL)");
        log.info("[REAL-SEEDER]  CHAT        : 8 messages sur la mission IN_TRANSIT");
        log.info("[REAL-SEEDER]  AVIS        : 2 × 5★ (sur mission RELEASED)");
        log.info("[REAL-SEEDER]  CANDIDATURES: 3 (APPROVED-Dava, PENDING, REJECTED)");
        log.info("[REAL-SEEDER]  NOTIFICATIONS: ~18 réparties sur les 3 comptes");
        log.info("[REAL-SEEDER] ═══════════════════════════════════════════════════════");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private Point pt(double lng, double lat) {
        return GEO.createPoint(new Coordinate(lng, lat));
    }

    private User createOrUpdate(String email, String name, String phone, User.Role role) {
        return userRepo.findByEmail(email).map(u -> {
            u.setFullName(name);
            u.setPasswordHash(PWD_HASH);
            u.setEmailVerified(true);
            u.setIsActive(true);
            u.setAccountStatus(User.AccountStatus.ACTIVE);
            u.setRole(role);
            return userRepo.save(u);
        }).orElseGet(() -> userRepo.save(User.builder()
            .fullName(name).email(email).phone(phone)
            .passwordHash(PWD_HASH)
            .role(role).emailVerified(true).isActive(true)
            .accountStatus(User.AccountStatus.ACTIVE).build()));
    }

    private void updateExistingAccounts() {
        userRepo.findByEmail("christarmandlemongo@gmail.com").ifPresent(u -> {
            u.setPasswordHash(PWD_HASH); u.setEmailVerified(true); u.setIsActive(true);
            u.setAccountStatus(User.AccountStatus.ACTIVE); userRepo.save(u);
        });
        userRepo.findByEmail("lkchrist4@gmail.com").ifPresent(u -> {
            u.setPasswordHash(PWD_HASH); u.setEmailVerified(true); u.setIsActive(true);
            u.setAccountStatus(User.AccountStatus.ACTIVE); userRepo.save(u);
        });
        userRepo.findByEmail("davashopoff@gmail.com").ifPresent(u -> {
            u.setPasswordHash(PWD_HASH); u.setEmailVerified(true); u.setIsActive(true);
            u.setAccountStatus(User.AccountStatus.ACTIVE); userRepo.save(u);
        });
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

    private ChatMessage chat(Mission m, User sender, String content) {
        return ChatMessage.builder()
            .mission(m).sender(sender).content(content)
            .msgType(ChatMessage.MsgType.TEXT).isRead(false).build();
    }

    private void notif(User user, String title, String body, String type, String refId) {
        notifRepo.save(Notification.builder()
            .user(user).title(title).body(body)
            .notifType(type).referenceId(refId).isRead(false).build());
    }
}
