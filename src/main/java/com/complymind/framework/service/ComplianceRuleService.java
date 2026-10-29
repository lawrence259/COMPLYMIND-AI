package com.complymind.framework.service;

import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.entity.ComplianceRule;
import com.complymind.framework.repository.ComplianceFrameworkRepository;
import com.complymind.framework.repository.ComplianceRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComplianceRuleService {

    private final ComplianceFrameworkRepository frameworkRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final AuditService auditService;

    public ComplianceRuleService(ComplianceFrameworkRepository frameworkRepository,
                                 ComplianceRuleRepository ruleRepository,
                                 AuditService auditService) {
        this.frameworkRepository = frameworkRepository;
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ComplianceFramework> getAllFrameworks() {
        return frameworkRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ComplianceFramework> getActiveFrameworks() {
        return frameworkRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public ComplianceFramework getFrameworkById(Long id) {
        return frameworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Framework not found with id: " + id));
    }

    @Transactional
    public ComplianceFramework createFramework(ComplianceFramework framework) {
        ComplianceFramework saved = frameworkRepository.save(framework);
        auditService.recordEvent("FRAMEWORK_CREATED", "ComplianceFramework", saved.getId().toString(),
                "Created framework: " + saved.getName() + " (" + saved.getCode() + ")");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ComplianceRule> getRulesByFramework(Long frameworkId) {
        return ruleRepository.findByFrameworkId(frameworkId);
    }

    @Transactional(readOnly = true)
    public List<ComplianceRule> getAllRules() {
        return ruleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ComplianceRule getRuleById(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with id: " + id));
    }

    @Transactional
    public ComplianceRule createRule(Long frameworkId, ComplianceRule rule) {
        ComplianceFramework framework = getFrameworkById(frameworkId);
        rule.setFramework(framework);
        ComplianceRule saved = ruleRepository.save(rule);
        auditService.recordEvent("RULE_CREATED", "ComplianceRule", saved.getId().toString(),
                "Created rule: " + saved.getRuleCode() + " under framework: " + framework.getCode());
        return saved;
    }

    @Transactional
    public ComplianceRule updateRule(Long id, ComplianceRule updated) {
        ComplianceRule existing = getRuleById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setSourceSection(updated.getSourceSection());
        existing.setSourceUrl(updated.getSourceUrl());
        existing.setApplicability(updated.getApplicability());
        existing.setRequiredEvidenceType(updated.getRequiredEvidenceType());
        existing.setValidationMethod(updated.getValidationMethod());
        existing.setVersion(updated.getVersion());
        existing.setVerified(updated.isVerified());
        existing.setActive(updated.isActive());
        existing.setReviewerNotes(updated.getReviewerNotes());

        ComplianceRule saved = ruleRepository.save(existing);
        auditService.recordEvent("RULE_UPDATED", "ComplianceRule", saved.getId().toString(),
                "Updated rule: " + saved.getRuleCode() + " v" + saved.getVersion());
        return saved;
    }
}
