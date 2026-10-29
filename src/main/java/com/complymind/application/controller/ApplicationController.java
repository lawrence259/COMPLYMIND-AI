package com.complymind.application.controller;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.service.EvidenceReviewService;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.service.ComplianceRuleService;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.service.InstitutionService;
import com.complymind.notification.entity.NotificationType;
import com.complymind.notification.service.NotificationService;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.service.RequirementService;
import com.complymind.security.SecurityUtils;
import com.complymind.user.entity.User;
import com.complymind.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final ComplianceApplicationService applicationService;
    private final InstitutionService institutionService;
    private final ComplianceRuleService ruleService;
    private final RequirementService requirementService;
    private final EvidenceReviewService evidenceService;
    private final UserService userService;
    private final NotificationService notificationService;

    public ApplicationController(ComplianceApplicationService applicationService,
                                 InstitutionService institutionService,
                                 ComplianceRuleService ruleService,
                                 RequirementService requirementService,
                                 EvidenceReviewService evidenceService,
                                 UserService userService,
                                 NotificationService notificationService) {
        this.applicationService = applicationService;
        this.institutionService = institutionService;
        this.ruleService = ruleService;
        this.requirementService = requirementService;
        this.evidenceService = evidenceService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listApplications(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        Page<ComplianceApplication> appPage = applicationService.getApplicationsPaged(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        model.addAttribute("applications", appPage);
        return "applications/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        List<Institution> institutions = institutionService.getAllInstitutions();
        List<ComplianceFramework> frameworks = ruleService.getActiveFrameworks();
        model.addAttribute("institutions", institutions);
        model.addAttribute("frameworks", frameworks);
        return "applications/create";
    }

    @PostMapping("/create")
    public String createApplication(@RequestParam Long institutionId,
                                    @RequestParam Long frameworkId,
                                    @RequestParam String title,
                                    @RequestParam String academicYear,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = SecurityUtils.getCurrentUserDetails()
                    .map(u -> userService.getUserById(u.getId()))
                    .orElse(null);

            ComplianceApplication app = applicationService.createApplication(institutionId, frameworkId, title, academicYear, user);

            // Notify creator about successful application creation
            if (user != null) {
                notificationService.createNotification(
                        user, app.getInstitution(),
                        "Application Created: " + app.getApplicationNumber(),
                        "Compliance application '" + app.getTitle() + "' has been created with " +
                                "status DRAFT. Framework: " + app.getFramework().getCode(),
                        NotificationType.APPLICATION_STATUS_CHANGE,
                        "/applications/" + app.getId()
                );
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Application " + app.getApplicationNumber() + " created successfully!");
            return "redirect:/applications/" + app.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/applications/new";
        }
    }

    @GetMapping("/{id}")
    public String viewApplication(@PathVariable Long id, Model model) {
        ComplianceApplication app = applicationService.getApplicationById(id);
        List<ApplicationRequirement> requirements = requirementService.getRequirementsForApplication(id);
        List<EvidenceDocument> evidenceList = evidenceService.getEvidenceForApplication(id);

        model.addAttribute("application", app);
        model.addAttribute("app", app);
        model.addAttribute("requirements", requirements);
        model.addAttribute("evidenceList", evidenceList);
        model.addAttribute("statuses", ApplicationStatus.values());
        return "applications/view";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ApplicationStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            ComplianceApplication app = applicationService.transitionStatus(id, status);

            // Trigger in-app notification for status change
            User currentUser = SecurityUtils.getCurrentUserDetails()
                    .map(u -> userService.getUserById(u.getId()))
                    .orElse(null);
            if (currentUser != null) {
                notificationService.createNotification(
                        currentUser, app.getInstitution(),
                        "Status Updated: " + app.getApplicationNumber(),
                        "Application '" + app.getTitle() + "' status changed to " + status.name() + ".",
                        NotificationType.APPLICATION_STATUS_CHANGE,
                        "/applications/" + app.getId()
                );
            }

            redirectAttributes.addFlashAttribute("successMessage", "Application status updated to: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }
}
