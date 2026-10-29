package com.complymind.requirement.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.repository.ComplianceFrameworkRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.requirement.repository.RequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final ComplianceFrameworkRepository frameworkRepository;
    private final AuditService auditService;

    public RequirementService(RequirementRepository requirementRepository,
                              ApplicationRequirementRepository appRequirementRepository,
                              ComplianceFrameworkRepository frameworkRepository,
                              AuditService auditService) {
        this.requirementRepository = requirementRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.frameworkRepository = frameworkRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Requirement> getRequirementsByFramework(Long frameworkId) {
        return requirementRepository.findByFrameworkId(frameworkId);
    }

    @Transactional(readOnly = true)
    public Requirement getRequirementById(Long id) {
        return requirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found with id: " + id));
    }

    @Transactional
    public Requirement createRequirement(Long frameworkId, Requirement req) {
        ComplianceFramework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Framework not found with id: " + frameworkId));
        req.setFramework(framework);
        Requirement saved = requirementRepository.save(req);
        auditService.recordEvent("REQUIREMENT_CREATED", "Requirement", saved.getId().toString(),
                "Created master requirement: " + saved.getCode() + " (" + saved.getTitle() + ")");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ApplicationRequirement> getRequirementsForApplication(Long applicationId) {
        return appRequirementRepository.findByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public ApplicationRequirement getApplicationRequirementById(Long id) {
        return appRequirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application Requirement not found with id: " + id));
    }

    @Transactional
    public ApplicationRequirement addRequirementToApplication(ComplianceApplication application, Requirement requirement, LocalDateTime deadline) {
        appRequirementRepository.findByApplicationIdAndRequirementId(application.getId(), requirement.getId())
                .ifPresent(ar -> {
                    throw new BusinessValidationException("Requirement " + requirement.getCode() + " is already attached to this application");
                });

        ApplicationRequirement ar = new ApplicationRequirement();
        ar.setApplication(application);
        ar.setRequirement(requirement);
        ar.setStatus(RequirementStatus.PENDING);
        ar.setDeadline(deadline != null ? deadline : LocalDateTime.now().plusDays(requirement.getDefaultDeadlineDays()));
        ar.setDemo(application.isDemo());

        ApplicationRequirement saved = appRequirementRepository.save(ar);
        auditService.recordEvent("APP_REQUIREMENT_ADDED", "ApplicationRequirement", saved.getId().toString(),
                "Attached requirement " + requirement.getCode() + " to application " + application.getApplicationNumber());
        return saved;
    }

    @Transactional
    public ApplicationRequirement updateRequirementStatus(Long appReqId, RequirementStatus newStatus, String notes) {
        ApplicationRequirement ar = getApplicationRequirementById(appReqId);
        RequirementStatus oldStatus = ar.getStatus();
        ar.setStatus(newStatus);
        if (notes != null) {
            ar.setComplianceNotes(notes);
        }
        ApplicationRequirement saved = appRequirementRepository.save(ar);
        auditService.recordEvent("REQUIREMENT_STATUS_CHANGE", "ApplicationRequirement", saved.getId().toString(),
                "Status changed from " + oldStatus + " to " + newStatus + " for " + ar.getRequirement().getCode());
        return saved;
    }
}
