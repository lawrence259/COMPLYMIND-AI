package com.complymind.application.service;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.repository.ComplianceFrameworkRepository;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.repository.InstitutionRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.requirement.repository.RequirementRepository;
import com.complymind.ripple.service.RippleEngineService;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.security.SecurityUtils;
import com.complymind.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplianceApplicationService {

    private final ComplianceApplicationRepository applicationRepository;
    private final InstitutionRepository institutionRepository;
    private final ComplianceFrameworkRepository frameworkRepository;
    private final RequirementRepository requirementRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final RippleEngineService rippleEngineService;
    private final RiskEngineService riskEngineService;
    private final AuditService auditService;

    public ComplianceApplicationService(ComplianceApplicationRepository applicationRepository,
                                      InstitutionRepository institutionRepository,
                                      ComplianceFrameworkRepository frameworkRepository,
                                      RequirementRepository requirementRepository,
                                      ApplicationRequirementRepository appRequirementRepository,
                                      RippleEngineService rippleEngineService,
                                      RiskEngineService riskEngineService,
                                      AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.institutionRepository = institutionRepository;
        this.frameworkRepository = frameworkRepository;
        this.requirementRepository = requirementRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.rippleEngineService = rippleEngineService;
        this.riskEngineService = riskEngineService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ComplianceApplication> getAllApplications() {
        Long instId = SecurityUtils.getCurrentInstitutionId();
        if (instId != null && !SecurityUtils.isAdmin()) {
            return applicationRepository.findByInstitutionId(instId);
        }
        return applicationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<ComplianceApplication> getApplicationsPaged(Pageable pageable) {
        Long instId = SecurityUtils.getCurrentInstitutionId();
        if (instId != null && !SecurityUtils.isAdmin()) {
            return applicationRepository.findByInstitutionId(instId, pageable);
        }
        return applicationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public ComplianceApplication getApplicationById(Long id) {
        ComplianceApplication app = applicationRepository.findByIdWithDetails(id)
                .or(() -> applicationRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        if (!SecurityUtils.canAccessInstitution(app.getInstitution().getId())) {
            throw new BusinessValidationException("Access Denied: You do not have permission to view records of other institutions.");
        }
        return app;
    }

    @Transactional
    public ComplianceApplication createApplication(Long institutionId, Long frameworkId, String title, String academicYear, User creator) {
        if (institutionId == null) {
            throw new BusinessValidationException("Institution ID is required");
        }
        if (frameworkId == null) {
            throw new BusinessValidationException("Compliance Framework ID is required");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessValidationException("Application title cannot be blank");
        }
        if (academicYear == null || academicYear.trim().isEmpty()) {
            throw new BusinessValidationException("Academic year cannot be blank");
        }

        if (!SecurityUtils.canAccessInstitution(institutionId)) {
            throw new BusinessValidationException("Access Denied: You cannot create applications for other institutions.");
        }

        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with ID: " + institutionId));

        ComplianceFramework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Framework not found with ID: " + frameworkId));

        // Generate guaranteed unique application number
        String appNumber;
        int attempts = 0;
        do {
            long epochSec = System.currentTimeMillis() / 1000;
            int randSuffix = (int) (Math.random() * 9000) + 1000;
            appNumber = "APP-" + institution.getCode() + "-" + epochSec + "-" + randSuffix;
            attempts++;
            if (attempts > 10) {
                appNumber = "APP-" + institution.getCode() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                break;
            }
        } while (applicationRepository.existsByApplicationNumber(appNumber));

        ComplianceApplication app = new ComplianceApplication();
        app.setApplicationNumber(appNumber);
        app.setInstitution(institution);
        app.setFramework(framework);
        app.setTitle(title.trim());
        app.setAcademicYear(academicYear.trim());
        app.setStatus(ApplicationStatus.DRAFT);
        app.setCreatedBy(creator);
        app.setDemo(institution.isDemo());

        ComplianceApplication saved = applicationRepository.save(app);

        // Auto-attach requirements from the framework to this application
        List<Requirement> frameworkReqs = requirementRepository.findByFrameworkId(frameworkId);
        LocalDateTime now = LocalDateTime.now();
        for (Requirement r : frameworkReqs) {
            ApplicationRequirement ar = new ApplicationRequirement();
            ar.setApplication(saved);
            ar.setRequirement(r);
            ar.setDeadline(now.plusDays(r.getDefaultDeadlineDays()));
            ar.setDemo(saved.isDemo());
            appRequirementRepository.save(ar);
        }

        auditService.recordEvent("APPLICATION_CREATED", "ComplianceApplication", saved.getId().toString(),
                "Created application " + saved.getApplicationNumber() + " with " + frameworkReqs.size() + " requirements");

        // Initial risk calculation
        riskEngineService.evaluateAndPersist(saved.getId());

        return saved;
    }

    @Transactional
    public ComplianceApplication transitionStatus(Long id, ApplicationStatus newStatus) {
        ComplianceApplication app = getApplicationById(id);
        ApplicationStatus current = app.getStatus();

        // Validate state transitions
        boolean valid = switch (current) {
            case DRAFT -> newStatus == ApplicationStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == ApplicationStatus.READY_FOR_REVIEW || newStatus == ApplicationStatus.DRAFT;
            case READY_FOR_REVIEW -> newStatus == ApplicationStatus.UNDER_REVIEW || newStatus == ApplicationStatus.ACTION_REQUIRED;
            case UNDER_REVIEW -> newStatus == ApplicationStatus.ACTION_REQUIRED || newStatus == ApplicationStatus.COMPLETED;
            case ACTION_REQUIRED -> newStatus == ApplicationStatus.IN_PROGRESS || newStatus == ApplicationStatus.READY_FOR_REVIEW;
            case COMPLETED -> false; // Final state
        };

        if (!valid && !SecurityUtils.isAdmin()) {
            throw new BusinessValidationException("Invalid status transition from " + current + " to " + newStatus);
        }

        app.setStatus(newStatus);
        if (newStatus == ApplicationStatus.READY_FOR_REVIEW && app.getSubmittedAt() == null) {
            app.setSubmittedAt(LocalDateTime.now());
        }

        ComplianceApplication saved = applicationRepository.save(app);
        auditService.recordEvent("APPLICATION_STATUS_TRANSITION", "ComplianceApplication", saved.getId().toString(),
                "Status moved from " + current + " to " + newStatus);
        return saved;
    }
}
