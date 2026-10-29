package com.complymind.ingestion.controller;

import com.complymind.common.dto.ApiResponse;
import com.complymind.framework.repository.ComplianceRuleRepository;
import com.complymind.ingestion.model.DataClassification;
import com.complymind.ingestion.model.DataIngestionResult;
import com.complymind.ingestion.service.PublicDataIngestionService;
import com.complymind.institution.repository.InstitutionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Tag(name = "Data Ingestion", description = "Public, Open-Source & Official AICTE Regulatory Data Pipeline")
public class DataIngestionController {

    private final PublicDataIngestionService ingestionService;
    private final InstitutionRepository institutionRepository;
    private final ComplianceRuleRepository ruleRepository;

    public DataIngestionController(PublicDataIngestionService ingestionService,
                                   InstitutionRepository institutionRepository,
                                   ComplianceRuleRepository ruleRepository) {
        this.ingestionService = ingestionService;
        this.institutionRepository = institutionRepository;
        this.ruleRepository = ruleRepository;
    }

    @GetMapping("/admin/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public String showIngestionDashboard(Model model) {
        model.addAttribute("totalInstitutions", institutionRepository.count());
        model.addAttribute("totalRules", ruleRepository.count());
        model.addAttribute("classifications", DataClassification.values());
        model.addAttribute("officialUrl", PublicDataIngestionService.AICTE_APH_OFFICIAL_URL);
        return "ingestion/index";
    }

    @PostMapping("/admin/ingest/institutions")
    @PreAuthorize("hasRole('ADMIN')")
    public String uploadInstitutionsCsv(@RequestParam("file") MultipartFile file,
                                        @RequestParam(required = false) String sourceUrl,
                                        @RequestParam(defaultValue = "PUBLIC_SOURCE_UNVERIFIED") DataClassification classification,
                                        RedirectAttributes redirectAttributes) {
        try {
            DataIngestionResult result = ingestionService.ingestInstitutionsFromCsv(file, sourceUrl, classification);
            redirectAttributes.addFlashAttribute("ingestResult", result);
            redirectAttributes.addFlashAttribute("successMessage",
                    "CSV Ingestion Processed: " + result.getAddedCount() + " added, " +
                            result.getUpdatedCount() + " updated, " + result.getRejectedCount() + " rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ingestion failed: " + e.getMessage());
        }
        return "redirect:/admin/ingest";
    }

    @PostMapping("/admin/ingest/official-catalog")
    @PreAuthorize("hasRole('ADMIN')")
    public String syncOfficialCatalog(RedirectAttributes redirectAttributes) {
        try {
            DataIngestionResult result = ingestionService.ingestVerifiedAictePublicCatalog();
            redirectAttributes.addFlashAttribute("ingestResult", result);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Official AICTE Statutory Handbook catalog synced: " + result.getAddedCount() +
                            " added, " + result.getUpdatedCount() + " updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Catalog synchronization failed: " + e.getMessage());
        }
        return "redirect:/admin/ingest";
    }

    @PostMapping("/api/ingest/institutions")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "REST API: Bulk ingest institutions via CSV upload")
    public ResponseEntity<ApiResponse<DataIngestionResult>> apiIngestInstitutions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(defaultValue = "PUBLIC_SOURCE_UNVERIFIED") DataClassification classification) {

        DataIngestionResult result = ingestionService.ingestInstitutionsFromCsv(file, sourceUrl, classification);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
