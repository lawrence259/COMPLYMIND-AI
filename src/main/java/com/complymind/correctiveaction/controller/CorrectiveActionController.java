package com.complymind.correctiveaction.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.correctiveaction.entity.ActionStatus;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import com.complymind.correctiveaction.entity.Priority;
import com.complymind.correctiveaction.service.CorrectiveActionService;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.service.RequirementService;
import com.complymind.security.SecurityUtils;
import com.complymind.user.entity.User;
import com.complymind.user.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/corrective-actions")
public class CorrectiveActionController {

    private final CorrectiveActionService correctiveActionService;
    private final ComplianceApplicationService applicationService;
    private final RequirementService requirementService;
    private final UserService userService;

    public CorrectiveActionController(CorrectiveActionService correctiveActionService,
                                     ComplianceApplicationService applicationService,
                                     RequirementService requirementService,
                                     UserService userService) {
        this.correctiveActionService = correctiveActionService;
        this.applicationService = applicationService;
        this.requirementService = requirementService;
        this.userService = userService;
    }

    @GetMapping
    public String listActions(Model model) {
        List<CorrectiveAction> actions = correctiveActionService.getAllActions();
        model.addAttribute("actions", actions);
        model.addAttribute("statuses", ActionStatus.values());
        return "correctiveactions/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long applicationId,
                                 @RequestParam(required = false) Long appReqId,
                                 @RequestParam(required = false) String rootIssue,
                                 Model model) {
        List<ComplianceApplication> applications = applicationService.getAllApplications();
        List<User> users = userService.getAllUsers();

        model.addAttribute("applications", applications);
        model.addAttribute("users", users);
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("selectedAppId", applicationId);
        model.addAttribute("selectedAppReqId", appReqId);
        model.addAttribute("defaultRootIssue", rootIssue);

        if (applicationId != null) {
            List<ApplicationRequirement> reqs = requirementService.getRequirementsForApplication(applicationId);
            model.addAttribute("requirements", reqs);
        }

        return "correctiveactions/create";
    }

    @PostMapping("/create")
    public String createAction(@RequestParam Long applicationId,
                               @RequestParam(required = false) Long appReqId,
                               @RequestParam String title,
                               @RequestParam String description,
                               @RequestParam String rootIssue,
                               @RequestParam(required = false) Long assignedToUserId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                               @RequestParam Priority priority,
                               RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime dueDateTime = dueDate.atTime(23, 59, 59);
            CorrectiveAction action = correctiveActionService.createAction(
                    applicationId, appReqId, title, description, rootIssue, assignedToUserId, dueDateTime, priority
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Corrective Action task created successfully: " + action.getTitle());
            return "redirect:/corrective-actions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/corrective-actions/new";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ActionStatus status,
                               @RequestParam(required = false) String resolutionSummary,
                               RedirectAttributes redirectAttributes) {
        try {
            correctiveActionService.updateStatus(id, status, resolutionSummary);
            redirectAttributes.addFlashAttribute("successMessage", "Task status updated to: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/corrective-actions";
    }

    @PostMapping("/{id}/verify")
    public String verifyAction(@PathVariable Long id,
                               @RequestParam boolean verified,
                               @RequestParam(required = false) String comments,
                               RedirectAttributes redirectAttributes) {
        try {
            User verifier = SecurityUtils.getCurrentUserDetails()
                    .map(u -> userService.getUserById(u.getId()))
                    .orElseThrow(() -> new RuntimeException("Verifier authentication required"));

            correctiveActionService.verifyAction(id, verifier, verified, comments);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Task verification completed: " + (verified ? "VERIFIED" : "REJECTED"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/corrective-actions";
    }
}
