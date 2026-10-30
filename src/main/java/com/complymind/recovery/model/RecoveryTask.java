package com.complymind.recovery.model;

public class RecoveryTask {

    private String taskId;
    private String title;
    private String description;
    private String requirementCode;
    private Long requirementId;
    private String priority; // P1_CRITICAL, P2_HIGH, P3_MEDIUM
    private String estimatedTurnaround; // e.g. "3 Days", "7 Days"
    private int estimatedDays;
    private double projectedRiskReduction;
    private String recommendedRole;
    private String regulatoryCitation;

    public RecoveryTask() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirementCode() {
        return requirementCode;
    }

    public void setRequirementCode(String requirementCode) {
        this.requirementCode = requirementCode;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getEstimatedTurnaround() {
        return estimatedTurnaround;
    }

    public void setEstimatedTurnaround(String estimatedTurnaround) {
        this.estimatedTurnaround = estimatedTurnaround;
    }

    public int getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(int estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public double getProjectedRiskReduction() {
        return projectedRiskReduction;
    }

    public void setProjectedRiskReduction(double projectedRiskReduction) {
        this.projectedRiskReduction = projectedRiskReduction;
    }

    public String getRecommendedRole() {
        return recommendedRole;
    }

    public void setRecommendedRole(String recommendedRole) {
        this.recommendedRole = recommendedRole;
    }

    public String getRegulatoryCitation() {
        return regulatoryCitation;
    }

    public void setRegulatoryCitation(String regulatoryCitation) {
        this.regulatoryCitation = regulatoryCitation;
    }
}
