package com.complymind.ingestion.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataIngestionResult {

    private String datasetName;
    private String sourceUrl;
    private String datasetVersion;
    private DataClassification classification;
    private LocalDateTime executionTime = LocalDateTime.now();

    private int totalRecords = 0;
    private int addedCount = 0;
    private int updatedCount = 0;
    private int skippedCount = 0;
    private int rejectedCount = 0;

    private List<String> errors = new ArrayList<>();
    private List<String> changeSummary = new ArrayList<>();

    public DataIngestionResult() {}

    public DataIngestionResult(String datasetName, String sourceUrl, String datasetVersion, DataClassification classification) {
        this.datasetName = datasetName;
        this.sourceUrl = sourceUrl;
        this.datasetVersion = datasetVersion;
        this.classification = classification;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(String datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public DataClassification getClassification() {
        return classification;
    }

    public void setClassification(DataClassification classification) {
        this.classification = classification;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(int addedCount) {
        this.addedCount = addedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(List<String> changeSummary) {
        this.changeSummary = changeSummary;
    }
}
