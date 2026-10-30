package com.complymind.digitaltwin.model;

public class PillarHealth {

    private String pillarKey;
    private String title;
    private String description;
    private int totalRequirements;
    private int satisfiedRequirements;
    private int blockedOrDeficient;
    private double healthScore; // 0.0 - 100.0
    private String statusBand; // HEALTHY, AT_RISK, CRITICAL

    public PillarHealth() {
    }

    public PillarHealth(String pillarKey, String title, String description) {
        this.pillarKey = pillarKey;
        this.title = title;
        this.description = description;
    }

    public String getPillarKey() {
        return pillarKey;
    }

    public void setPillarKey(String pillarKey) {
        this.pillarKey = pillarKey;
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

    public int getTotalRequirements() {
        return totalRequirements;
    }

    public void setTotalRequirements(int totalRequirements) {
        this.totalRequirements = totalRequirements;
    }

    public int getSatisfiedRequirements() {
        return satisfiedRequirements;
    }

    public void setSatisfiedRequirements(int satisfiedRequirements) {
        this.satisfiedRequirements = satisfiedRequirements;
    }

    public int getBlockedOrDeficient() {
        return blockedOrDeficient;
    }

    public void setBlockedOrDeficient(int blockedOrDeficient) {
        this.blockedOrDeficient = blockedOrDeficient;
    }

    public double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(double healthScore) {
        this.healthScore = healthScore;
    }

    public String getStatusBand() {
        return statusBand;
    }

    public void setStatusBand(String statusBand) {
        this.statusBand = statusBand;
    }
}
