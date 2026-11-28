package com.cms.controller;

import com.cms.dto.response.CsvImportSummaryResponse;
import com.cms.security.UserPrincipal;
import com.cms.service.CsvExportService;
import com.cms.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/csv")
@Tag(name = "CSV Import & Export")
public class CsvController {

    private final CsvExportService csvExportService;
    private final CsvImportService csvImportService;

    public CsvController(CsvExportService csvExportService, CsvImportService csvImportService) {
        this.csvExportService = csvExportService;
        this.csvImportService = csvImportService;
    }

    @GetMapping("/export")
    @Operation(summary = "Export all user contacts to downloadable CSV file")
    public ResponseEntity<byte[]> exportContacts(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] csvData = csvExportService.exportContactsToCsv(principal.id());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contacts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/template")
    @Operation(summary = "Download standard CSV template for contact imports")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] templateData = csvExportService.getCsvTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contacts_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(templateData);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and import contacts from CSV file")
    public ResponseEntity<CsvImportSummaryResponse> importContacts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        CsvImportSummaryResponse summary = csvImportService.importContactsFromCsv(principal.id(), file);
        return ResponseEntity.ok(summary);
    }
}
