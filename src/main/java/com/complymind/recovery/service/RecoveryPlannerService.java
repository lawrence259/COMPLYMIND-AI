package com.complymind.recovery.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.correctiveaction.entity.ActionStatus;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import com.complymind.correctiveaction.entity.Priority;
import com.complymind.correctiveaction.repository.CorrectiveActionRepository;
import com.complymind.discrepancy.entity.DiscrepancyFinding;
import com.complymind.discrepancy.entity.FindingReviewStatus;
import com.complymind.discrepancy.service.DiscrepancyDetectionService;
import com.complymind.notification.entity.NotificationType;
import com.complymind.notification.service.NotificationService;
import com.complymind.recovery.model.RecoveryPlan;
import com.complymind.recovery.model.RecoveryTask;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.risk.model.RiskBreakdown;
import com.complymind.risk.model.RootCauseGroup;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.risk.service.RootCauseInvestigatorService;
import com.complymind.user.entity.User;
import com.complymind.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecoveryPlannerService {

    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final RootCauseInvestigatorService rootCauseService;
    private final DiscrepancyDetectionService discrepancyService;
    private final RiskEngineService riskEngineService;
    private final CorrectiveActionRepository correctiveActionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public RecoveryPlannerService(ComplianceApplicationRepository applicationRepository,
                                  ApplicationRequirementRepository appRequirementRepository,
                                  RootCauseInvestigatorService rootCauseService,
                                  DiscrepancyDetectionService discrepancyService,
                                  RiskEngineService riskEngineService,
                                  CorrectiveActionRepository correctiveActionRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService,
                                  AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.rootCauseService = rootCauseService;
        this.discrepancyService = discrepancyService;
        this.riskEngineService = riskEngineService;
        this.correctiveActionRepository = correctiveActionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public RecoveryPlan generateRecoveryPlan(Long applicationId) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        RiskBreakdown risk = riskEngineService.calculateRisk(applicationId);
        List<RootCauseGroup> rootCauses = rootCauseService.investigateRootCauses(applicationId);
        List<DiscrepancyFinding> discrepancies = discrepancyService.getFindingsForApplication(applicationId);
        List<ApplicationRequirement> appReqs = appRequirementRepository.findByApplicationId(applicationId);

        Map<String, ApplicationRequirement> codeToReq = new HashMap<>();
        for (ApplicationRequirement ar : appReqs) {
            codeToReq.put(ar.getRequirement().getCode(), ar);
        }

        RecoveryPlan plan = new RecoveryPlan();
        plan.setApplicationId(applicationId);
        plan.setApplicationNumber(app.getApplicationNumber());
        plan.setCurrentRiskScore(risk.getOverallScore());

        double accumulatedRiskReduction = 0.0;
        int maxDays = 0;
        int taskCounter = 1;

        // --- Milestone 1: Critical Prerequisite Clearance (P1 - Root Blockers) ---
        for (RootCauseGroup rc : rootCauses) {
            ApplicationRequirement ar = codeToReq.get(rc.getRootRequirementCode());
            RecoveryTask task = new RecoveryTask();
            task.setTaskId("REC-P1-" + (taskCounter++));
            task.setRequirementCode(rc.getRootRequirementCode());
            if (ar != null) task.setRequirementId(ar.getId());
            task.setTitle("Clear Root Blocker: " + rc.getRootRequirementCode() + " (" + rc.getRootRequirementTitle() + ")");
            task.setDescription("Remediate primary blocking issue on " + rc.getRootRequirementCode() +
                    ". Upload compliant regulatory proof to liberate " + rc.getAffectedRequirementsCount() + " downstream items.");
            task.setPriority("P1_CRITICAL");
            task.setEstimatedTurnaround("3 Days");
            task.setEstimatedDays(3);
            maxDays = Math.max(maxDays, 3);
            double reduction = Math.min(30.0, 15.0 + (rc.getAffectedRequirementsCount() * 3.5));
            task.setProjectedRiskReduction(Math.round(reduction * 10.0) / 10.0);
            accumulatedRiskReduction += reduction;
            task.setRecommendedRole("INSTITUTION_ADMIN");
            task.setRegulatoryCitation("AICTE APH 2026-27 Prerequisite Mandates");
            plan.getMilestone1Immediate().add(task);
        }

        // --- Milestone 2: Evidentiary Remediation & Discrepancies (P2 - Urgent) ---
        for (DiscrepancyFinding df : discrepancies) {
            if (df.getReviewStatus() != FindingReviewStatus.DISMISSED) {
                RecoveryTask task = new RecoveryTask();
                task.setTaskId("REC-P2-" + (taskCounter++));
                task.setRequirementCode(df.getApplicationRequirement().getRequirement().getCode());
                task.setRequirementId(df.getApplicationRequirement().getId());
                task.setTitle("Resolve Discrepancy on " + df.getFieldName());
                task.setDescription("Field '" + df.getFieldName() + "' declared as '" + df.getDeclaredValue() +
                        "' but evidence reveals '" + df.getExtractedValue() + "'. Rectify submission dossier.");
                task.setPriority(df.getSeverity().name().equals("CRITICAL") ? "P1_CRITICAL" : "P2_HIGH");
                task.setEstimatedTurnaround("7 Days");
                task.setEstimatedDays(7);
                maxDays = Math.max(maxDays, 7);
                double reduction = 8.5;
                task.setProjectedRiskReduction(reduction);
                accumulatedRiskReduction += reduction;
                task.setRecommendedRole("COMPLIANCE_OFFICER");
                task.setRegulatoryCitation(df.getRegulatoryReference() != null ? df.getRegulatoryReference() : "AICTE APH 2026-27");
                plan.getMilestone2Urgent().add(task);
            }
        }

        // Add any remaining unapproved mandatory requirements without root cause
        for (ApplicationRequirement ar : appReqs) {
            Requirement req = ar.getRequirement();
            if (req.isMandatory() && ar.getStatus() == RequirementStatus.PENDING) {
                boolean alreadyInM1 = plan.getMilestone1Immediate().stream().anyMatch(t -> t.getRequirementCode().equals(req.getCode()));
                boolean alreadyInM2 = plan.getMilestone2Urgent().stream().anyMatch(t -> t.getRequirementCode().equals(req.getCode()));
                if (!alreadyInM1 && !alreadyInM2) {
                    RecoveryTask task = new RecoveryTask();
                    task.setTaskId("REC-P2-" + (taskCounter++));
                    task.setRequirementCode(req.getCode());
                    task.setRequirementId(ar.getId());
                    task.setTitle("Submit Verification Portfolio: " + req.getCode());
                    task.setDescription("Upload verified supporting documentation for " + req.getTitle());
                    task.setPriority("P2_HIGH");
                    task.setEstimatedTurnaround("7 Days");
                    task.setEstimatedDays(7);
                    maxDays = Math.max(maxDays, 7);
                    task.setProjectedRiskReduction(6.0);
                    accumulatedRiskReduction += 6.0;
                    task.setRecommendedRole("INSTITUTION_STAFF");
                    task.setRegulatoryCitation("AICTE APH 2026-27 General Portfolio");
                    plan.getMilestone2Urgent().add(task);
                }
            }
        }

        // --- Milestone 3: Verification & Final Closure (P3 - Audit) ---
        RecoveryTask closureTask = new RecoveryTask();
        closureTask.setTaskId("REC-P3-" + (taskCounter++));
        closureTask.setRequirementCode("ALL");
        closureTask.setTitle("Pre-Submission Internal Compliance Verification Sign-Off");
        closureTask.setDescription("Conduct final verification audit, ensure zero discrepancies, and seal Evidence Passport.");
        closureTask.setPriority("P3_MEDIUM");
        closureTask.setEstimatedTurnaround("14 Days");
        closureTask.setEstimatedDays(14);
        maxDays = Math.max(maxDays, 14);
        closureTask.setProjectedRiskReduction(5.0);
        accumulatedRiskReduction += 5.0;
        closureTask.setRecommendedRole("COMPLIANCE_OFFICER");
        closureTask.setRegulatoryCitation("AICTE Approval Process Handbook 2026-27 Final Inspection Checklist");
        plan.getMilestone3Verification().add(closureTask);

        double totalReduction = Math.min(risk.getOverallScore(), Math.round(accumulatedRiskReduction * 10.0) / 10.0);
        plan.setTotalProjectedRiskReduction(totalReduction);
        double postRisk = Math.max(0.0, Math.round((risk.getOverallScore() - totalReduction) * 10.0) / 10.0);
        plan.setProjectedPostRecoveryRiskScore(postRisk);
        plan.setTotalRemediationDays(maxDays);

        return plan;
    }

    @Transactional
    public List<CorrectiveAction> convertPlanToCorrectiveActions(Long applicationId, User creator) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        RecoveryPlan plan = generateRecoveryPlan(applicationId);
        List<CorrectiveAction> createdActions = new ArrayList<>();

        // Find fallback assignee in institution
        List<User> usersInInst = userRepository.findByInstitutionId(app.getInstitution().getId());
        User assignee = usersInInst.isEmpty() ? creator : usersInInst.get(0);

        LocalDateTime now = LocalDateTime.now();

        for (RecoveryTask task : plan.getAllTasks()) {
            if ("ALL".equals(task.getRequirementCode())) continue;

            // Check if action with same title already exists
            List<CorrectiveAction> existing = correctiveActionRepository.findByApplicationId(applicationId);
            boolean exists = existing.stream().anyMatch(ca -> ca.getTitle().equalsIgnoreCase(task.getTitle()) &&
                    ca.getStatus() != ActionStatus.VERIFIED);
            if (exists) continue;

            CorrectiveAction ca = new CorrectiveAction();
            ca.setApplication(app);
            if (task.getRequirementId() != null) {
                appRequirementRepository.findById(task.getRequirementId()).ifPresent(ca::setApplicationRequirement);
            }
            ca.setTitle(task.getTitle());
            ca.setDescription(task.getDescription() + " (Target Regulatory Citation: " + task.getRegulatoryCitation() + ")");
            ca.setRootIssue(task.getRequirementCode() + " remediation path");
            ca.setAssignedTo(assignee);
            ca.setDueDate(now.plusDays(task.getEstimatedDays()));
            ca.setPriority("P1_CRITICAL".equals(task.getPriority()) ? Priority.CRITICAL :
                    ("P2_HIGH".equals(task.getPriority()) ? Priority.HIGH : Priority.MEDIUM));
            ca.setStatus(ActionStatus.OPEN);
            ca.setDemo(app.isDemo());

            CorrectiveAction saved = correctiveActionRepository.save(ca);
            createdActions.add(saved);

            notificationService.createNotification(
                    assignee,
                    app.getInstitution(),
                    "New Recovery Task: " + saved.getTitle(),
                    "Assigned high-priority recovery task due on " + saved.getDueDate().toLocalDate() + ".",
                    NotificationType.TASK_ASSIGNED,
                    "/corrective-actions"
            );
        }

        auditService.recordEvent("RECOVERY_PLAN_CONVERTED", "ComplianceApplication", applicationId.toString(),
                "Generated " + createdActions.size() + " formal corrective actions from Recovery Roadmap by " + creator.getUsername());

        return createdActions;
    }
}
