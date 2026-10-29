package com.complymind.risk.model;

import com.complymind.risk.entity.RiskBand;
import java.util.ArrayList;
import java.util.List;

public class RiskBreakdown {
    private double overallScore;
    private RiskBand riskBand;
    private double deadlineUrgencyScore;
    private double blockedDependencyScore;
    private double evidenceDeficiencyScore;
    private double unresolvedPendencyScore;
    private List<String> contributingFactors = new ArrayList<>();
    private String heuristicDisclaimer = "Illustrative prioritisation heuristic calculated via deterministic rule engine. Not an official AICTE outcome prediction.";

    public RiskBreakdown() {}

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public RiskBand getRiskBand() {
        return riskBand;
    }

    public void setRiskBand(RiskBand riskBand) {
        this.riskBand = riskBand;
    }

    public double getDeadlineUrgencyScore() {
        return deadlineUrgencyScore;
    }

    public void setDeadlineUrgencyScore(double deadlineUrgencyScore) {
        this.deadlineUrgencyScore = deadlineUrgencyScore;
    }

    public double getBlockedDependencyScore() {
        return blockedDependencyScore;
    }

    public void setBlockedDependencyScore(double blockedDependencyScore) {
        this.blockedDependencyScore = blockedDependencyScore;
    }

    public double getEvidenceDeficiencyScore() {
        return evidenceDeficiencyScore;
    }

    public void setEvidenceDeficiencyScore(double evidenceDeficiencyScore) {
        this.evidenceDeficiencyScore = evidenceDeficiencyScore;
    }

    public double getUnresolvedPendencyScore() {
        return unresolvedPendencyScore;
    }

    public void setUnresolvedPendencyScore(double unresolvedPendencyScore) {
        this.unresolvedPendencyScore = unresolvedPendencyScore;
    }

    public List<String> getContributingFactors() {
        return contributingFactors;
    }

    public void setContributingFactors(List<String> contributingFactors) {
        this.contributingFactors = contributingFactors;
    }

    public String getHeuristicDisclaimer() {
        return heuristicDisclaimer;
    }

    public void setHeuristicDisclaimer(String heuristicDisclaimer) {
        this.heuristicDisclaimer = heuristicDisclaimer;
    }
}
