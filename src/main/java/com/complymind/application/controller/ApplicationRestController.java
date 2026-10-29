package com.complymind.application.controller;

import com.complymind.application.dto.CreateApplicationRequest;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.common.dto.ApiResponse;
import com.complymind.security.SecurityUtils;
import com.complymind.user.entity.User;
import com.complymind.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Compliance Application lifecycle endpoints")
public class ApplicationRestController {

    private final ComplianceApplicationService applicationService;
    private final UserService userService;

    public ApplicationRestController(ComplianceApplicationService applicationService, UserService userService) {
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get compliance applications")
    public ApiResponse<List<ComplianceApplication>> getApplications() {
        return ApiResponse.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID")
    public ApiResponse<ComplianceApplication> getApplication(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.getApplicationById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new compliance application")
    public ResponseEntity<ApiResponse<ComplianceApplication>> createApplication(@Valid @RequestBody CreateApplicationRequest request) {
        User user = SecurityUtils.getCurrentUserDetails()
                .map(u -> userService.getUserById(u.getId()))
                .orElse(null);

        ComplianceApplication app = applicationService.createApplication(
                request.getInstitutionId(),
                request.getFrameworkId(),
                request.getTitle(),
                request.getAcademicYear(),
                user
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(app));
    }
}
