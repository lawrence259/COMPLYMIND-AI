package com.complymind.ripple.model;

import com.complymind.ripple.entity.DependencyType;

public class GraphEdge {
    private String id;
    private String source;
    private String target;
    private DependencyType dependencyType;
    private String description;
    private boolean blocking;

    public GraphEdge() {}

    public GraphEdge(String id, String source, String target, DependencyType dependencyType, String description, boolean blocking) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.dependencyType = dependencyType;
        this.description = description;
        this.blocking = blocking;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
}
