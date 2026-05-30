package cm.kmerfret.backend.dto.response;

import cm.kmerfret.backend.model.MissionDocument;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DocumentResponse {

    private UUID id;
    private UUID missionId;
    private UUID uploadedById;
    private String docType;
    private String fileUrl;
    private String ocrRawText;
    private BigDecimal ocrConfidence;
    private Boolean isVerified;
    private OffsetDateTime createdAt;

    public static DocumentResponse from(MissionDocument d) {
        DocumentResponse r = new DocumentResponse();
        r.id = d.getId();
        r.missionId = d.getMission().getId();
        r.uploadedById = d.getUploadedBy().getId();
        r.docType = d.getDocType() != null ? d.getDocType().name() : null;
        r.fileUrl = d.getFileUrl();
        r.ocrRawText = d.getOcrRawText();
        r.ocrConfidence = d.getOcrConfidence();
        r.isVerified = d.getIsVerified();
        r.createdAt = d.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getMissionId() { return missionId; }
    public UUID getUploadedById() { return uploadedById; }
    public String getDocType() { return docType; }
    public String getFileUrl() { return fileUrl; }
    public String getOcrRawText() { return ocrRawText; }
    public BigDecimal getOcrConfidence() { return ocrConfidence; }
    public Boolean getIsVerified() { return isVerified; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
