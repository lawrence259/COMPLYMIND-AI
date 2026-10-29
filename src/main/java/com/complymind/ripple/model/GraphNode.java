package com.complymind.ripple.model;

import com.complymind.requirement.entity.RequirementStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    private String id;
    private Long requirementId;
    private Long appRequirementId;
    private String code;
    private String title;
    private String category;
    private boolean mandatory;
    private RequirementStatus status;
    private boolean rootIssue;
    private String explanation;
    private List<String> blockedByPaths = new ArrayList<>();
    private int evidenceCount;
    private LocalDateTime deadline;

    public GraphNode() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public Long getAppRequirementId() {
        return appRequirementId;
    }

    public void setAppRequirementId(Long appRequirementId) {
        this.appRequirementId = appRequirementId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
    }

    public boolean isRootIssue() {
        return rootIssue;
    }

    public void setRootIssue(boolean rootIssue) {
        this.rootIssue = rootIssue;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getBlockedByPaths() {
        return blockedByPaths;
    }

    public void setBlockedByPaths(List<String> blockedByPaths) {
        this.blockedByPaths = blockedByPaths;
    }

    public int getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(int evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
}
