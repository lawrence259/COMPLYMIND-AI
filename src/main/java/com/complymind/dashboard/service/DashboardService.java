package com.complymind.dashboard.service;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.correctiveaction.entity.ActionStatus;
import com.complymind.correctiveaction.repository.CorrectiveActionRepository;
import com.complymind.dashboard.model.DashboardStats;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.risk.entity.RiskBand;
import com.complymind.risk.repository.RiskAssessmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final CorrectiveActionRepository correctiveActionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    public DashboardService(ComplianceApplicationRepository applicationRepository,
                            ApplicationRequirementRepository appRequirementRepository,
                            CorrectiveActionRepository correctiveActionRepository,
                            RiskAssessmentRepository riskAssessmentRepository) {
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.correctiveActionRepository = correctiveActionRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStatistics() {
        DashboardStats stats = new DashboardStats();

        // 1. Total Applications from DB
        long totalApps = applicationRepository.count();
        stats.setTotalApplications(totalApps);

        if (totalApps == 0) {
            return stats;
        }

        // 2. Applications by status
        for (ApplicationStatus status : ApplicationStatus.values()) {
            stats.getApplicationsByStatus().put(status, applicationRepository.countByStatus(status));
        }

        // 3. Blocked and Rejected Requirements count
        stats.setBlockedRequirementsCount(appRequirementRepository.countByStatus(RequirementStatus.BLOCKED));
        long rejectedCount = appRequirementRepository.countByStatus(RequirementStatus.REJECTED);
        long atRiskCount = appRequirementRepository.countByStatus(RequirementStatus.AT_RISK);
        stats.setOpenIssuesCount(stats.getBlockedRequirementsCount() + rejectedCount + atRiskCount);

        // 4. High & Critical Risk count
        stats.setHighRiskCount(riskAssessmentRepository.countByRiskBand(RiskBand.HIGH));
        stats.setCriticalRiskCount(riskAssessmentRepository.countByRiskBand(RiskBand.CRITICAL));

        // 5. Upcoming Deadlines (within 7 days) & Overdue actions
        LocalDateTime now = LocalDateTime.now();
        stats.setUpcomingDeadlinesCount(appRequirementRepository.findOverdueRequirements(now.plusDays(7)).size());

        stats.setOverdueActionsCount(correctiveActionRepository.countByDueDateBeforeAndStatusNotIn(
                now, List.of(ActionStatus.VERIFIED, ActionStatus.OVERDUE)
        ) + correctiveActionRepository.countByStatus(ActionStatus.OVERDUE));

        // 6. Recent Applications with eagerly fetched institution
        stats.setRecentApplications(applicationRepository.findTop5RecentWithInstitution(
                PageRequest.of(0, 5)
        ));

        return stats;
    }
}
