package com.complymind.recovery.model;

import java.util.ArrayList;
import java.util.List;

public class RecoveryPlan {

    private Long applicationId;
    private String applicationNumber;
    private double currentRiskScore;
    private double projectedPostRecoveryRiskScore;
    private double totalProjectedRiskReduction;
    private int totalRemediationDays;

    private List<RecoveryTask> milestone1Immediate = new ArrayList<>(); // Root blockers (P1)
    private List<RecoveryTask> milestone2Urgent = new ArrayList<>();    // Discrepancies & deficiencies (P2)
    private List<RecoveryTask> milestone3Verification = new ArrayList<>(); // Audit & closure (P3)

    public RecoveryPlan() {
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

    public double getCurrentRiskScore() {
        return currentRiskScore;
    }

    public void setCurrentRiskScore(double currentRiskScore) {
        this.currentRiskScore = currentRiskScore;
    }

    public double getProjectedPostRecoveryRiskScore() {
        return projectedPostRecoveryRiskScore;
    }

    public void setProjectedPostRecoveryRiskScore(double projectedPostRecoveryRiskScore) {
        this.projectedPostRecoveryRiskScore = projectedPostRecoveryRiskScore;
    }

    public double getTotalProjectedRiskReduction() {
        return totalProjectedRiskReduction;
    }

    public void setTotalProjectedRiskReduction(double totalProjectedRiskReduction) {
        this.totalProjectedRiskReduction = totalProjectedRiskReduction;
    }

    public int getTotalRemediationDays() {
        return totalRemediationDays;
    }

    public void setTotalRemediationDays(int totalRemediationDays) {
        this.totalRemediationDays = totalRemediationDays;
    }

    public List<RecoveryTask> getMilestone1Immediate() {
        return milestone1Immediate;
    }

    public void setMilestone1Immediate(List<RecoveryTask> milestone1Immediate) {
        this.milestone1Immediate = milestone1Immediate;
    }

    public List<RecoveryTask> getMilestone2Urgent() {
        return milestone2Urgent;
    }

    public void setMilestone2Urgent(List<RecoveryTask> milestone2Urgent) {
        this.milestone2Urgent = milestone2Urgent;
    }

    public List<RecoveryTask> getMilestone3Verification() {
        return milestone3Verification;
    }

    public void setMilestone3Verification(List<RecoveryTask> milestone3Verification) {
        this.milestone3Verification = milestone3Verification;
    }

    public List<RecoveryTask> getAllTasks() {
        List<RecoveryTask> all = new ArrayList<>();
        all.addAll(milestone1Immediate);
        all.addAll(milestone2Urgent);
        all.addAll(milestone3Verification);
        return all;
    }
}
