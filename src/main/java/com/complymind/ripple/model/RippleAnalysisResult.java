package com.complymind.ripple.model;

import java.util.*;

public class RippleAnalysisResult {
    private int totalNodes;
    private int satisfiedCount;
    private int pendingCount;
    private int blockedCount;
    private int atRiskCount;
    private int rejectedCount;
    private boolean cycleDetected;
    private String cycleMessage;
    private Map<String, List<String>> rootCauseMap = new LinkedHashMap<>();
    private Map<String, String> pathExplanations = new LinkedHashMap<>();
    private List<String> directFailedRequirements = new ArrayList<>();

    public RippleAnalysisResult() {}

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public int getSatisfiedCount() {
        return satisfiedCount;
    }

    public void setSatisfiedCount(int satisfiedCount) {
        this.satisfiedCount = satisfiedCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(int blockedCount) {
        this.blockedCount = blockedCount;
    }

    public int getAtRiskCount() {
        return atRiskCount;
    }

    public void setAtRiskCount(int atRiskCount) {
        this.atRiskCount = atRiskCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public boolean isCycleDetected() {
        return cycleDetected;
    }

    public void setCycleDetected(boolean cycleDetected) {
        this.cycleDetected = cycleDetected;
    }

    public String getCycleMessage() {
        return cycleMessage;
    }

    public void setCycleMessage(String cycleMessage) {
        this.cycleMessage = cycleMessage;
    }

    public Map<String, List<String>> getRootCauseMap() {
        return rootCauseMap;
    }

    public void setRootCauseMap(Map<String, List<String>> rootCauseMap) {
        this.rootCauseMap = rootCauseMap;
    }

    public Map<String, String> getPathExplanations() {
        return pathExplanations;
    }

    public void setPathExplanations(Map<String, String> pathExplanations) {
        this.pathExplanations = pathExplanations;
    }

    public List<String> getDirectFailedRequirements() {
        return directFailedRequirements;
    }

    public void setDirectFailedRequirements(List<String> directFailedRequirements) {
        this.directFailedRequirements = directFailedRequirements;
    }
}
