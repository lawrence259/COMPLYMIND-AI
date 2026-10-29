package com.complymind.risk.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.entity.ReviewStatus;
import com.complymind.evidence.repository.EvidenceDocumentRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.risk.entity.RiskAssessment;
import com.complymind.risk.entity.RiskBand;
import com.complymind.risk.model.RiskBreakdown;
import com.complymind.risk.repository.RiskAssessmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RiskEngineService {

    @Value("${app.risk.weights.deadline-urgency:0.35}")
    private double wDeadlineUrgency;

    @Value("${app.risk.weights.blocked-dependency:0.30}")
    private double wBlockedDependency;

    @Value("${app.risk.weights.evidence-deficiency:0.20}")
    private double wEvidenceDeficiency;

    @Value("${app.risk.weights.unresolved-pendency:0.15}")
    private double wUnresolvedPendency;

    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final EvidenceDocumentRepository evidenceRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RiskEngineService(ComplianceApplicationRepository applicationRepository,
                             ApplicationRequirementRepository appRequirementRepository,
                             EvidenceDocumentRepository evidenceRepository,
                             RiskAssessmentRepository riskAssessmentRepository,
                             AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.evidenceRepository = evidenceRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public RiskBreakdown calculateRisk(Long applicationId) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        List<ApplicationRequirement> requirements = appRequirementRepository.findByApplicationId(applicationId);
        List<EvidenceDocument> documents = evidenceRepository.findByApplicationId(applicationId);

        RiskBreakdown breakdown = new RiskBreakdown();
        if (requirements.isEmpty()) {
            breakdown.setOverallScore(0.0);
            breakdown.setRiskBand(RiskBand.LOW);
            breakdown.getContributingFactors().add("No requirements currently attached to this application");
            return breakdown;
        }

        LocalDateTime now = LocalDateTime.now();
        int totalReqs = requirements.size();

        // 1. Deadline Urgency Factor (0 - 100)
        double totalUrgency = 0.0;
        int urgencyCount = 0;
        int overdueCount = 0;
        int impendingCount = 0;

        for (ApplicationRequirement ar : requirements) {
            if (ar.getStatus() != RequirementStatus.SATISFIED) {
                urgencyCount++;
                if (ar.getDeadline() == null) {
                    totalUrgency += 20.0;
                } else if (ar.getDeadline().isBefore(now)) {
                    totalUrgency += 100.0;
                    overdueCount++;
                } else {
                    long days = Duration.between(now, ar.getDeadline()).toDays();
                    if (days <= 2) {
                        totalUrgency += 95.0;
                        impendingCount++;
                    } else if (days <= 7) {
                        totalUrgency += 75.0;
                        impendingCount++;
                    } else if (days <= 14) {
                        totalUrgency += 50.0;
                    } else if (days <= 30) {
                        totalUrgency += 25.0;
                    } else {
                        totalUrgency += 10.0;
                    }
                }
            }
        }
        double deadlineUrgencyScore = urgencyCount > 0 ? (totalUrgency / urgencyCount) : 0.0;

        // 2. Blocked Dependency Severity (0 - 100)
        long blockedCount = requirements.stream().filter(r -> r.getStatus() == RequirementStatus.BLOCKED).count();
        long rejectedCount = requirements.stream().filter(r -> r.getStatus() == RequirementStatus.REJECTED).count();
        long atRiskCount = requirements.stream().filter(r -> r.getStatus() == RequirementStatus.AT_RISK).count();

        double blockedRatio = ((double) (blockedCount + rejectedCount) / totalReqs);
        double blockedDependencyScore = Math.min(100.0, (blockedRatio * 100.0) + (atRiskCount * 5.0));

        // 3. Evidence Deficiency (0 - 100)
        int deficientEvidenceCount = 0;
        for (ApplicationRequirement ar : requirements) {
            if (ar.getStatus() != RequirementStatus.SATISFIED) {
                List<EvidenceDocument> evs = evidenceRepository.findByApplicationRequirementId(ar.getId());
                boolean hasApproved = evs.stream().anyMatch(e -> e.getReviewStatus() == ReviewStatus.APPROVED);
                if (!hasApproved) {
                    deficientEvidenceCount++;
                }
            }
        }
        double evidenceDeficiencyScore = ((double) deficientEvidenceCount / totalReqs) * 100.0;

        // 4. Unresolved Pendency (0 - 100)
        long satisfiedCount = requirements.stream().filter(r -> r.getStatus() == RequirementStatus.SATISFIED).count();
        double unresolvedPendencyScore = ((double) (totalReqs - satisfiedCount) / totalReqs) * 100.0;

        // Weighted Overall Risk Formula
        double rawRisk = (wDeadlineUrgency * deadlineUrgencyScore)
                + (wBlockedDependency * blockedDependencyScore)
                + (wEvidenceDeficiency * evidenceDeficiencyScore)
                + (wUnresolvedPendency * unresolvedPendencyScore);

        // Normalize strictly within 0.0 to 100.0
        double overallScore = Math.max(0.0, Math.min(100.0, rawRisk));

        // Determine Risk Band
        RiskBand band;
        if (overallScore <= 29.0) {
            band = RiskBand.LOW;
        } else if (overallScore <= 59.0) {
            band = RiskBand.MEDIUM;
        } else if (overallScore <= 79.0) {
            band = RiskBand.HIGH;
        } else {
            band = RiskBand.CRITICAL;
        }

        breakdown.setOverallScore(Math.round(overallScore * 100.0) / 100.0);
        breakdown.setRiskBand(band);
        breakdown.setDeadlineUrgencyScore(Math.round(deadlineUrgencyScore * 100.0) / 100.0);
        breakdown.setBlockedDependencyScore(Math.round(blockedDependencyScore * 100.0) / 100.0);
        breakdown.setEvidenceDeficiencyScore(Math.round(evidenceDeficiencyScore * 100.0) / 100.0);
        breakdown.setUnresolvedPendencyScore(Math.round(unresolvedPendencyScore * 100.0) / 100.0);

        // Construct explainable factors
        if (overdueCount > 0) {
            breakdown.getContributingFactors().add(overdueCount + " requirement(s) have passed their deadline without resolution.");
        }
        if (impendingCount > 0) {
            breakdown.getContributingFactors().add(impendingCount + " requirement(s) are due within 7 days.");
        }
        if (blockedCount > 0) {
            breakdown.getContributingFactors().add(blockedCount + " downstream requirement(s) are actively BLOCKED by prerequisite failures.");
        }
        if (rejectedCount > 0) {
            breakdown.getContributingFactors().add(rejectedCount + " requirement(s) have REJECTED evidence requiring re-submission.");
        }
        if (deficientEvidenceCount > 0) {
            breakdown.getContributingFactors().add(deficientEvidenceCount + " requirement(s) lack approved supporting evidence documents.");
        }
        if (breakdown.getContributingFactors().isEmpty()) {
            breakdown.getContributingFactors().add("All attached requirements have satisfactory progress and are on schedule.");
        }

        return breakdown;
    }

    @Transactional
    public RiskAssessment evaluateAndPersist(Long applicationId) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        RiskBreakdown breakdown = calculateRisk(applicationId);

        RiskAssessment assessment = new RiskAssessment();
        assessment.setApplication(app);
        assessment.setOverallScore(BigDecimal.valueOf(breakdown.getOverallScore()));
        assessment.setRiskBand(breakdown.getRiskBand());
        assessment.setDeadlineUrgencyScore(BigDecimal.valueOf(breakdown.getDeadlineUrgencyScore()));
        assessment.setBlockedDependencyScore(BigDecimal.valueOf(breakdown.getBlockedDependencyScore()));
        assessment.setEvidenceDeficiencyScore(BigDecimal.valueOf(breakdown.getEvidenceDeficiencyScore()));
        assessment.setUnresolvedPendencyScore(BigDecimal.valueOf(breakdown.getUnresolvedPendencyScore()));

        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("weights", Map.of(
                    "deadlineUrgency", wDeadlineUrgency,
                    "blockedDependency", wBlockedDependency,
                    "evidenceDeficiency", wEvidenceDeficiency,
                    "unresolvedPendency", wUnresolvedPendency
            ));
            jsonMap.put("factors", breakdown.getContributingFactors());
            jsonMap.put("disclaimer", breakdown.getHeuristicDisclaimer());
            assessment.setExplanationJson(objectMapper.writeValueAsString(jsonMap));
        } catch (Exception e) {
            assessment.setExplanationJson("{\"factors\":[]}");
        }

        RiskAssessment saved = riskAssessmentRepository.save(assessment);

        // Update application cached risk fields
        app.setRiskScore(assessment.getOverallScore());
        app.setRiskBand(assessment.getRiskBand());
        applicationRepository.save(app);

        auditService.recordEvent("RISK_ASSESSED", "ComplianceApplication", applicationId.toString(),
                "Risk recalculated: " + saved.getOverallScore() + " (" + saved.getRiskBand() + ")");

        return saved;
    }
}
