package com.complymind.correctiveaction.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.correctiveaction.entity.ActionStatus;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import com.complymind.correctiveaction.entity.Priority;
import com.complymind.correctiveaction.repository.CorrectiveActionRepository;
import com.complymind.notification.entity.NotificationType;
import com.complymind.notification.service.NotificationService;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.ripple.service.RippleEngineService;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.user.entity.User;
import com.complymind.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CorrectiveActionService {

    private final CorrectiveActionRepository correctiveActionRepository;
    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final RiskEngineService riskEngineService;
    private final RippleEngineService rippleEngineService;

    public CorrectiveActionService(CorrectiveActionRepository correctiveActionRepository,
                                   ComplianceApplicationRepository applicationRepository,
                                   ApplicationRequirementRepository appRequirementRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService,
                                   AuditService auditService,
                                   RiskEngineService riskEngineService,
                                   RippleEngineService rippleEngineService) {
        this.correctiveActionRepository = correctiveActionRepository;
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.riskEngineService = riskEngineService;
        this.rippleEngineService = rippleEngineService;
    }

    @Transactional(readOnly = true)
    public List<CorrectiveAction> getAllActions() {
        return correctiveActionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CorrectiveAction> getActionsForApplication(Long applicationId) {
        return correctiveActionRepository.findByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public CorrectiveAction getActionById(Long id) {
        return correctiveActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corrective action not found: " + id));
    }

    @Transactional
    public CorrectiveAction createAction(Long applicationId, Long appReqId, String title, String description,
                                         String rootIssue, Long assignedToUserId, LocalDateTime dueDate, Priority priority) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        CorrectiveAction action = new CorrectiveAction();
        action.setApplication(app);
        action.setTitle(title);
        action.setDescription(description);
        action.setRootIssue(rootIssue);
        action.setDueDate(dueDate);
        action.setPriority(priority != null ? priority : Priority.MEDIUM);
        action.setStatus(ActionStatus.OPEN);
        action.setDemo(app.isDemo());

        if (appReqId != null) {
            ApplicationRequirement ar = appRequirementRepository.findById(appReqId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application requirement not found: " + appReqId));
            action.setApplicationRequirement(ar);
        }

        if (assignedToUserId != null) {
            User assignee = userRepository.findById(assignedToUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assignedToUserId));
            action.setAssignedTo(assignee);

            notificationService.createNotification(
                    assignee,
                    app.getInstitution(),
                    "New Corrective Action Assigned",
                    "You have been assigned action: " + title + " (Due: " + dueDate.toLocalDate() + ")",
                    NotificationType.TASK_ASSIGNED,
                    "/corrective-actions"
            );
        }

        CorrectiveAction saved = correctiveActionRepository.save(action);
        auditService.recordEvent("ACTION_CREATED", "CorrectiveAction", saved.getId().toString(),
                "Created recovery task: " + title + " linked to root: " + rootIssue);

        // Synchronize and recompute Risk Engine
        riskEngineService.evaluateAndPersist(app.getId());

        return saved;
    }

    @Transactional
    public CorrectiveAction updateStatus(Long actionId, ActionStatus newStatus, String resolutionSummary) {
        CorrectiveAction action = getActionById(actionId);
        ActionStatus oldStatus = action.getStatus();
        action.setStatus(newStatus);
        if (resolutionSummary != null) {
            action.setResolutionSummary(resolutionSummary);
        }

        CorrectiveAction saved = correctiveActionRepository.save(action);
        auditService.recordEvent("ACTION_STATUS_CHANGE", "CorrectiveAction", saved.getId().toString(),
                "Status changed from " + oldStatus + " to " + newStatus);

        // Recalculate risk on action progress
        riskEngineService.evaluateAndPersist(saved.getApplication().getId());

        return saved;
    }

    @Transactional
    public CorrectiveAction verifyAction(Long actionId, User verifier, boolean verified, String comments) {
        CorrectiveAction action = getActionById(actionId);
        action.setVerifiedBy(verifier);
        action.setVerifiedAt(LocalDateTime.now());
        if (verified) {
            action.setStatus(ActionStatus.VERIFIED);
        } else {
            action.setStatus(ActionStatus.REJECTED);
        }
        if (comments != null) {
            action.setResolutionSummary(action.getResolutionSummary() != null ?
                    action.getResolutionSummary() + " | Verifier Note: " + comments : comments);
        }

        CorrectiveAction saved = correctiveActionRepository.save(action);
        auditService.recordEvent("ACTION_VERIFIED", "CorrectiveAction", saved.getId().toString(),
                "Corrective action verification: " + (verified ? "VERIFIED" : "REJECTED"));

        // Recalculate both ripple propagation and risk assessment!
        rippleEngineService.recalculateAndPersist(saved.getApplication().getId());
        riskEngineService.evaluateAndPersist(saved.getApplication().getId());

        return saved;
    }

    @Scheduled(cron = "0 0 * * * *") // Check hourly for overdue actions
    @Transactional
    public void checkOverdueActions() {
        LocalDateTime now = LocalDateTime.now();
        List<CorrectiveAction> dueActions = correctiveActionRepository.findActionsDueBefore(now,
                List.of(ActionStatus.VERIFIED, ActionStatus.OVERDUE));
        for (CorrectiveAction action : dueActions) {
            action.setStatus(ActionStatus.OVERDUE);
            correctiveActionRepository.save(action);

            if (action.getAssignedTo() != null) {
                notificationService.createNotification(
                        action.getAssignedTo(),
                        action.getApplication().getInstitution(),
                        "Corrective Action OVERDUE",
                        "Task '" + action.getTitle() + "' is now OVERDUE! Immediate action required.",
                        NotificationType.ESCALATION,
                        "/corrective-actions"
                );
            }
        }
    }
}
