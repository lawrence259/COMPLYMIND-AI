package com.complymind.risk.service;

import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.ripple.model.DependencyGraph;
import com.complymind.ripple.model.GraphNode;
import com.complymind.ripple.service.RippleEngineService;
import com.complymind.risk.model.RootCauseGroup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RootCauseInvestigatorService {

    private final RippleEngineService rippleEngineService;
    private final ApplicationRequirementRepository appRequirementRepository;

    public RootCauseInvestigatorService(RippleEngineService rippleEngineService,
                                        ApplicationRequirementRepository appRequirementRepository) {
        this.rippleEngineService = rippleEngineService;
        this.appRequirementRepository = appRequirementRepository;
    }

    @Transactional(readOnly = true)
    public List<RootCauseGroup> investigateRootCauses(Long applicationId) {
        DependencyGraph graph = rippleEngineService.buildGraphForApplication(applicationId);
        List<ApplicationRequirement> appReqs = appRequirementRepository.findByApplicationId(applicationId);

        Map<String, ApplicationRequirement> codeToAppReq = new HashMap<>();
        for (ApplicationRequirement ar : appReqs) {
            codeToAppReq.put(ar.getRequirement().getCode(), ar);
        }

        List<RootCauseGroup> groups = new ArrayList<>();

        for (String rootCode : graph.getRootIssues()) {
            ApplicationRequirement rootAppReq = codeToAppReq.get(rootCode);
            if (rootAppReq == null) continue;

            Requirement rootReq = rootAppReq.getRequirement();
            RootCauseGroup group = new RootCauseGroup();
            group.setRootRequirementCode(rootReq.getCode());
            group.setRootRequirementTitle(rootReq.getTitle());

            String issueReason = "Requirement status is " + rootAppReq.getStatus() + ".";
            if (rootAppReq.getComplianceNotes() != null && !rootAppReq.getComplianceNotes().isBlank()) {
                issueReason += " Note: " + rootAppReq.getComplianceNotes();
            }
            group.setRootIssueReason(issueReason);

            // Find all nodes in the graph that were blocked by this root issue
            List<String> affected = new ArrayList<>();
            List<String> paths = new ArrayList<>();

            for (GraphNode node : graph.getNodes()) {
                if (node.getBlockedByPaths() != null) {
                    for (String path : node.getBlockedByPaths()) {
                        if (path.startsWith(rootCode)) {
                            affected.add(node.getCode() + " - " + node.getTitle());
                            paths.add(path);
                            break;
                        }
                    }
                }
            }

            group.setAffectedRequirementsCount(affected.size());
            group.setAffectedRequirements(affected);
            group.setDependencyPaths(paths);

            // Recommend corrective action
            String actionSuggestion = "Resolve primary blocker on " + rootReq.getCode() +
                    " (" + rootReq.getTitle() + ") by uploading compliant and verified evidence.";
            group.setSuggestedCorrectiveAction(actionSuggestion);
            group.setResolutionStatus(rootAppReq.getStatus().name());

            groups.add(group);
        }

        return groups;
    }
}
