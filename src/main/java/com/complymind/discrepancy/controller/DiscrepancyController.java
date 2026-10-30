package com.complymind.discrepancy.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.discrepancy.entity.DiscrepancyFinding;
import com.complymind.discrepancy.entity.FindingReviewStatus;
import com.complymind.discrepancy.service.DiscrepancyDetectionService;
import com.complymind.security.CustomUserDetails;
import com.complymind.security.SecurityUtils;
import com.complymind.user.entity.User;
import com.complymind.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DiscrepancyController {

    private final DiscrepancyDetectionService discrepancyService;
    private final ComplianceApplicationService applicationService;
    private final UserService userService;

    public DiscrepancyController(DiscrepancyDetectionService discrepancyService,
                                 ComplianceApplicationService applicationService,
                                 UserService userService) {
        this.discrepancyService = discrepancyService;
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @GetMapping("/applications/{id}/discrepancies")
    public String showDiscrepancies(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access Denied: insufficient permissions for this institution.");
        }

        List<DiscrepancyFinding> findings = discrepancyService.getFindingsForApplication(id);
        if (findings.isEmpty()) {
            findings = discrepancyService.scanApplication(id);
        }

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("findings", findings);
        model.addAttribute("activeMenu", "applications");

        return "evidence/discrepancies";
    }

    @PostMapping("/applications/{id}/discrepancies/scan")
    public String rescanDiscrepancies(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access Denied: insufficient permissions for this institution.");
        }

        List<DiscrepancyFinding> findings = discrepancyService.scanApplication(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Automated discrepancy scan completed. " + findings.size() + " findings identified.");

        return "redirect:/applications/" + id + "/discrepancies";
    }

    @PostMapping("/discrepancies/{id}/review")
    public String reviewFinding(@PathVariable Long id,
                                @RequestParam FindingReviewStatus decision,
                                @RequestParam(defaultValue = "") String notes,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User reviewer = userService.getUserById(userDetails.getId());
        DiscrepancyFinding updated = discrepancyService.reviewFinding(id, decision, notes, reviewer);
        redirectAttributes.addFlashAttribute("successMessage",
                "Finding updated to: " + decision);

        return "redirect:/applications/" + updated.getApplication().getId() + "/discrepancies";
    }
}
