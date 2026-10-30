package com.complymind.digitaltwin.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.digitaltwin.model.InstitutionalDigitalTwin;
import com.complymind.digitaltwin.model.PillarHealth;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.repository.InstitutionRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DigitalTwinService {

    private final InstitutionRepository institutionRepository;
    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final AuditService auditService;

    public DigitalTwinService(InstitutionRepository institutionRepository,
                              ComplianceApplicationRepository applicationRepository,
                              ApplicationRequirementRepository appRequirementRepository,
                              AuditService auditService) {
        this.institutionRepository = institutionRepository;
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public InstitutionalDigitalTwin computeDigitalTwin(Long institutionId) {
        Institution inst = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found: " + institutionId));

        List<ComplianceApplication> apps = applicationRepository.findByInstitutionId(institutionId);

        InstitutionalDigitalTwin twin = new InstitutionalDigitalTwin();
        twin.setInstitutionId(inst.getId());
        twin.setInstitutionCode(inst.getCode());
        twin.setInstitutionName(inst.getName());
        twin.setAictePermanentId(inst.getAictePermanentId() != null ? inst.getAictePermanentId() : "N/A");
        twin.setState(inst.getState() != null ? inst.getState() : "N/A");
        twin.setCity(inst.getCity() != null ? inst.getCity() : "N/A");
        twin.setTwinVersion("v2026.1-LIVE");
        twin.setLastSynchronizedAt(LocalDateTime.now());
        twin.setRegulatoryFrameworkCode("AICTE-APH-2026-27");

        // Define the 5 pillars
        PillarHealth pInfra = new PillarHealth("INFRASTRUCTURE", "Land & Physical Infrastructure",
                "Built-up area, classrooms, instructional laboratories, barrier-free access");
        PillarHealth pFaculty = new PillarHealth("FACULTY", "Faculty & Academic Staffing",
                "Cadre ratio (1:20 norm), principal qualifications, pay commission scales");
        PillarHealth pSafety = new PillarHealth("SAFETY", "Statutory Clearances & Safety",
                "Fire & Life Safety NOC, Building Structural Stability, Environmental clearances");
        PillarHealth pGovernance = new PillarHealth("GOVERNANCE", "Academic Governance & Affiliation",
                "Affiliating University NOC, Governing Body constitution, Internal Grievance Cell");
        PillarHealth pFinance = new PillarHealth("FINANCE", "Financial Solvency & Corpus",
                "Fixed Deposit Receipt corpus, audited financial balance sheets, fee compliance");

        List<PillarHealth> pillars = List.of(pInfra, pFaculty, pSafety, pGovernance, pFinance);

        // Aggregate across all applications for this institution
        List<ApplicationRequirement> allAppReqs = new ArrayList<>();
        for (ComplianceApplication a : apps) {
            allAppReqs.addAll(appRequirementRepository.findByApplicationId(a.getId()));
        }

        for (ApplicationRequirement ar : allAppReqs) {
            Requirement req = ar.getRequirement();
            String cat = req.getCategory() != null ? req.getCategory().toUpperCase() : "";
            String code = req.getCode() != null ? req.getCode().toUpperCase() : "";

            PillarHealth target = pInfra;
            if (cat.contains("FACULTY") || cat.contains("STAFF") || code.contains("FAC")) {
                target = pFaculty;
            } else if (cat.contains("SAFETY") || cat.contains("FIRE") || code.contains("NOC") || code.contains("FIRE")) {
                target = pSafety;
            } else if (cat.contains("GOV") || cat.contains("AFFIL") || code.contains("AFFIL")) {
                target = pGovernance;
            } else if (cat.contains("FIN") || cat.contains("CORPUS") || code.contains("FEE") || code.contains("FD")) {
                target = pFinance;
            }

            target.setTotalRequirements(target.getTotalRequirements() + 1);
            if (ar.getStatus() == RequirementStatus.SATISFIED) {
                target.setSatisfiedRequirements(target.getSatisfiedRequirements() + 1);
            } else if (ar.getStatus() == RequirementStatus.BLOCKED || ar.getStatus() == RequirementStatus.REJECTED) {
                target.setBlockedOrDeficient(target.getBlockedOrDeficient() + 1);
            }
        }

        // Calculate scores and health bands
        double totalWeightedHealth = 0.0;
        int activePillars = 0;

        for (PillarHealth p : pillars) {
            if (p.getTotalRequirements() > 0) {
                double score = ((double) p.getSatisfiedRequirements() / p.getTotalRequirements()) * 100.0;
                p.setHealthScore(Math.round(score * 10.0) / 10.0);
                activePillars++;
                totalWeightedHealth += p.getHealthScore();
            } else {
                p.setHealthScore(100.0); // Baseline default if not evaluated
            }

            if (p.getHealthScore() >= 80.0) {
                p.setStatusBand("HEALTHY");
            } else if (p.getHealthScore() >= 50.0) {
                p.setStatusBand("AT_RISK");
                twin.getActiveDriftWarnings().add("Pillar '" + p.getTitle() + "' is AT RISK (" + p.getHealthScore() + "%). Remediate deficient items.");
            } else {
                p.setStatusBand("CRITICAL");
                twin.getActiveDriftWarnings().add("CRITICAL DRIFT: Pillar '" + p.getTitle() + "' has collapsed to " + p.getHealthScore() + "%. Regulatory action imminent.");
            }
        }

        twin.setPillars(pillars);

        double overallHealth = activePillars == 0 ? 100.0 : totalWeightedHealth / activePillars;
        overallHealth = Math.round(overallHealth * 10.0) / 10.0;
        twin.setOverallDigitalHealthIndex(overallHealth);

        double drift = Math.max(0.0, Math.round((100.0 - overallHealth) * 10.0) / 10.0);
        twin.setActiveComplianceDrift(drift);

        return twin;
    }
}
