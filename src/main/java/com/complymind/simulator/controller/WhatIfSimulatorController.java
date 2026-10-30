package com.complymind.simulator.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.service.RequirementService;
import com.complymind.security.CustomUserDetails;
import com.complymind.security.SecurityUtils;
import com.complymind.simulator.model.SimulationResult;
import com.complymind.simulator.model.SimulationScenario;
import com.complymind.simulator.service.WhatIfSimulatorService;
import com.complymind.user.entity.User;
import com.complymind.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WhatIfSimulatorController {

    private final WhatIfSimulatorService simulatorService;
    private final ComplianceApplicationService applicationService;
    private final RequirementService requirementService;
    private final UserService userService;

    public WhatIfSimulatorController(WhatIfSimulatorService simulatorService,
                                     ComplianceApplicationService applicationService,
                                     RequirementService requirementService,
                                     UserService userService) {
        this.simulatorService = simulatorService;
        this.applicationService = applicationService;
        this.requirementService = requirementService;
        this.userService = userService;
    }

    @GetMapping("/applications/{id}/simulator")
    public String showSimulator(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access Denied: insufficient permissions for this institution.");
        }

        List<ApplicationRequirement> requirements = requirementService.getRequirementsForApplication(id);

        SimulationScenario defaultScenario = new SimulationScenario(id);
        SimulationResult initialResult = simulatorService.runSimulation(defaultScenario);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("requirements", requirements);
        model.addAttribute("simulationResult", initialResult);
        model.addAttribute("statuses", RequirementStatus.values());
        model.addAttribute("activeMenu", "applications");

        return "simulator/view";
    }

    @PostMapping("/applications/{id}/simulator/run")
    public String runSimulation(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int deadlineExtensionDays,
                                @RequestParam(defaultValue = "Hypothetical Scenario") String scenarioName,
                                @RequestParam Map<String, String> allParams,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access Denied: insufficient permissions for this institution.");
        }

        List<ApplicationRequirement> requirements = requirementService.getRequirementsForApplication(id);

        SimulationScenario scenario = buildScenario(id, scenarioName, deadlineExtensionDays, allParams, requirements);
        SimulationResult result = simulatorService.runSimulation(scenario);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("requirements", requirements);
        model.addAttribute("simulationResult", result);
        model.addAttribute("scenario", scenario);
        model.addAttribute("statuses", RequirementStatus.values());
        model.addAttribute("activeMenu", "applications");

        return "simulator/view";
    }

    @PostMapping("/applications/{id}/simulator/apply")
    public String applySimulation(@PathVariable Long id,
                                  @RequestParam(defaultValue = "0") int deadlineExtensionDays,
                                  @RequestParam(defaultValue = "Applied Scenario") String scenarioName,
                                  @RequestParam Map<String, String> allParams,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access Denied: insufficient permissions for this institution.");
        }

        List<ApplicationRequirement> requirements = requirementService.getRequirementsForApplication(id);
        SimulationScenario scenario = buildScenario(id, scenarioName, deadlineExtensionDays, allParams, requirements);

        User authorizedUser = userService.getUserById(userDetails.getId());
        simulatorService.applySimulationToLive(id, scenario, authorizedUser);

        redirectAttributes.addFlashAttribute("successMessage",
                "Simulation scenario applied successfully to live application records.");

        return "redirect:/applications/" + id;
    }

    private SimulationScenario buildScenario(Long appId, String scenarioName, int deadlineDays,
                                              Map<String, String> allParams,
                                              List<ApplicationRequirement> requirements) {
        SimulationScenario scenario = new SimulationScenario(appId);
        scenario.setScenarioName(scenarioName);
        scenario.setDeadlineExtensionDays(deadlineDays);

        Map<Long, RequirementStatus> overrides = new HashMap<>();
        for (ApplicationRequirement ar : requirements) {
            String paramVal = allParams.get("override_" + ar.getId());
            if (paramVal != null && !paramVal.isBlank()) {
                try {
                    overrides.put(ar.getId(), RequirementStatus.valueOf(paramVal));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        scenario.setRequirementOverrides(overrides);
        return scenario;
    }
}
