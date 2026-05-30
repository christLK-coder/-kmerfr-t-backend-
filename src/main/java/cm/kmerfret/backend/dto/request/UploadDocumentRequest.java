package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public class UploadDocumentRequest {

    @NotNull(message = "L'identifiant de la mission est obligatoire")
    private UUID missionId;

    @NotBlank(message = "Le type de document est obligatoire")
    @Pattern(regexp = "^(LETTRE_VOITURE|BON_SORTIE|FACTURE|INSURANCE|ID_CARD|PERMIT|OTHER)$",
             message = "Type de document invalide")
    private String docType;

    @NotBlank(message = "L'URL du fichier est obligatoire")
    private String fileUrl;

    private String ocrRawText;
    private BigDecimal ocrConfidence;

    public UUID getMissionId() { return missionId; }
    public void setMissionId(UUID missionId) { this.missionId = missionId; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getOcrRawText() { return ocrRawText; }
    public void setOcrRawText(String ocrRawText) { this.ocrRawText = ocrRawText; }

    public BigDecimal getOcrConfidence() { return ocrConfidence; }
    public void setOcrConfidence(BigDecimal ocrConfidence) { this.ocrConfidence = ocrConfidence; }
}
