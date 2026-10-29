package com.complymind.demo.service;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.correctiveaction.entity.ActionStatus;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import com.complymind.correctiveaction.entity.Priority;
import com.complymind.correctiveaction.repository.CorrectiveActionRepository;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.entity.ReviewStatus;
import com.complymind.evidence.repository.EvidenceDocumentRepository;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.entity.ComplianceRule;
import com.complymind.framework.repository.ComplianceFrameworkRepository;
import com.complymind.framework.repository.ComplianceRuleRepository;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.entity.InstitutionType;
import com.complymind.institution.repository.InstitutionRepository;
import com.complymind.notification.repository.NotificationRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.requirement.repository.RequirementRepository;
import com.complymind.ripple.entity.DependencyType;
import com.complymind.ripple.entity.RequirementDependency;
import com.complymind.ripple.repository.RequirementDependencyRepository;
import com.complymind.ripple.service.RippleEngineService;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import com.complymind.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DemoDatasetService {

    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final ComplianceFrameworkRepository frameworkRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementDependencyRepository dependencyRepository;
    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final EvidenceDocumentRepository evidenceRepository;
    private final CorrectiveActionRepository correctiveActionRepository;
    private final NotificationRepository notificationRepository;
    private final RippleEngineService rippleEngineService;
    private final RiskEngineService riskEngineService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public DemoDatasetService(InstitutionRepository institutionRepository,
                              UserRepository userRepository,
                              ComplianceFrameworkRepository frameworkRepository,
                              ComplianceRuleRepository ruleRepository,
                              RequirementRepository requirementRepository,
                              RequirementDependencyRepository dependencyRepository,
                              ComplianceApplicationRepository applicationRepository,
                              ApplicationRequirementRepository appRequirementRepository,
                              EvidenceDocumentRepository evidenceRepository,
                              CorrectiveActionRepository correctiveActionRepository,
                              NotificationRepository notificationRepository,
                              RippleEngineService rippleEngineService,
                              RiskEngineService riskEngineService,
                              PasswordEncoder passwordEncoder,
                              AuditService auditService) {
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.frameworkRepository = frameworkRepository;
        this.ruleRepository = ruleRepository;
        this.requirementRepository = requirementRepository;
        this.dependencyRepository = dependencyRepository;
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.evidenceRepository = evidenceRepository;
        this.correctiveActionRepository = correctiveActionRepository;
        this.notificationRepository = notificationRepository;
        this.rippleEngineService = rippleEngineService;
        this.riskEngineService = riskEngineService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public void loadDemoDataset() {
        // Clean any existing demo data first to keep it idempotent
        removeDemoDataset();

        LocalDateTime now = LocalDateTime.now();

        // 1. Synthetic Demo Institution
        Institution inst = new Institution();
        inst.setCode("NIST-DEMO");
        inst.setName("National Institute of Science & Technology (Demo)");
        inst.setType(InstitutionType.ENGINEERING_AND_TECHNOLOGY);
        inst.setAictePermanentId("1-DEMO-998822");
        inst.setState("Maharashtra");
        inst.setCity("Pune");
        inst.setAddress("Sector 14, Hinjewadi Phase 2, Pune");
        inst.setContactEmail("compliance@nist-demo.edu.in");
        inst.setContactPhone("+91 20 4455 6677");
        inst.setDemo(true);
        inst = institutionRepository.save(inst);

        // 2. Demo Users
        if (userRepository.findByUsername("inst_nist").isEmpty()) {
            User instUser = new User();
            instUser.setUsername("inst_nist");
            instUser.setEmail("nodal@nist-demo.edu.in");
            instUser.setFullName("Prof. Rajesh Kulkarni (Nodal Officer)");
            instUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            instUser.setRole(UserRole.INSTITUTION_USER);
            instUser.setInstitution(inst);
            instUser.setEnabled(true);
            userRepository.save(instUser);
        }

        if (userRepository.findByUsername("officer_sharma").isEmpty()) {
            User officer = new User();
            officer.setUsername("officer_sharma");
            officer.setEmail("v.sharma@complymind.ai");
            officer.setFullName("Dr. Vikram Sharma (Compliance Officer)");
            officer.setPasswordHash(passwordEncoder.encode("Password123!"));
            officer.setRole(UserRole.COMPLIANCE_OFFICER);
            officer.setEnabled(true);
            userRepository.save(officer);
        }

        User reviewer;
        if (userRepository.findByUsername("reviewer_patel").isEmpty()) {
            reviewer = new User();
            reviewer.setUsername("reviewer_patel");
            reviewer.setEmail("a.patel@complymind.ai");
            reviewer.setFullName("Er. Anand Patel (Technical Reviewer)");
            reviewer.setPasswordHash(passwordEncoder.encode("Password123!"));
            reviewer.setRole(UserRole.REVIEWER);
            reviewer.setEnabled(true);
            reviewer = userRepository.save(reviewer);
        } else {
            reviewer = userRepository.findByUsername("reviewer_patel").get();
        }

        // 3. Synthetic Demo Framework
        ComplianceFramework framework = new ComplianceFramework();
        framework.setCode("AICTE-APH-DEMO-2627");
        framework.setName("AICTE Approval Process Handbook (Synthetic Demo Model 2026-27)");
        framework.setCycleYear("2026-2027");
        framework.setDescription("Synthetic demonstration framework modeling land, building, safety, and EoA approval dependencies. Labelled as synthetic demonstration data.");
        framework.setSourceUrl("https://www.aicte-india.org/bureaus/approval/approval-process-handbook");
        framework.setVerified(false);
        framework.setActive(true);
        framework.setDemo(true);
        framework = frameworkRepository.save(framework);

        // 4. Synthetic Demo Rules
        ComplianceRule r1 = new ComplianceRule();
        r1.setFramework(framework);
        r1.setRuleCode("RULE-LND-01");
        r1.setName("Land Ownership & Registered Title Deed Verification");
        r1.setDescription("Land must be in undisputed possession of the trust/society via registered sale deed or 30+ year lease registered with the Sub-Registrar.");
        r1.setSourceSection("Section 1.3");
        r1.setSourceUrl("https://www.aicte-india.org/rules/demo-land");
        r1.setApplicability("All technical institutions seeking approval/extension");
        r1.setRequiredEvidenceType("Registered Sale Deed / Lease Deed PDF");
        r1.setValidationMethod("Sub-Registrar Stamp, Deed Number, Area Verification");
        r1.setVersion("1.0");
        r1.setVerified(false);
        r1.setActive(true);
        r1.setDemo(true);
        r1.setReviewerNotes("SYNTHETIC DEMO RULE: Verification required by legal cell.");
        ruleRepository.save(r1);

        ComplianceRule r2 = new ComplianceRule();
        r2.setFramework(framework);
        r2.setRuleCode("RULE-BLD-02");
        r2.setName("Building Plan Sanction & Structural Safety");
        r2.setDescription("Building plan must be sanctioned by Town Planning Authority or Municipal Corporation with earmarked educational usage.");
        r2.setSourceSection("Section 2.1");
        r2.setApplicability("Institutional buildings");
        r2.setRequiredEvidenceType("Sanctioned Blueprint & Structural Certificate");
        r2.setValidationMethod("Authority Seal, Municipal Sanction Number");
        r2.setVersion("1.0");
        r2.setVerified(false);
        r2.setActive(true);
        r2.setDemo(true);
        ruleRepository.save(r2);

        // 5. Master Requirements
        Requirement req1 = createReq(framework, "REQ-01", "Land Title Deed & Ownership Registry", "Infrastructure", 30);
        Requirement req2 = createReq(framework, "REQ-02", "Sanctioned Building Plan Approval", "Infrastructure", 30);
        Requirement req3 = createReq(framework, "REQ-03", "Occupancy Certificate & Structural Stability", "Safety & Occupancy", 25);
        Requirement req4 = createReq(framework, "REQ-04", "Fire and Life Safety NOC from State Authority", "Safety", 15);
        Requirement req5 = createReq(framework, "REQ-05", "Final AICTE Portal EoA Clearance Dossier", "Regulatory Approval", 10);

        // 6. Dependencies (The DAG)
        createDep(req1, req2, DependencyType.HARD_BLOCKER, "Building Plan approval legally requires clear Land Title possession");
        createDep(req2, req3, DependencyType.HARD_BLOCKER, "Occupancy certificate can only be issued against sanctioned building plan");
        createDep(req3, req4, DependencyType.PREREQUISITE, "Fire inspection requires structural completion and occupancy compliance");
        createDep(req4, req5, DependencyType.HARD_BLOCKER, "Final AICTE approval dossier strictly blocked without valid Fire NOC");

        // 7. Compliance Application
        ComplianceApplication app = new ComplianceApplication();
        app.setApplicationNumber("APP-NIST-2026-DEMO");
        app.setInstitution(inst);
        app.setFramework(framework);
        app.setTitle("Annual AICTE Extension of Approval (EoA) 2026-27");
        app.setAcademicYear("2026-2027");
        app.setStatus(ApplicationStatus.ACTION_REQUIRED);
        app.setSubmittedAt(now.minusDays(5));
        app.setDemo(true);
        app = applicationRepository.save(app);

        // 8. Application Requirements
        ApplicationRequirement ar1 = createAppReq(app, req1, RequirementStatus.REJECTED, now.plusDays(10), "REJECTED: Sub-registrar registration seal is unreadable in uploaded copy.");
        ApplicationRequirement ar2 = createAppReq(app, req2, RequirementStatus.PENDING, now.plusDays(14), null);
        ApplicationRequirement ar3 = createAppReq(app, req3, RequirementStatus.PENDING, now.plusDays(20), null);
        ApplicationRequirement ar4 = createAppReq(app, req4, RequirementStatus.PENDING, now.plusDays(25), null);
        ApplicationRequirement ar5 = createAppReq(app, req5, RequirementStatus.PENDING, now.plusDays(30), null);

        // 9. Evidence Document for REQ-01 (Simulating the rejected upload)
        EvidenceDocument ev = new EvidenceDocument();
        ev.setApplication(app);
        ev.setApplicationRequirement(ar1);
        ev.setOriginalFilename("NIST_Land_Deed_Registry_Scan.pdf");
        ev.setStoredFilename("demo-nist-land-deed.pdf");
        ev.setFileSize(2048500L);
        ev.setMimeType("application/pdf");
        ev.setSha256Checksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        ev.setVersionNumber(1);
        ev.setReviewStatus(ReviewStatus.REJECTED);
        ev.setExtractedText("Government of Maharashtra Land Records. Survey No. 44/2. Title Deed. [STAMP DAMAGED / UNREADABLE]");
        ev.setExtractionStatus("SUCCESS");
        ev.setUploadedBy(reviewer);
        ev.setDemo(true);
        evidenceRepository.save(ev);

        // 10. Corrective Action Task
        CorrectiveAction action = new CorrectiveAction();
        action.setApplication(app);
        action.setApplicationRequirement(ar1);
        action.setTitle("Obtain Certified High-Resolution Copy of Land Deed");
        action.setDescription("Procure re-certified clear copy of Land Title Deed with clear Sub-Registrar stamp from the Pune Taluka Registry Office and re-upload.");
        action.setRootIssue("REQ-01 Land Title Deed rejected due to illegible registration stamp.");
        action.setAssignedTo(reviewer);
        action.setDueDate(now.plusDays(7));
        action.setPriority(Priority.CRITICAL);
        action.setStatus(ActionStatus.OPEN);
        action.setDemo(true);
        correctiveActionRepository.save(action);

        // 11. Run the Compliance Ripple Engine to ripple downstream blockers!
        rippleEngineService.recalculateAndPersist(app.getId());

        // 12. Run the Risk Engine to compute explainable risk!
        riskEngineService.evaluateAndPersist(app.getId());

        auditService.recordEvent("DEMO_DATA_LOADED", "System", app.getId().toString(),
                "Loaded synthetic demo dataset for jury demonstration: APP-NIST-2026-DEMO");
    }

    private Requirement createReq(ComplianceFramework framework, String code, String title, String category, int days) {
        Requirement r = new Requirement();
        r.setFramework(framework);
        r.setCode(code);
        r.setTitle(title);
        r.setDescription("Mandatory compliance verification for " + title);
        r.setCategory(category);
        r.setMandatory(true);
        r.setDefaultDeadlineDays(days);
        r.setDemo(true);
        return requirementRepository.save(r);
    }

    private void createDep(Requirement src, Requirement tgt, DependencyType type, String desc) {
        RequirementDependency dep = new RequirementDependency();
        dep.setSourceRequirement(src);
        dep.setDependentRequirement(tgt);
        dep.setDependencyType(type);
        dep.setDescription(desc);
        dep.setDemo(true);
        dependencyRepository.save(dep);
    }

    private ApplicationRequirement createAppReq(ComplianceApplication app, Requirement req, RequirementStatus status, LocalDateTime deadline, String notes) {
        ApplicationRequirement ar = new ApplicationRequirement();
        ar.setApplication(app);
        ar.setRequirement(req);
        ar.setStatus(status);
        ar.setDeadline(deadline);
        ar.setComplianceNotes(notes);
        ar.setDemo(true);
        return appRequirementRepository.save(ar);
    }

    @Transactional
    public void removeDemoDataset() {
        // Delete in safe FK dependency order to avoid constraint violations.
        // Step 1: corrective actions reference applications and users
        correctiveActionRepository.deleteByIsDemoTrue();

        // Step 2: notifications linked to demo institutions (ON DELETE CASCADE in DB schema,
        // but explicit delete here ensures the cache is cleared and avoids lazy-load surprises)
        institutionRepository.findByIsDemo(true).forEach(inst ->
                notificationRepository.deleteByInstitutionId(inst.getId())
        );

        // Step 3: evidence links to application_requirements and applications
        evidenceRepository.deleteByIsDemoTrue();

        // Step 4: application_requirements link applications to requirements
        appRequirementRepository.deleteByIsDemoTrue();

        // Step 5: applications — risk_assessments will cascade-delete in DB (ON DELETE CASCADE)
        applicationRepository.deleteByIsDemoTrue();

        // Step 6: requirement dependency edges (before requirements)
        dependencyRepository.deleteByIsDemoTrue();

        // Step 7: master requirements under framework
        requirementRepository.deleteByIsDemoTrue();

        // Step 8: compliance rules under framework
        ruleRepository.deleteByIsDemoTrue();

        // Step 9: frameworks
        frameworkRepository.deleteByIsDemoTrue();

        // Step 10: demo users — bulk native delete by known username list;
        // must come before institution delete (users.institution_id FK is ON DELETE SET NULL,
        // but deleting through Hibernate entity lifecycle after institution is gone
        // can cause LazyInitializationExceptions on the institution proxy).
        userRepository.deleteByUsernameIn(
                java.util.List.of("inst_nist", "officer_sharma", "reviewer_patel")
        );

        // Step 11: institutions — now safe (no referencing demo users or applications remain)
        institutionRepository.deleteByIsDemoTrue();

        auditService.recordEvent("DEMO_DATA_REMOVED", "System", "0",
                "Removed all synthetic demo dataset records.");
    }
}
