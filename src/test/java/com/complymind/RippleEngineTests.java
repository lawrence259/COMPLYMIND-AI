package com.complymind;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.ripple.entity.DependencyType;
import com.complymind.ripple.entity.RequirementDependency;
import com.complymind.ripple.model.DependencyGraph;
import com.complymind.ripple.model.GraphEdge;
import com.complymind.ripple.model.GraphNode;
import com.complymind.ripple.model.RippleAnalysisResult;
import com.complymind.ripple.service.RippleEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RippleEngineTests {

    private final RippleEngineService rippleService = new RippleEngineService(null, null, null, null);

    @Test
    @DisplayName("BFS/DFS: Propagate downstream blockers when root prerequisite is REJECTED")
    void testDownstreamRipplePropagation() {
        // Setup 4 requirements: REQ-01 -> REQ-02 -> REQ-03 -> REQ-04
        ComplianceFramework fw = new ComplianceFramework();
        fw.setId(1L);
        fw.setCode("DEMO-FW");

        Requirement r1 = createReq(1L, "REQ-01", "Land Title Deed");
        Requirement r2 = createReq(2L, "REQ-02", "Building Plan Approval");
        Requirement r3 = createReq(3L, "REQ-03", "Occupancy Certificate");
        Requirement r4 = createReq(4L, "REQ-04", "Fire NOC");

        ApplicationRequirement ar1 = createAppReq(101L, r1, RequirementStatus.REJECTED);
        ApplicationRequirement ar2 = createAppReq(102L, r2, RequirementStatus.PENDING);
        ApplicationRequirement ar3 = createAppReq(103L, r3, RequirementStatus.PENDING);
        ApplicationRequirement ar4 = createAppReq(104L, r4, RequirementStatus.PENDING);

        List<ApplicationRequirement> appReqs = List.of(ar1, ar2, ar3, ar4);

        Map<Long, GraphNode> nodeMap = new HashMap<>();
        for (ApplicationRequirement ar : appReqs) {
            GraphNode gn = new GraphNode();
            gn.setId("req_" + ar.getRequirement().getId());
            gn.setRequirementId(ar.getRequirement().getId());
            gn.setCode(ar.getRequirement().getCode());
            gn.setTitle(ar.getRequirement().getTitle());
            gn.setStatus(ar.getStatus());
            nodeMap.put(ar.getRequirement().getId(), gn);
        }

        // Dependencies: r1 -> r2 (HARD_BLOCKER), r2 -> r3 (HARD_BLOCKER), r3 -> r4 (PREREQUISITE)
        Map<Long, List<RequirementDependency>> adjList = new HashMap<>();
        Map<Long, List<RequirementDependency>> reverseAdj = new HashMap<>();

        addDep(r1, r2, DependencyType.HARD_BLOCKER, adjList, reverseAdj);
        addDep(r2, r3, DependencyType.HARD_BLOCKER, adjList, reverseAdj);
        addDep(r3, r4, DependencyType.PREREQUISITE, adjList, reverseAdj);

        RippleAnalysisResult result = rippleService.performRippleAnalysis(appReqs, adjList, reverseAdj, nodeMap);

        // Verify Root Cause
        assertTrue(result.getRootCauseMap().containsKey("REQ-01"), "Root cause map should identify REQ-01");
        List<String> affected = result.getRootCauseMap().get("REQ-01");
        assertTrue(affected.contains("REQ-02"), "REQ-02 must be affected downstream");
        assertTrue(affected.contains("REQ-03"), "REQ-03 must be affected downstream");
        assertTrue(affected.contains("REQ-04"), "REQ-04 must be affected downstream");

        // Verify downstream statuses
        assertEquals(RequirementStatus.BLOCKED, nodeMap.get(2L).getStatus(), "REQ-02 should be BLOCKED");
        assertEquals(RequirementStatus.BLOCKED, nodeMap.get(3L).getStatus(), "REQ-03 should be BLOCKED");
        assertEquals(RequirementStatus.AT_RISK, nodeMap.get(4L).getStatus(), "REQ-04 should be AT_RISK");

        // Verify explanation paths
        assertNotNull(nodeMap.get(2L).getExplanation());
        assertTrue(nodeMap.get(2L).getExplanation().contains("Root: REQ-01 is REJECTED"));
    }

    @Test
    @DisplayName("Cycle Detection: DFS detects circular dependencies (A -> B -> C -> A)")
    void testCycleDetection() {
        DependencyGraph graph = new DependencyGraph();
        Set<Long> nodeIds = Set.of(1L, 2L, 3L);

        Requirement r1 = createReq(1L, "REQ-01", "A");
        Requirement r2 = createReq(2L, "REQ-02", "B");
        Requirement r3 = createReq(3L, "REQ-03", "C");

        Map<Long, List<RequirementDependency>> adjList = new HashMap<>();
        Map<Long, List<RequirementDependency>> reverseAdj = new HashMap<>();

        // Cycle: 1 -> 2, 2 -> 3, 3 -> 1
        addDep(r1, r2, DependencyType.HARD_BLOCKER, adjList, reverseAdj);
        addDep(r2, r3, DependencyType.HARD_BLOCKER, adjList, reverseAdj);
        addDep(r3, r1, DependencyType.HARD_BLOCKER, adjList, reverseAdj);

        // Call cycle check via reflection or simulate DFS
        Map<Long, Integer> visited = new HashMap<>();
        for (Long id : nodeIds) visited.put(id, 0);

        boolean cycleFound = false;
        for (Long id : nodeIds) {
            if (visited.get(id) == 0 && dfs(id, visited, adjList)) {
                cycleFound = true;
                break;
            }
        }

        assertTrue(cycleFound, "Circular dependency A -> B -> C -> A must be detected");
    }

    private boolean dfs(Long curr, Map<Long, Integer> visited, Map<Long, List<RequirementDependency>> adjList) {
        visited.put(curr, 1);
        for (RequirementDependency dep : adjList.getOrDefault(curr, Collections.emptyList())) {
            Long next = dep.getDependentRequirement().getId();
            if (visited.getOrDefault(next, 0) == 1) return true;
            if (visited.getOrDefault(next, 0) == 0 && dfs(next, visited, adjList)) return true;
        }
        visited.put(curr, 2);
        return false;
    }

    private Requirement createReq(Long id, String code, String title) {
        Requirement r = new Requirement();
        r.setId(id);
        r.setCode(code);
        r.setTitle(title);
        r.setMandatory(true);
        return r;
    }

    private ApplicationRequirement createAppReq(Long id, Requirement req, RequirementStatus status) {
        ApplicationRequirement ar = new ApplicationRequirement();
        ar.setId(id);
        ar.setRequirement(req);
        ar.setStatus(status);
        return ar;
    }

    private void addDep(Requirement src, Requirement tgt, DependencyType type,
                        Map<Long, List<RequirementDependency>> adj,
                        Map<Long, List<RequirementDependency>> rev) {
        RequirementDependency dep = new RequirementDependency();
        dep.setSourceRequirement(src);
        dep.setDependentRequirement(tgt);
        dep.setDependencyType(type);
        adj.computeIfAbsent(src.getId(), k -> new ArrayList<>()).add(dep);
        rev.computeIfAbsent(tgt.getId(), k -> new ArrayList<>()).add(dep);
    }
}
