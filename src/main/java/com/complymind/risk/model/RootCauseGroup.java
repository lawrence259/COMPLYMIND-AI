package com.complymind.risk.model;

import java.util.ArrayList;
import java.util.List;

public class RootCauseGroup {
    private String rootRequirementCode;
    private String rootRequirementTitle;
    private String rootIssueReason;
    private int affectedRequirementsCount;
    private List<String> affectedRequirements = new ArrayList<>();
    private List<String> dependencyPaths = new ArrayList<>();
    private String suggestedCorrectiveAction;
    private String resolutionStatus;

    public RootCauseGroup() {}

    public String getRootRequirementCode() {
        return rootRequirementCode;
    }

    public void setRootRequirementCode(String rootRequirementCode) {
        this.rootRequirementCode = rootRequirementCode;
    }

    public String getRootRequirementTitle() {
        return rootRequirementTitle;
    }

    public void setRootRequirementTitle(String rootRequirementTitle) {
        this.rootRequirementTitle = rootRequirementTitle;
    }

    public String getRootIssueReason() {
        return rootIssueReason;
    }

    public void setRootIssueReason(String rootIssueReason) {
        this.rootIssueReason = rootIssueReason;
    }

    public int getAffectedRequirementsCount() {
        return affectedRequirementsCount;
    }

    public void setAffectedRequirementsCount(int affectedRequirementsCount) {
        this.affectedRequirementsCount = affectedRequirementsCount;
    }

    public List<String> getAffectedRequirements() {
        return affectedRequirements;
    }

    public void setAffectedRequirements(List<String> affectedRequirements) {
        this.affectedRequirements = affectedRequirements;
    }

    public List<String> getDependencyPaths() {
        return dependencyPaths;
    }

    public void setDependencyPaths(List<String> dependencyPaths) {
        this.dependencyPaths = dependencyPaths;
    }

    public String getSuggestedCorrectiveAction() {
        return suggestedCorrectiveAction;
    }

    public void setSuggestedCorrectiveAction(String suggestedCorrectiveAction) {
        this.suggestedCorrectiveAction = suggestedCorrectiveAction;
    }

    public String getResolutionStatus() {
        return resolutionStatus;
    }

    public void setResolutionStatus(String resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }
}
