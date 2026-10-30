package com.complymind.simulator.model;

import com.complymind.requirement.entity.RequirementStatus;

import java.util.HashMap;
import java.util.Map;

public class SimulationScenario {

    private Long applicationId;
    private Map<Long, RequirementStatus> requirementOverrides = new HashMap<>();
    private Integer deadlineExtensionDays = 0;
    private String scenarioName = "Custom Hypothetical Scenario";

    public SimulationScenario() {
    }

    public SimulationScenario(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Map<Long, RequirementStatus> getRequirementOverrides() {
        return requirementOverrides;
    }

    public void setRequirementOverrides(Map<Long, RequirementStatus> requirementOverrides) {
        this.requirementOverrides = requirementOverrides;
    }

    public Integer getDeadlineExtensionDays() {
        return deadlineExtensionDays;
    }

    public void setDeadlineExtensionDays(Integer deadlineExtensionDays) {
        this.deadlineExtensionDays = deadlineExtensionDays;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }
}
