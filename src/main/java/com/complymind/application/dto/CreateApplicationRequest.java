package com.complymind.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateApplicationRequest {

    @NotNull(message = "Institution ID is required")
    private Long institutionId;

    @NotNull(message = "Compliance Framework ID is required")
    private Long frameworkId;

    @NotBlank(message = "Application title cannot be blank")
    @Size(max = 255, message = "Application title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Academic year is required")
    @Size(max = 32, message = "Academic year cannot exceed 32 characters")
    private String academicYear;

    public CreateApplicationRequest() {}

    public CreateApplicationRequest(Long institutionId, Long frameworkId, String title, String academicYear) {
        this.institutionId = institutionId;
        this.frameworkId = frameworkId;
        this.title = title;
        this.academicYear = academicYear;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Long getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(Long frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }
}
