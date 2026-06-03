package cm.kmerfret.backend.controller;

import cm.kmerfret.backend.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/missions/{missionId}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID missionId) {
        byte[] pdf = invoiceService.generateInvoice(missionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"facture-kmerfret-" + missionId.toString().substring(0, 8) + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
