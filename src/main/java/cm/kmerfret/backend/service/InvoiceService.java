package cm.kmerfret.backend.service;

import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.exception.UnauthorizedException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.UserRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    private static final DeviceRgb GREEN  = new DeviceRgb(27,  94,  32);
    private static final DeviceRgb LGREY  = new DeviceRgb(245, 245, 245);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateInvoice(UUID missionId) {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Mission m = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable"));

        boolean isImporter = m.getImporter().getId().equals(user.getId());
        boolean isDriver   = m.getDriver() != null && m.getDriver().getId().equals(user.getId());
        if (!isImporter && !isDriver && user.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Accès non autorisé à cette facture");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf);

            // ─── En-tête ───────────────────────────────────────────────────────
            Paragraph title = new Paragraph("KMERFRЕT")
                .setFontColor(GREEN)
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
            doc.add(title);

            doc.add(new Paragraph("Facture de Transport Agricole")
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12));

            doc.add(new Paragraph(" "));

            // ─── Infos mission ──────────────────────────────────────────────────
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            infoTable.setBackgroundColor(LGREY);
            addCell(infoTable, "Numéro de mission",   m.getId().toString().substring(0, 8).toUpperCase());
            addCell(infoTable, "Date de création",     m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : "—");
            addCell(infoTable, "Livraison confirmée",  m.getDeliveredAt() != null ? m.getDeliveredAt().format(FMT) : "En attente");
            addCell(infoTable, "Statut",               m.getStatus().name());
            doc.add(infoTable);

            doc.add(new Paragraph(" "));

            // ─── Parties ───────────────────────────────────────────────────────
            Table partiesTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            Paragraph impTitle = new Paragraph("EXPORTATEUR / PRODUCTEUR").setFontColor(GREEN).setBold();
            partiesTable.addCell(new Cell().add(impTitle).add(new Paragraph(m.getImporter().getFullName())).add(new Paragraph(m.getImporter().getPhone())));
            Paragraph drTitle  = new Paragraph("CHAUFFEUR").setFontColor(GREEN).setBold();
            String driverName  = m.getDriver() != null ? m.getDriver().getFullName() : "Non assigné";
            String driverPhone = m.getDriver() != null ? m.getDriver().getPhone()    : "—";
            partiesTable.addCell(new Cell().add(drTitle).add(new Paragraph(driverName)).add(new Paragraph(driverPhone)));
            doc.add(partiesTable);

            doc.add(new Paragraph(" "));

            // ─── Route ─────────────────────────────────────────────────────────
            doc.add(new Paragraph("ITINÉRAIRE").setFontColor(GREEN).setBold().setFontSize(12));
            doc.add(new Paragraph("Origine : " + m.getOriginLabel()));
            doc.add(new Paragraph("Destination : " + m.getDestinationLabel()));
            doc.add(new Paragraph("Cargaison : " + m.getCargoDescription()));
            doc.add(new Paragraph("Type : " + (m.getCargoType() != null ? m.getCargoType().name() : "—")));
            doc.add(new Paragraph("Poids : " + m.getCargoWeightTons() + " tonnes"));

            doc.add(new Paragraph(" "));

            // ─── Finances ──────────────────────────────────────────────────────
            doc.add(new Paragraph("DÉTAIL FINANCIER").setFontColor(GREEN).setBold().setFontSize(12));
            Table finTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
            addFinRow(finTable, "Prix total transport",     m.getTotalPrice() != null ? m.getTotalPrice().toPlainString() + " FCFA" : "—", false);
            addFinRow(finTable, "Commission KmerFret (7.5%)", m.getCommissionAmount() != null ? "- " + m.getCommissionAmount().toPlainString() + " FCFA" : "—", false);
            addFinRow(finTable, "Payout chauffeur",         m.getDriverPayout() != null ? m.getDriverPayout().toPlainString() + " FCFA" : "—", true);
            doc.add(finTable);

            doc.add(new Paragraph(" "));

            // ─── Pied de page ──────────────────────────────────────────────────
            doc.add(new Paragraph("Ce document est généré automatiquement par la plateforme KmerFret.")
                .setFontColor(ColorConstants.GRAY).setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("KmerFret — Transport agricole sécurisé | Douala, Cameroun")
                .setFontColor(ColorConstants.GRAY).setFontSize(9).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage());
        }
    }

    private void addCell(Table t, String key, String value) {
        t.addCell(new Cell().add(new Paragraph(key).setBold().setFontSize(9)));
        t.addCell(new Cell().add(new Paragraph(value).setFontSize(9)));
    }

    private void addFinRow(Table t, String label, String amount, boolean total) {
        Paragraph p1 = new Paragraph(label).setFontSize(total ? 11 : 10);
        Paragraph p2 = new Paragraph(amount).setFontSize(total ? 13 : 10).setTextAlignment(TextAlignment.RIGHT);
        if (total) { p1.setBold(); p2.setBold(); }
        Cell c1 = new Cell().add(p1);
        Cell c2 = new Cell().add(p2);
        if (total) {
            c1.setFontColor(GREEN).setBold();
            c2.setFontColor(GREEN).setBold();
        }
        t.addCell(c1);
        t.addCell(c2);
    }
}
