package com.complymind.digitaltwin.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InstitutionalDigitalTwin {

    private Long institutionId;
    private String institutionCode;
    private String institutionName;
    private String aictePermanentId;
    private String state;
    private String city;

    private String twinVersion; // e.g. "v2026.1-PROD"
    private LocalDateTime lastSynchronizedAt;
    private double overallDigitalHealthIndex; // 0.0 - 100.0
    private double activeComplianceDrift; // 0.0 - 100.0
    private String regulatoryFrameworkCode;

    private List<PillarHealth> pillars = new ArrayList<>();
    private List<String> activeDriftWarnings = new ArrayList<>();

    public InstitutionalDigitalTwin() {
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getAictePermanentId() {
        return aictePermanentId;
    }

    public void setAictePermanentId(String aictePermanentId) {
        this.aictePermanentId = aictePermanentId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTwinVersion() {
        return twinVersion;
    }

    public void setTwinVersion(String twinVersion) {
        this.twinVersion = twinVersion;
    }

    public LocalDateTime getLastSynchronizedAt() {
        return lastSynchronizedAt;
    }

    public void setLastSynchronizedAt(LocalDateTime lastSynchronizedAt) {
        this.lastSynchronizedAt = lastSynchronizedAt;
    }

    public double getOverallDigitalHealthIndex() {
        return overallDigitalHealthIndex;
    }

    public void setOverallDigitalHealthIndex(double overallDigitalHealthIndex) {
        this.overallDigitalHealthIndex = overallDigitalHealthIndex;
    }

    public double getActiveComplianceDrift() {
        return activeComplianceDrift;
    }

    public void setActiveComplianceDrift(double activeComplianceDrift) {
        this.activeComplianceDrift = activeComplianceDrift;
    }

    public String getRegulatoryFrameworkCode() {
        return regulatoryFrameworkCode;
    }

    public void setRegulatoryFrameworkCode(String regulatoryFrameworkCode) {
        this.regulatoryFrameworkCode = regulatoryFrameworkCode;
    }

    public List<PillarHealth> getPillars() {
        return pillars;
    }

    public void setPillars(List<PillarHealth> pillars) {
        this.pillars = pillars;
    }

    public List<String> getActiveDriftWarnings() {
        return activeDriftWarnings;
    }

    public void setActiveDriftWarnings(List<String> activeDriftWarnings) {
        this.activeDriftWarnings = activeDriftWarnings;
    }
}
