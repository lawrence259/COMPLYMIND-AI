package com.complymind.ripple.entity;

import com.complymind.requirement.entity.Requirement;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirement_dependencies")
public class RequirementDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_requirement_id", nullable = false)
    private Requirement sourceRequirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_requirement_id", nullable = false)
    private Requirement dependentRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 50)
    private DependencyType dependencyType = DependencyType.HARD_BLOCKER;

    @Column(length = 255)
    private String description;

    @Column(name = "is_demo", nullable = false)
    private boolean isDemo = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public RequirementDependency() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Requirement getSourceRequirement() {
        return sourceRequirement;
    }

    public void setSourceRequirement(Requirement sourceRequirement) {
        this.sourceRequirement = sourceRequirement;
    }

    public Requirement getDependentRequirement() {
        return dependentRequirement;
    }

    public void setDependentRequirement(Requirement dependentRequirement) {
        this.dependentRequirement = dependentRequirement;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDemo() {
        return isDemo;
    }

    public void setDemo(boolean demo) {
        isDemo = demo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
