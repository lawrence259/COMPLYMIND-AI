package com.complymind.risk.entity;

import com.complymind.application.entity.ComplianceApplication;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ComplianceApplication application;

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", nullable = false, length = 32)
    private RiskBand riskBand;

    @Column(name = "deadline_urgency_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal deadlineUrgencyScore;

    @Column(name = "blocked_dependency_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal blockedDependencyScore;

    @Column(name = "evidence_deficiency_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal evidenceDeficiencyScore;

    @Column(name = "unresolved_pendency_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal unresolvedPendencyScore;

    @Column(name = "explanation_json", columnDefinition = "TEXT")
    private String explanationJson;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        this.calculatedAt = LocalDateTime.now();
    }

    public RiskAssessment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ComplianceApplication getApplication() {
        return application;
    }

    public void setApplication(ComplianceApplication application) {
        this.application = application;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public RiskBand getRiskBand() {
        return riskBand;
    }

    public void setRiskBand(RiskBand riskBand) {
        this.riskBand = riskBand;
    }

    public BigDecimal getDeadlineUrgencyScore() {
        return deadlineUrgencyScore;
    }

    public void setDeadlineUrgencyScore(BigDecimal deadlineUrgencyScore) {
        this.deadlineUrgencyScore = deadlineUrgencyScore;
    }

    public BigDecimal getBlockedDependencyScore() {
        return blockedDependencyScore;
    }

    public void setBlockedDependencyScore(BigDecimal blockedDependencyScore) {
        this.blockedDependencyScore = blockedDependencyScore;
    }

    public BigDecimal getEvidenceDeficiencyScore() {
        return evidenceDeficiencyScore;
    }

    public void setEvidenceDeficiencyScore(BigDecimal evidenceDeficiencyScore) {
        this.evidenceDeficiencyScore = evidenceDeficiencyScore;
    }

    public BigDecimal getUnresolvedPendencyScore() {
        return unresolvedPendencyScore;
    }

    public void setUnresolvedPendencyScore(BigDecimal unresolvedPendencyScore) {
        this.unresolvedPendencyScore = unresolvedPendencyScore;
    }

    public String getExplanationJson() {
        return explanationJson;
    }

    public void setExplanationJson(String explanationJson) {
        this.explanationJson = explanationJson;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
