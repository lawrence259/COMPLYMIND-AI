package com.complymind.ripple.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.common.dto.ApiResponse;
import com.complymind.ripple.model.DependencyGraph;
import com.complymind.ripple.model.RippleAnalysisResult;
import com.complymind.ripple.service.RippleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Tag(name = "Compliance Ripple Engine", description = "DAG traversal and downstream impact analysis")
public class RippleGraphController {

    private final RippleEngineService rippleEngineService;
    private final ComplianceApplicationService applicationService;

    public RippleGraphController(RippleEngineService rippleEngineService,
                                 ComplianceApplicationService applicationService) {
        this.rippleEngineService = rippleEngineService;
        this.applicationService = applicationService;
    }

    @GetMapping("/applications/{id}/graph")
    public String showGraphPage(@PathVariable Long id, Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        DependencyGraph graph = rippleEngineService.buildGraphForApplication(id);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("graph", graph);
        return "ripple/graph";
    }

    @GetMapping("/api/applications/{id}/graph-data")
    @ResponseBody
    @Operation(summary = "Get Cytoscape graph nodes and edges data for an application")
    public ApiResponse<DependencyGraph> getGraphData(@PathVariable Long id) {
        DependencyGraph graph = rippleEngineService.buildGraphForApplication(id);
        return ApiResponse.ok(graph);
    }

    @PostMapping("/applications/{id}/ripple/recalculate")
    public String recalculateRipple(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            RippleAnalysisResult result = rippleEngineService.recalculateAndPersist(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ripple Engine analysis completed: " + result.getBlockedCount() + " blocked, " +
                            result.getAtRiskCount() + " at-risk, " + result.getSatisfiedCount() + " satisfied.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ripple analysis error: " + e.getMessage());
        }
        return "redirect:/applications/" + id + "/graph";
    }
}
