package com.complymind.simulator.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.entity.ReviewStatus;
import com.complymind.evidence.repository.EvidenceDocumentRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.ripple.entity.DependencyType;
import com.complymind.ripple.entity.RequirementDependency;
import com.complymind.ripple.repository.RequirementDependencyRepository;
import com.complymind.ripple.service.RippleEngineService;
import com.complymind.risk.entity.RiskBand;
import com.complymind.risk.model.RiskBreakdown;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.simulator.model.SimulationScenario;
import com.complymind.simulator.model.SimulationResult;
import com.complymind.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WhatIfSimulatorService {

    @Value("${app.risk.weights.deadline-urgency:0.35}")
    private double wDeadlineUrgency;

    @Value("${app.risk.weights.blocked-dependency:0.30}")
    private double wBlockedDependency;

    @Value("${app.risk.weights.evidence-deficiency:0.20}")
    private double wEvidenceDeficiency;

    @Value("${app.risk.weights.unresolved-pendency:0.15}")
    private double wUnresolvedPendency;

    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final RequirementDependencyRepository dependencyRepository;
    private final EvidenceDocumentRepository evidenceRepository;
    private final RiskEngineService riskEngineService;
    private final RippleEngineService rippleEngineService;
    private final AuditService auditService;

    public WhatIfSimulatorService(ComplianceApplicationRepository applicationRepository,
                                  ApplicationRequirementRepository appRequirementRepository,
                                  RequirementDependencyRepository dependencyRepository,
                                  EvidenceDocumentRepository evidenceRepository,
                                  RiskEngineService riskEngineService,
                                  RippleEngineService rippleEngineService,
                                  AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.dependencyRepository = dependencyRepository;
        this.evidenceRepository = evidenceRepository;
        this.riskEngineService = riskEngineService;
        this.rippleEngineService = rippleEngineService;
        this.auditService = auditService;
    }

    /**
     * Executes in-memory simulation against a snapshot.
     * Absolutely zero modifications are persisted to live database entities.
     */
    @Transactional(readOnly = true)
    public SimulationResult runSimulation(SimulationScenario scenario) {
        Long appId = scenario.getApplicationId();
        ComplianceApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + appId));

        List<ApplicationRequirement> liveReqs = appRequirementRepository.findByApplicationId(appId);
        List<RequirementDependency> dependencies = dependencyRepository.findByFrameworkId(app.getFramework().getId());
        List<EvidenceDocument> documents = evidenceRepository.findByApplicationId(appId);

        // Current Baseline
        RiskBreakdown baselineBreakdown = riskEngineService.calculateRisk(appId);

        SimulationResult result = new SimulationResult();
        result.setApplicationId(appId);
        result.setApplicationNumber(app.getApplicationNumber());
        result.setScenarioName(scenario.getScenarioName());
        result.setBaselineRiskScore(baselineBreakdown.getOverallScore());
        result.setBaselineRiskBand(baselineBreakdown.getRiskBand());

        int baselineBlocked = 0;
        int baselineSatisfied = 0;
        for (ApplicationRequirement ar : liveReqs) {
            if (ar.getStatus() == RequirementStatus.BLOCKED) baselineBlocked++;
            if (ar.getStatus() == RequirementStatus.SATISFIED) baselineSatisfied++;
        }
        result.setBaselineBlockedCount(baselineBlocked);
        result.setBaselineSatisfiedCount(baselineSatisfied);

        // 1. Build In-Memory Snapshot Status Map
        Map<Long, RequirementStatus> snapshotStatuses = new HashMap<>();
        Map<Long, ApplicationRequirement> reqMap = new HashMap<>();

        for (ApplicationRequirement ar : liveReqs) {
            reqMap.put(ar.getRequirement().getId(), ar);
            RequirementStatus hypStatus = scenario.getRequirementOverrides().get(ar.getId());
            if (hypStatus != null) {
                snapshotStatuses.put(ar.getRequirement().getId(), hypStatus);
                result.getModifiedRequirements().add(new SimulationResult.SimulatedDiff(
                        ar.getRequirement().getCode(),
                        ar.getRequirement().getTitle(),
                        ar.getStatus().name(),
                        hypStatus.name()
                ));
            } else {
                snapshotStatuses.put(ar.getRequirement().getId(), ar.getStatus());
            }
        }

        // 2. In-Memory Graph Ripple Propagation (BFS)
        Map<Long, List<RequirementDependency>> adjList = new HashMap<>();
        for (RequirementDependency dep : dependencies) {
            adjList.computeIfAbsent(dep.getSourceRequirement().getId(), k -> new ArrayList<>()).add(dep);
        }

        // Propagate cascades in memory
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();

        for (Map.Entry<Long, RequirementStatus> entry : snapshotStatuses.entrySet()) {
            if (entry.getValue() == RequirementStatus.REJECTED || entry.getValue() == RequirementStatus.BLOCKED) {
                queue.add(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            Long srcId = queue.poll();
            if (visited.contains(srcId)) continue;
            visited.add(srcId);

            List<RequirementDependency> outgoing = adjList.getOrDefault(srcId, Collections.emptyList());
            for (RequirementDependency dep : outgoing) {
                if (dep.getDependencyType() == DependencyType.HARD_BLOCKER || dep.getDependencyType() == DependencyType.PREREQUISITE) {
                    Long targetId = dep.getDependentRequirement().getId();
                    RequirementStatus currentSnap = snapshotStatuses.get(targetId);
                    if (currentSnap != null && currentSnap != RequirementStatus.BLOCKED) {
                        snapshotStatuses.put(targetId, RequirementStatus.BLOCKED);
                        queue.add(targetId);
                    }
                }
            }
        }

        // Compare newly blocked or unblocked
        int projectedBlocked = 0;
        int projectedSatisfied = 0;

        for (ApplicationRequirement ar : liveReqs) {
            Long reqId = ar.getRequirement().getId();
            RequirementStatus original = ar.getStatus();
            RequirementStatus projected = snapshotStatuses.get(reqId);

            if (projected == RequirementStatus.BLOCKED) projectedBlocked++;
            if (projected == RequirementStatus.SATISFIED) projectedSatisfied++;

            if (original == RequirementStatus.BLOCKED && projected != RequirementStatus.BLOCKED) {
                result.getUnblockedRequirements().add(ar.getRequirement().getCode() + " - " + ar.getRequirement().getTitle());
            } else if (original != RequirementStatus.BLOCKED && projected == RequirementStatus.BLOCKED) {
                result.getNewlyBlockedRequirements().add(ar.getRequirement().getCode() + " - " + ar.getRequirement().getTitle());
            }
        }

        result.setProjectedBlockedCount(projectedBlocked);
        result.setProjectedSatisfiedCount(projectedSatisfied);

        // 3. Compute Simulated Multi-Factor Risk Score In-Memory
        LocalDateTime simNow = LocalDateTime.now();
        int daysExt = scenario.getDeadlineExtensionDays() != null ? scenario.getDeadlineExtensionDays() : 0;
        int totalReqs = liveReqs.size();

        // Factor 1: Deadline Urgency with simulated extension
        double totalUrgency = 0.0;
        int urgencyCount = 0;
        for (ApplicationRequirement ar : liveReqs) {
            RequirementStatus status = snapshotStatuses.get(ar.getRequirement().getId());
            if (status != RequirementStatus.SATISFIED) {
                urgencyCount++;
                LocalDateTime effectiveDeadline = ar.getDeadline() != null ? ar.getDeadline().plusDays(daysExt) : null;
                if (effectiveDeadline == null) {
                    totalUrgency += 20.0;
                } else if (effectiveDeadline.isBefore(simNow)) {
                    totalUrgency += 100.0;
                } else {
                    long days = Duration.between(simNow, effectiveDeadline).toDays();
                    if (days <= 2) totalUrgency += 95.0;
                    else if (days <= 7) totalUrgency += 75.0;
                    else if (days <= 14) totalUrgency += 50.0;
                    else if (days <= 30) totalUrgency += 25.0;
                    else totalUrgency += 10.0;
                }
            }
        }
        double simDeadlineScore = urgencyCount == 0 ? 0.0 : totalUrgency / urgencyCount;

        // Factor 2: Blocked Dependency
        double simBlockedScore = totalReqs == 0 ? 0.0 : ((double) projectedBlocked / totalReqs) * 100.0;

        // Factor 3: Evidence Deficiency
        int deficientEvidenceCount = 0;
        for (ApplicationRequirement ar : liveReqs) {
            RequirementStatus status = snapshotStatuses.get(ar.getRequirement().getId());
            if (status != RequirementStatus.SATISFIED) {
                boolean hasApproved = documents.stream().anyMatch(d ->
                        d.getApplicationRequirement().getId().equals(ar.getId()) && d.getReviewStatus() == ReviewStatus.APPROVED);
                if (!hasApproved) deficientEvidenceCount++;
            }
        }
        double simEvidenceScore = totalReqs == 0 ? 0.0 : ((double) deficientEvidenceCount / totalReqs) * 100.0;

        // Factor 4: Unresolved Pendency
        int pendingCount = 0;
        for (ApplicationRequirement ar : liveReqs) {
            RequirementStatus status = snapshotStatuses.get(ar.getRequirement().getId());
            if (status != RequirementStatus.SATISFIED) pendingCount++;
        }
        double simPendencyScore = totalReqs == 0 ? 0.0 : ((double) pendingCount / totalReqs) * 100.0;

        // Overall Weighted Formula
        double rawSimRisk = (wDeadlineUrgency * simDeadlineScore)
                + (wBlockedDependency * simBlockedScore)
                + (wEvidenceDeficiency * simEvidenceScore)
                + (wUnresolvedPendency * simPendencyScore);

        double projectedScore = Math.max(0.0, Math.min(100.0, rawSimRisk));
        projectedScore = Math.round(projectedScore * 100.0) / 100.0;

        RiskBand simBand;
        if (projectedScore <= 29.0) simBand = RiskBand.LOW;
        else if (projectedScore <= 59.0) simBand = RiskBand.MEDIUM;
        else if (projectedScore <= 79.0) simBand = RiskBand.HIGH;
        else simBand = RiskBand.CRITICAL;

        result.setProjectedRiskScore(projectedScore);
        result.setProjectedRiskBand(simBand);
        result.setRiskScoreDelta(Math.round((projectedScore - baselineBreakdown.getOverallScore()) * 100.0) / 100.0);

        if (!result.getUnblockedRequirements().isEmpty()) {
            result.getProjectedContributingFactors().add("Resolving prerequisite blockers successfully unblocks " +
                    result.getUnblockedRequirements().size() + " downstream requirement(s).");
        }
        if (daysExt > 0) {
            result.getProjectedContributingFactors().add("Extending deadlines by " + daysExt + " days relieves immediate impending urgency penalties.");
        }
        result.getProjectedContributingFactors().add("Projected risk changes from " + baselineBreakdown.getOverallScore() +
                " (" + baselineBreakdown.getRiskBand() + ") to " + projectedScore + " (" + simBand + ").");

        return result;
    }

    /**
     * Authorised action: explicitly commits a simulation scenario to live application records.
     */
    @Transactional
    public void applySimulationToLive(Long applicationId, SimulationScenario scenario, User authorizedUser) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        List<ApplicationRequirement> liveReqs = appRequirementRepository.findByApplicationId(applicationId);

        int appliedCount = 0;
        for (ApplicationRequirement ar : liveReqs) {
            RequirementStatus hypStatus = scenario.getRequirementOverrides().get(ar.getId());
            if (hypStatus != null && hypStatus != ar.getStatus()) {
                ar.setStatus(hypStatus);
                ar.setComplianceNotes("Applied from What-If simulation scenario: " + scenario.getScenarioName() +
                        " by " + authorizedUser.getUsername());
                appRequirementRepository.save(ar);
                appliedCount++;
            }
            if (scenario.getDeadlineExtensionDays() != null && scenario.getDeadlineExtensionDays() > 0 && ar.getDeadline() != null) {
                ar.setDeadline(ar.getDeadline().plusDays(scenario.getDeadlineExtensionDays()));
                appRequirementRepository.save(ar);
            }
        }

        // Re-run real ripple engine and risk engine to persist verified live state
        rippleEngineService.recalculateAndPersist(applicationId);
        riskEngineService.evaluateAndPersist(applicationId);

        auditService.recordEvent("WHAT_IF_APPLIED", "ComplianceApplication", applicationId.toString(),
                "Simulation scenario '" + scenario.getScenarioName() + "' applied to live state. " +
                        appliedCount + " requirement status updates applied by " + authorizedUser.getUsername());
    }
}
