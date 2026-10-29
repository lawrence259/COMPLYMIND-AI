package com.complymind.ripple.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.evidence.repository.EvidenceDocumentRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.ripple.entity.DependencyType;
import com.complymind.ripple.entity.RequirementDependency;
import com.complymind.ripple.model.DependencyGraph;
import com.complymind.ripple.model.GraphEdge;
import com.complymind.ripple.model.GraphNode;
import com.complymind.ripple.model.RippleAnalysisResult;
import com.complymind.ripple.repository.RequirementDependencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RippleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RippleEngineService.class);

    private final ApplicationRequirementRepository appRequirementRepository;
    private final RequirementDependencyRepository dependencyRepository;
    private final EvidenceDocumentRepository evidenceRepository;
    private final ComplianceApplicationRepository applicationRepository;

    public RippleEngineService(ApplicationRequirementRepository appRequirementRepository,
                               RequirementDependencyRepository dependencyRepository,
                               EvidenceDocumentRepository evidenceRepository,
                               ComplianceApplicationRepository applicationRepository) {
        this.appRequirementRepository = appRequirementRepository;
        this.dependencyRepository = dependencyRepository;
        this.evidenceRepository = evidenceRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public DependencyGraph buildGraphForApplication(Long applicationId) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        List<ApplicationRequirement> appReqs = appRequirementRepository.findByApplicationId(applicationId);
        List<RequirementDependency> allDeps = dependencyRepository.findByFrameworkId(app.getFramework().getId());

        DependencyGraph graph = new DependencyGraph();

        // Map requirement ID -> GraphNode
        Map<Long, GraphNode> nodeMap = new HashMap<>();
        Map<Long, ApplicationRequirement> reqToAppReq = new HashMap<>();

        for (ApplicationRequirement ar : appReqs) {
            Requirement req = ar.getRequirement();
            reqToAppReq.put(req.getId(), ar);

            GraphNode node = new GraphNode();
            node.setId("req_" + req.getId());
            node.setRequirementId(req.getId());
            node.setAppRequirementId(ar.getId());
            node.setCode(req.getCode());
            node.setTitle(req.getTitle());
            node.setCategory(req.getCategory());
            node.setMandatory(req.isMandatory());
            node.setStatus(ar.getStatus());
            node.setDeadline(ar.getDeadline());

            int evCount = evidenceRepository.findByApplicationRequirementId(ar.getId()).size();
            node.setEvidenceCount(evCount);

            nodeMap.put(req.getId(), node);
            graph.getNodes().add(node);
        }

        // Build adjacency list for cycle detection and downstream ripple
        Map<Long, List<RequirementDependency>> adjList = new HashMap<>();
        Map<Long, List<RequirementDependency>> reverseAdj = new HashMap<>();

        for (RequirementDependency dep : allDeps) {
            Long srcId = dep.getSourceRequirement().getId();
            Long tgtId = dep.getDependentRequirement().getId();

            if (nodeMap.containsKey(srcId) && nodeMap.containsKey(tgtId)) {
                adjList.computeIfAbsent(srcId, k -> new ArrayList<>()).add(dep);
                reverseAdj.computeIfAbsent(tgtId, k -> new ArrayList<>()).add(dep);

                GraphEdge edge = new GraphEdge(
                        "edge_" + dep.getId(),
                        "req_" + srcId,
                        "req_" + tgtId,
                        dep.getDependencyType(),
                        dep.getDescription(),
                        false
                );
                graph.getEdges().add(edge);
            }
        }

        // 1. Detect cycles
        checkCycles(nodeMap.keySet(), adjList, graph);

        // 2. Ripple Analysis (Downstream propagation & root cause)
        RippleAnalysisResult analysis = performRippleAnalysis(appReqs, adjList, reverseAdj, nodeMap);

        // Mark root issues on nodes
        for (String rootCode : analysis.getRootCauseMap().keySet()) {
            graph.getRootIssues().add(rootCode);
            for (GraphNode node : graph.getNodes()) {
                if (node.getCode().equals(rootCode)) {
                    node.setRootIssue(true);
                }
            }
        }

        // Highlight blocking edges in the graph
        for (GraphEdge edge : graph.getEdges()) {
            Long srcId = Long.parseLong(edge.getSource().replace("req_", ""));
            GraphNode srcNode = nodeMap.get(srcId);
            if (srcNode != null && (srcNode.getStatus() == RequirementStatus.REJECTED || srcNode.getStatus() == RequirementStatus.BLOCKED)) {
                edge.setBlocking(true);
            }
        }

        return graph;
    }

    private void checkCycles(Set<Long> nodeIds, Map<Long, List<RequirementDependency>> adjList, DependencyGraph graph) {
        Map<Long, Integer> visited = new HashMap<>(); // 0: unvisited, 1: visiting, 2: visited
        for (Long id : nodeIds) {
            visited.put(id, 0);
        }

        for (Long id : nodeIds) {
            if (visited.get(id) == 0) {
                if (dfsCycle(id, visited, adjList, new ArrayList<>(), graph)) {
                    graph.setHasCycle(true);
                    break;
                }
            }
        }
    }

    private boolean dfsCycle(Long current, Map<Long, Integer> visited,
                             Map<Long, List<RequirementDependency>> adjList,
                             List<Long> path, DependencyGraph graph) {
        visited.put(current, 1);
        path.add(current);

        List<RequirementDependency> neighbors = adjList.getOrDefault(current, Collections.emptyList());
        for (RequirementDependency dep : neighbors) {
            Long next = dep.getDependentRequirement().getId();
            if (visited.getOrDefault(next, 0) == 1) {
                graph.setCycleDescription("Cycle detected: path includes node " + current + " -> " + next);
                return true;
            }
            if (visited.getOrDefault(next, 0) == 0) {
                if (dfsCycle(next, visited, adjList, path, graph)) {
                    return true;
                }
            }
        }

        visited.put(current, 2);
        path.remove(path.size() - 1);
        return false;
    }

    public RippleAnalysisResult performRippleAnalysis(
            List<ApplicationRequirement> appReqs,
            Map<Long, List<RequirementDependency>> adjList,
            Map<Long, List<RequirementDependency>> reverseAdj,
            Map<Long, GraphNode> nodeMap) {

        RippleAnalysisResult result = new RippleAnalysisResult();
        result.setTotalNodes(appReqs.size());

        // Identify direct failed or unresolved blocking nodes
        List<Long> rootIssueIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (ApplicationRequirement ar : appReqs) {
            boolean isExplicitRejected = ar.getStatus() == RequirementStatus.REJECTED;
            boolean isOverdueMandatory = ar.getRequirement().isMandatory()
                    && ar.getDeadline() != null
                    && ar.getDeadline().isBefore(now)
                    && ar.getStatus() != RequirementStatus.SATISFIED;

            if (isExplicitRejected || isOverdueMandatory) {
                result.getDirectFailedRequirements().add(ar.getRequirement().getCode());
                // If it has no unresolved parents, it is a pure root cause
                List<RequirementDependency> parents = reverseAdj.getOrDefault(ar.getRequirement().getId(), Collections.emptyList());
                boolean hasFailedParent = parents.stream().anyMatch(p -> {
                    GraphNode parentNode = nodeMap.get(p.getSourceRequirement().getId());
                    return parentNode != null && (parentNode.getStatus() == RequirementStatus.REJECTED || parentNode.getStatus() == RequirementStatus.BLOCKED);
                });
                if (!hasFailedParent) {
                    rootIssueIds.add(ar.getRequirement().getId());
                }
            }
        }

        // For each root issue, propagate downstream using BFS
        for (Long rootId : rootIssueIds) {
            GraphNode rootNode = nodeMap.get(rootId);
            String rootCode = rootNode.getCode();
            List<String> affectedCodes = new ArrayList<>();

            Queue<Long> queue = new LinkedList<>();
            Set<Long> visitedInRipple = new HashSet<>();
            Map<Long, String> pathMap = new HashMap<>();

            queue.add(rootId);
            visitedInRipple.add(rootId);
            pathMap.put(rootId, rootCode);

            while (!queue.isEmpty()) {
                Long currId = queue.poll();
                String currPath = pathMap.get(currId);

                List<RequirementDependency> downstream = adjList.getOrDefault(currId, Collections.emptyList());
                for (RequirementDependency dep : downstream) {
                    Long nextId = dep.getDependentRequirement().getId();
                    GraphNode nextNode = nodeMap.get(nextId);

                    if (nextNode != null && !visitedInRipple.contains(nextId)) {
                        visitedInRipple.add(nextId);
                        queue.add(nextId);

                        String nextPath = currPath + " -> " + nextNode.getCode();
                        pathMap.put(nextId, nextPath);
                        affectedCodes.add(nextNode.getCode());

                        // Downstream status adjustment if not already REJECTED
                        if (nextNode.getStatus() != RequirementStatus.REJECTED) {
                            if (dep.getDependencyType() == DependencyType.HARD_BLOCKER) {
                                nextNode.setStatus(RequirementStatus.BLOCKED);
                                String reason = "BLOCKED by upstream prerequisite: " + currPath + " (Root: " + rootCode + " is REJECTED)";
                                nextNode.setExplanation(reason);
                                nextNode.getBlockedByPaths().add(nextPath);
                                result.getPathExplanations().put(nextNode.getCode(), reason);
                            } else {
                                if (nextNode.getStatus() == RequirementStatus.PENDING) {
                                    nextNode.setStatus(RequirementStatus.AT_RISK);
                                    String reason = "AT_RISK due to dependent warning: " + currPath;
                                    nextNode.setExplanation(reason);
                                    result.getPathExplanations().put(nextNode.getCode(), reason);
                                }
                            }
                        }
                    }
                }
            }

            result.getRootCauseMap().put(rootCode, affectedCodes);
        }

        // Count final states
        int satisfied = 0, pending = 0, blocked = 0, atRisk = 0, rejected = 0;
        for (GraphNode node : nodeMap.values()) {
            switch (node.getStatus()) {
                case SATISFIED -> satisfied++;
                case PENDING -> pending++;
                case BLOCKED -> blocked++;
                case AT_RISK -> atRisk++;
                case REJECTED -> rejected++;
                default -> {}
            }
        }

        result.setSatisfiedCount(satisfied);
        result.setPendingCount(pending);
        result.setBlockedCount(blocked);
        result.setAtRiskCount(atRisk);
        result.setRejectedCount(rejected);

        return result;
    }

    @Transactional
    public RippleAnalysisResult recalculateAndPersist(Long applicationId) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        List<ApplicationRequirement> appReqs = appRequirementRepository.findByApplicationId(applicationId);
        List<RequirementDependency> allDeps = dependencyRepository.findByFrameworkId(app.getFramework().getId());

        Map<Long, GraphNode> nodeMap = new HashMap<>();
        Map<Long, ApplicationRequirement> reqToAppReq = new HashMap<>();

        for (ApplicationRequirement ar : appReqs) {
            Requirement req = ar.getRequirement();
            reqToAppReq.put(req.getId(), ar);

            GraphNode node = new GraphNode();
            node.setId("req_" + req.getId());
            node.setRequirementId(req.getId());
            node.setCode(req.getCode());
            node.setTitle(req.getTitle());
            node.setStatus(ar.getStatus());
            nodeMap.put(req.getId(), node);
        }

        Map<Long, List<RequirementDependency>> adjList = new HashMap<>();
        Map<Long, List<RequirementDependency>> reverseAdj = new HashMap<>();

        for (RequirementDependency dep : allDeps) {
            Long srcId = dep.getSourceRequirement().getId();
            Long tgtId = dep.getDependentRequirement().getId();
            if (nodeMap.containsKey(srcId) && nodeMap.containsKey(tgtId)) {
                adjList.computeIfAbsent(srcId, k -> new ArrayList<>()).add(dep);
                reverseAdj.computeIfAbsent(tgtId, k -> new ArrayList<>()).add(dep);
            }
        }

        RippleAnalysisResult result = performRippleAnalysis(appReqs, adjList, reverseAdj, nodeMap);

        // Update database statuses for application requirements
        for (GraphNode node : nodeMap.values()) {
            ApplicationRequirement ar = reqToAppReq.get(node.getRequirementId());
            if (ar != null && ar.getStatus() != node.getStatus()) {
                ar.setStatus(node.getStatus());
                if (node.getExplanation() != null) {
                    ar.setComplianceNotes(node.getExplanation());
                }
                appRequirementRepository.save(ar);
            }
        }

        return result;
    }
}
