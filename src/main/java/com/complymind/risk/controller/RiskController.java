package com.complymind.risk.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.common.dto.ApiResponse;
import com.complymind.risk.model.RiskBreakdown;
import com.complymind.risk.model.RootCauseGroup;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.risk.service.RootCauseInvestigatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Tag(name = "Risk Engine & Root Cause", description = "Explainable risk metrics and issue grouping")
public class RiskController {

    private final RiskEngineService riskEngineService;
    private final RootCauseInvestigatorService rootCauseInvestigatorService;
    private final ComplianceApplicationService applicationService;
    private final com.complymind.risk.repository.RiskAssessmentRepository riskAssessmentRepository;

    public RiskController(RiskEngineService riskEngineService,
                          RootCauseInvestigatorService rootCauseInvestigatorService,
                          ComplianceApplicationService applicationService,
                          com.complymind.risk.repository.RiskAssessmentRepository riskAssessmentRepository) {
        this.riskEngineService = riskEngineService;
        this.rootCauseInvestigatorService = rootCauseInvestigatorService;
        this.applicationService = applicationService;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    @GetMapping("/applications/{id}/risk")
    public String showRiskPage(@PathVariable Long id, Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        RiskBreakdown breakdown = riskEngineService.calculateRisk(id);
        List<com.complymind.risk.entity.RiskAssessment> history = riskAssessmentRepository.findByApplicationIdOrderByCalculatedAtDesc(id);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("breakdown", breakdown);
        model.addAttribute("history", history);
        return "risk/view";
    }

    @GetMapping("/applications/{id}/root-cause")
    public String showRootCausePage(@PathVariable Long id, Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        List<RootCauseGroup> rootCauses = rootCauseInvestigatorService.investigateRootCauses(id);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("rootCauses", rootCauses);
        return "risk/rootcause";
    }

    @GetMapping("/api/applications/{id}/risk-breakdown")
    @ResponseBody
    @Operation(summary = "Get factor-level risk breakdown for an application")
    public ApiResponse<RiskBreakdown> getRiskBreakdown(@PathVariable Long id) {
        return ApiResponse.ok(riskEngineService.calculateRisk(id));
    }

    @PostMapping("/applications/{id}/risk/recalculate")
    public String recalculateRisk(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            riskEngineService.evaluateAndPersist(id);
            redirectAttributes.addFlashAttribute("successMessage", "Risk assessment recalculated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/applications/" + id + "/risk";
    }
}
