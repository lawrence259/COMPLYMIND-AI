package com.complymind.audit.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.audit.entity.AuditEvent;
import com.complymind.audit.service.AuditService;
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

import java.util.List;

@Controller
public class EvidencePassportController {

    private final ComplianceApplicationService applicationService;
    private final EvidenceReviewService evidenceService;
    private final RequirementService requirementService;
    private final AuditService auditService;

    public EvidencePassportController(ComplianceApplicationService applicationService,
                                     EvidenceReviewService evidenceService,
                                     RequirementService requirementService,
                                     AuditService auditService) {
        this.applicationService = applicationService;
        this.evidenceService = evidenceService;
        this.requirementService = requirementService;
        this.auditService = auditService;
    }

    @GetMapping("/applications/{id}/passport")
    public String showEvidencePassport(@PathVariable Long id, Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        List<EvidenceDocument> documents = evidenceService.getEvidenceForApplication(id);
        List<ApplicationRequirement> requirements = requirementService.getRequirementsForApplication(id);
        List<AuditEvent> auditHistory = auditService.getEventsForEntity("ComplianceApplication", id.toString());

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("documents", documents);
        model.addAttribute("requirements", requirements);
        model.addAttribute("auditHistory", auditHistory);
        return "evidence/passport";
    }

    @GetMapping("/audit")
    public String showFullAuditLog(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   Model model) {
        Page<AuditEvent> eventPage = auditService.getAuditEvents(PageRequest.of(page, size));
        model.addAttribute("events", eventPage);
        return "audit/list";
    }
}
