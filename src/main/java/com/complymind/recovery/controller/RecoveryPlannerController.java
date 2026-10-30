package com.complymind.recovery.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import com.complymind.recovery.model.RecoveryPlan;
import com.complymind.recovery.service.RecoveryPlannerService;
import com.complymind.security.CustomUserDetails;
import com.complymind.security.SecurityUtils;
import com.complymind.user.entity.User;
import com.complymind.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class RecoveryPlannerController {

    private final RecoveryPlannerService recoveryService;
    private final ComplianceApplicationService applicationService;
    private final UserService userService;

    public RecoveryPlannerController(RecoveryPlannerService recoveryService,
                                     ComplianceApplicationService applicationService,
                                     UserService userService) {
        this.recoveryService = recoveryService;
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @GetMapping("/applications/{id}/recovery-plan")
    public String showRecoveryPlan(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access denied to institution resources.");
        }

        RecoveryPlan plan = recoveryService.generateRecoveryPlan(id);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("plan", plan);
        model.addAttribute("activeMenu", "applications");

        return "recovery/plan";
    }

    @PostMapping("/applications/{id}/recovery-plan/generate-actions")
    public String convertToActions(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access denied to institution resources.");
        }

        User creator = userService.getUserById(userDetails.getId());
        List<CorrectiveAction> actions = recoveryService.convertPlanToCorrectiveActions(id, creator);

        redirectAttributes.addFlashAttribute("successMessage",
                "Recovery Plan successfully converted! " + actions.size() + " Corrective Action tasks dispatched to institutional personnel.");

        return "redirect:/corrective-actions";
    }
}
