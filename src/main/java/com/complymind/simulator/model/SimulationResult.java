package com.complymind.simulator.model;

import com.complymind.risk.entity.RiskBand;

import java.util.ArrayList;
import java.util.List;

public class SimulationResult {

    private Long applicationId;
    private String applicationNumber;
    private String scenarioName;

    // Baseline Metrics
    private double baselineRiskScore;
    private RiskBand baselineRiskBand;
    private int baselineBlockedCount;
    private int baselineSatisfiedCount;

    // Projected Metrics
    private double projectedRiskScore;
    private RiskBand projectedRiskBand;
    private int projectedBlockedCount;
    private int projectedSatisfiedCount;

    // Deltas
    private double riskScoreDelta; // projected - baseline
    private List<String> unblockedRequirements = new ArrayList<>();
    private List<String> newlyBlockedRequirements = new ArrayList<>();
    private List<SimulatedDiff> modifiedRequirements = new ArrayList<>();
    private List<String> projectedContributingFactors = new ArrayList<>();

    public static class SimulatedDiff {
        private String requirementCode;
        private String requirementTitle;
        private String originalStatus;
        private String projectedStatus;

        public SimulatedDiff(String requirementCode, String requirementTitle, String originalStatus, String projectedStatus) {
            this.requirementCode = requirementCode;
            this.requirementTitle = requirementTitle;
            this.originalStatus = originalStatus;
            this.projectedStatus = projectedStatus;
        }

        public String getRequirementCode() { return requirementCode; }
        public String getRequirementTitle() { return requirementTitle; }
        public String getOriginalStatus() { return originalStatus; }
        public String getProjectedStatus() { return projectedStatus; }
    }

    public SimulationResult() {
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public double getBaselineRiskScore() {
        return baselineRiskScore;
    }

    public void setBaselineRiskScore(double baselineRiskScore) {
        this.baselineRiskScore = baselineRiskScore;
    }

    public RiskBand getBaselineRiskBand() {
        return baselineRiskBand;
    }

    public void setBaselineRiskBand(RiskBand baselineRiskBand) {
        this.baselineRiskBand = baselineRiskBand;
    }

    public int getBaselineBlockedCount() {
        return baselineBlockedCount;
    }

    public void setBaselineBlockedCount(int baselineBlockedCount) {
        this.baselineBlockedCount = baselineBlockedCount;
    }

    public int getBaselineSatisfiedCount() {
        return baselineSatisfiedCount;
    }

    public void setBaselineSatisfiedCount(int baselineSatisfiedCount) {
        this.baselineSatisfiedCount = baselineSatisfiedCount;
    }

    public double getProjectedRiskScore() {
        return projectedRiskScore;
    }

    public void setProjectedRiskScore(double projectedRiskScore) {
        this.projectedRiskScore = projectedRiskScore;
    }

    public RiskBand getProjectedRiskBand() {
        return projectedRiskBand;
    }

    public void setProjectedRiskBand(RiskBand projectedRiskBand) {
        this.projectedRiskBand = projectedRiskBand;
    }

    public int getProjectedBlockedCount() {
        return projectedBlockedCount;
    }

    public void setProjectedBlockedCount(int projectedBlockedCount) {
        this.projectedBlockedCount = projectedBlockedCount;
    }

    public int getProjectedSatisfiedCount() {
        return projectedSatisfiedCount;
    }

    public void setProjectedSatisfiedCount(int projectedSatisfiedCount) {
        this.projectedSatisfiedCount = projectedSatisfiedCount;
    }

    public double getRiskScoreDelta() {
        return riskScoreDelta;
    }

    public void setRiskScoreDelta(double riskScoreDelta) {
        this.riskScoreDelta = riskScoreDelta;
    }

    public List<String> getUnblockedRequirements() {
        return unblockedRequirements;
    }

    public void setUnblockedRequirements(List<String> unblockedRequirements) {
        this.unblockedRequirements = unblockedRequirements;
    }

    public List<String> getNewlyBlockedRequirements() {
        return newlyBlockedRequirements;
    }

    public void setNewlyBlockedRequirements(List<String> newlyBlockedRequirements) {
        this.newlyBlockedRequirements = newlyBlockedRequirements;
    }

    public List<SimulatedDiff> getModifiedRequirements() {
        return modifiedRequirements;
    }

    public void setModifiedRequirements(List<SimulatedDiff> modifiedRequirements) {
        this.modifiedRequirements = modifiedRequirements;
    }

    public List<String> getProjectedContributingFactors() {
        return projectedContributingFactors;
    }

    public void setProjectedContributingFactors(List<String> projectedContributingFactors) {
        this.projectedContributingFactors = projectedContributingFactors;
    }
}
