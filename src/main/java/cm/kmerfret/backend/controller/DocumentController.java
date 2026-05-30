package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.dto.request.UploadDocumentRequest;
import cm.kmerfret.backend.dto.response.ApiResponse;
import cm.kmerfret.backend.dto.response.DocumentResponse;
import cm.kmerfret.backend.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Valid @RequestBody UploadDocumentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(documentService.uploadDocument(req)));
    }

    @GetMapping("/mission/{missionId}")
    public ResponseEntity<ApiResponse<java.util.List<DocumentResponse>>> getMissionDocuments(
            @PathVariable java.util.UUID missionId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getMissionDocuments(missionId)));
    }
}
