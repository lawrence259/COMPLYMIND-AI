package com.complymind.discrepancy.entity;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discrepancy_findings")
public class DiscrepancyFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ComplianceApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_document_id")
    private EvidenceDocument evidenceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_requirement_id", nullable = false)
    private ApplicationRequirement applicationRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_type", nullable = false, length = 64)
    private DiscrepancyType findingType;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "declared_value")
    private String declaredValue;

    @Column(name = "extracted_value")
    private String extractedValue;

    @Column(name = "source_citation")
    private String sourceCitation;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "regulatory_reference")
    private String regulatoryReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private FindingSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private FindingReviewStatus reviewStatus = FindingReviewStatus.PENDING_REVIEW;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public DiscrepancyFinding() {
    }

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

    public EvidenceDocument getEvidenceDocument() {
        return evidenceDocument;
    }

    public void setEvidenceDocument(EvidenceDocument evidenceDocument) {
        this.evidenceDocument = evidenceDocument;
    }

    public ApplicationRequirement getApplicationRequirement() {
        return applicationRequirement;
    }

    public void setApplicationRequirement(ApplicationRequirement applicationRequirement) {
        this.applicationRequirement = applicationRequirement;
    }

    public DiscrepancyType getFindingType() {
        return findingType;
    }

    public void setFindingType(DiscrepancyType findingType) {
        this.findingType = findingType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(String declaredValue) {
        this.declaredValue = declaredValue;
    }

    public String getExtractedValue() {
        return extractedValue;
    }

    public void setExtractedValue(String extractedValue) {
        this.extractedValue = extractedValue;
    }

    public String getSourceCitation() {
        return sourceCitation;
    }

    public void setSourceCitation(String sourceCitation) {
        this.sourceCitation = sourceCitation;
    }

    public Integer getSourcePage() {
        return sourcePage;
    }

    public void setSourcePage(Integer sourcePage) {
        this.sourcePage = sourcePage;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getRegulatoryReference() {
        return regulatoryReference;
    }

    public void setRegulatoryReference(String regulatoryReference) {
        this.regulatoryReference = regulatoryReference;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(FindingSeverity severity) {
        this.severity = severity;
    }

    public FindingReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(FindingReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewerNotes() {
        return reviewerNotes;
    }

    public void setReviewerNotes(String reviewerNotes) {
        this.reviewerNotes = reviewerNotes;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
