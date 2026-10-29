package com.complymind.institution.controller;

import com.complymind.common.dto.ApiResponse;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institutions")
@Tag(name = "Institutions", description = "Institution registry and management APIs")
public class InstitutionRestController {

    private final InstitutionService institutionService;

    public InstitutionRestController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    @Operation(summary = "Get all registered institutions")
    public ApiResponse<List<Institution>> getAllInstitutions() {
        return ApiResponse.ok(institutionService.getAllInstitutions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get institution by ID")
    public ApiResponse<Institution> getInstitution(@PathVariable Long id) {
        return ApiResponse.ok(institutionService.getInstitutionById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new institution")
    public ResponseEntity<ApiResponse<Institution>> createInstitution(@Valid @RequestBody Institution institution) {
        Institution created = institutionService.createInstitution(institution);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Institution created", created));
    }
}
