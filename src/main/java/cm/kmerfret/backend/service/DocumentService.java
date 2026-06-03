package cm.kmerfret.backend.service;

import cm.kmerfret.backend.dto.request.UploadDocumentRequest;
import cm.kmerfret.backend.dto.response.DocumentResponse;
import cm.kmerfret.backend.exception.ResourceNotFoundException;
import cm.kmerfret.backend.model.Mission;
import cm.kmerfret.backend.model.MissionDocument;
import cm.kmerfret.backend.model.User;
import cm.kmerfret.backend.repository.MissionDocumentRepository;
import cm.kmerfret.backend.repository.MissionRepository;
import cm.kmerfret.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final MissionDocumentRepository documentRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    private User currentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public DocumentResponse uploadDocument(UploadDocumentRequest req) {
        User uploader = currentUser();
        Mission mission = missionRepository.findById(req.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mission introuvable : " + req.getMissionId()));

        MissionDocument doc = MissionDocument.builder()
                .mission(mission)
                .uploadedBy(uploader)
                .docType(MissionDocument.DocType.valueOf(req.getDocType()))
                .fileUrl(req.getFileUrl())
                .ocrRawText(req.getOcrRawText())
                .ocrConfidence(req.getOcrConfidence())
                .build();

        return DocumentResponse.from(documentRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getMissionDocuments(UUID missionId) {
        if (!missionRepository.existsById(missionId)) {
            throw new ResourceNotFoundException("Mission introuvable : " + missionId);
        }
        return documentRepository.findByMissionId(missionId)
                .stream().map(DocumentResponse::from).collect(Collectors.toList());
    }
}
