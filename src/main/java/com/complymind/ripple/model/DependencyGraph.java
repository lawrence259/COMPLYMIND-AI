package com.complymind.ripple.model;

import java.util.ArrayList;
import java.util.List;

public class DependencyGraph {
    private List<GraphNode> nodes = new ArrayList<>();
    private List<GraphEdge> edges = new ArrayList<>();
    private boolean hasCycle = false;
    private String cycleDescription;
    private List<String> rootIssues = new ArrayList<>();

    public DependencyGraph() {}

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdge> edges) {
        this.edges = edges;
    }

    public boolean isHasCycle() {
        return hasCycle;
    }

    public void setHasCycle(boolean hasCycle) {
        this.hasCycle = hasCycle;
    }

    public String getCycleDescription() {
        return cycleDescription;
    }

    public void setCycleDescription(String cycleDescription) {
        this.cycleDescription = cycleDescription;
    }

    public List<String> getRootIssues() {
        return rootIssues;
    }

    public void setRootIssues(List<String> rootIssues) {
        this.rootIssues = rootIssues;
    }
}
