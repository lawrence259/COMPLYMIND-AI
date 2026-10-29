package com.complymind.evidence.entity;

import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_decisions")
public class ReviewDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_document_id", nullable = false)
    private EvidenceDocument evidenceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_requirement_id", nullable = false)
    private ApplicationRequirement applicationRequirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    private User reviewer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReviewStatus decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ReviewDecision() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public ReviewStatus getDecision() {
        return decision;
    }

    public void setDecision(ReviewStatus decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
