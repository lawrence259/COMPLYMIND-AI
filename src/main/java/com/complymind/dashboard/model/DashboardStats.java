package com.complymind.dashboard.model;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.risk.entity.RiskBand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStats {
    private long totalApplications = 0;
    private Map<ApplicationStatus, Long> applicationsByStatus = new HashMap<>();
    private long openIssuesCount = 0;
    private long blockedRequirementsCount = 0;
    private long highRiskCount = 0;
    private long criticalRiskCount = 0;
    private long upcomingDeadlinesCount = 0;
    private long overdueActionsCount = 0;
    private List<ComplianceApplication> recentApplications;

    public DashboardStats() {}

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public Map<ApplicationStatus, Long> getApplicationsByStatus() {
        return applicationsByStatus;
    }

    public void setApplicationsByStatus(Map<ApplicationStatus, Long> applicationsByStatus) {
        this.applicationsByStatus = applicationsByStatus;
    }

    public long getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(long openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public long getBlockedRequirementsCount() {
        return blockedRequirementsCount;
    }

    public void setBlockedRequirementsCount(long blockedRequirementsCount) {
        this.blockedRequirementsCount = blockedRequirementsCount;
    }

    public long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public long getCriticalRiskCount() {
        return criticalRiskCount;
    }

    public void setCriticalRiskCount(long criticalRiskCount) {
        this.criticalRiskCount = criticalRiskCount;
    }

    public long getUpcomingDeadlinesCount() {
        return upcomingDeadlinesCount;
    }

    public void setUpcomingDeadlinesCount(long upcomingDeadlinesCount) {
        this.upcomingDeadlinesCount = upcomingDeadlinesCount;
    }

    public long getOverdueActionsCount() {
        return overdueActionsCount;
    }

    public void setOverdueActionsCount(long overdueActionsCount) {
        this.overdueActionsCount = overdueActionsCount;
    }

    public List<ComplianceApplication> getRecentApplications() {
        return recentApplications;
    }

    public void setRecentApplications(List<ComplianceApplication> recentApplications) {
        this.recentApplications = recentApplications;
    }

    public boolean isEmpty() {
        return totalApplications == 0;
    }
}
