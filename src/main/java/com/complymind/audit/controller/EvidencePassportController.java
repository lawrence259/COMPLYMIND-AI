package com.complymind.audit.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.audit.entity.AuditEvent;
import com.complymind.audit.service.AuditService;
import com.complymind.digitaltwin.model.InstitutionalDigitalTwin;
import com.complymind.digitaltwin.service.DigitalTwinService;
import com.complymind.discrepancy.entity.DiscrepancyFinding;
import com.complymind.discrepancy.service.DiscrepancyDetectionService;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.service.EvidenceReviewService;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.service.RequirementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Controller
public class EvidencePassportController {

    private final ComplianceApplicationService applicationService;
    private final EvidenceReviewService evidenceService;
    private final RequirementService requirementService;
    private final AuditService auditService;
    private final DigitalTwinService digitalTwinService;
    private final DiscrepancyDetectionService discrepancyService;

    public EvidencePassportController(ComplianceApplicationService applicationService,
                                     EvidenceReviewService evidenceService,
                                     RequirementService requirementService,
                                     AuditService auditService,
                                     DigitalTwinService digitalTwinService,
                                     DiscrepancyDetectionService discrepancyService) {
        this.applicationService = applicationService;
        this.evidenceService = evidenceService;
        this.requirementService = requirementService;
        this.auditService = auditService;
        this.digitalTwinService = digitalTwinService;
        this.discrepancyService = discrepancyService;
    }

    @GetMapping("/applications/{id}/passport")
    public String showEvidencePassport(@PathVariable Long id, Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        List<EvidenceDocument> documents = evidenceService.getEvidenceForApplication(id);
        List<ApplicationRequirement> requirements = requirementService.getRequirementsForApplication(id);
        List<AuditEvent> auditHistory = auditService.getEventsForEntity("ComplianceApplication", id.toString());
        List<DiscrepancyFinding> discrepancies = discrepancyService.getFindingsForApplication(id);

        InstitutionalDigitalTwin twin = null;
        if (app.getInstitution() != null) {
            twin = digitalTwinService.computeDigitalTwin(app.getInstitution().getId());
        }

        // Compute Cryptographic Verification Seal
        String passportSeal = computePassportChecksum(app, documents);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("documents", documents);
        model.addAttribute("requirements", requirements);
        model.addAttribute("auditHistory", auditHistory);
        model.addAttribute("discrepancies", discrepancies);
        model.addAttribute("twin", twin);
        model.addAttribute("passportSeal", passportSeal);
        model.addAttribute("activeMenu", "applications");

        return "evidence/passport";
    }

    @GetMapping("/audit")
    public String showFullAuditLog(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   Model model) {
        Page<AuditEvent> eventPage = auditService.getAuditEvents(PageRequest.of(page, size));
        model.addAttribute("events", eventPage);
        model.addAttribute("activeMenu", "audit");
        return "audit/list";
    }

    private String computePassportChecksum(ComplianceApplication app, List<EvidenceDocument> documents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(app.getApplicationNumber()).append(":");
            sb.append(app.getStatus()).append(":");
            sb.append(app.getRiskScore()).append(":");
            if (app.getInstitution() != null) {
                sb.append(app.getInstitution().getCode()).append(":");
            }
            for (EvidenceDocument doc : documents) {
                if (doc.getSha256Checksum() != null) {
                    sb.append(doc.getSha256Checksum()).append(";");
                }
            }
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toUpperCase();
        } catch (Exception e) {
            return "UNVERIFIED_SEAL_COMPUTATION_ERROR";
        }
    }
}
