package com.complymind;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.correctiveaction.entity.ActionStatus;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import com.complymind.correctiveaction.entity.Priority;
import com.complymind.correctiveaction.service.CorrectiveActionService;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.entity.ReviewStatus;
import com.complymind.evidence.service.EvidenceReviewService;
import com.complymind.evidence.service.EvidenceStorageService;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.entity.ComplianceRule;
import com.complymind.framework.repository.ComplianceFrameworkRepository;
import com.complymind.framework.repository.ComplianceRuleRepository;
import com.complymind.ingestion.model.DataClassification;
import com.complymind.ingestion.model.DataIngestionResult;
import com.complymind.ingestion.service.PublicDataIngestionService;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.entity.InstitutionType;
import com.complymind.institution.repository.InstitutionRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.requirement.repository.RequirementRepository;
import com.complymind.ripple.entity.DependencyType;
import com.complymind.ripple.entity.RequirementDependency;
import com.complymind.ripple.model.DependencyGraph;
import com.complymind.ripple.model.RippleAnalysisResult;
import com.complymind.ripple.repository.RequirementDependencyRepository;
import com.complymind.ripple.service.RippleEngineService;
import com.complymind.risk.entity.RiskAssessment;
import com.complymind.risk.entity.RiskBand;
import com.complymind.risk.model.RiskBreakdown;
import com.complymind.risk.model.RootCauseGroup;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.risk.service.RootCauseInvestigatorService;
import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import com.complymind.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullWorkflowIntegrationTests {

    private static ConfigurableApplicationContext ctx;

    private static InstitutionRepository institutionRepo;
    private static ComplianceFrameworkRepository frameworkRepo;
    private static RequirementRepository requirementRepo;
    private static RequirementDependencyRepository dependencyRepo;
    private static ComplianceApplicationRepository appRepo;
    private static ApplicationRequirementRepository appReqRepo;
    private static UserService userService;
    private static ComplianceApplicationService appService;
    private static EvidenceStorageService storageService;
    private static EvidenceReviewService evidenceService;
    private static RippleEngineService rippleService;
    private static RiskEngineService riskService;
    private static RootCauseInvestigatorService rootCauseService;
    private static CorrectiveActionService correctiveActionService;
    private static PublicDataIngestionService ingestionService;

    private static Institution testInstitution;
    private static ComplianceFramework testFramework;
    private static User testAdmin;

    @BeforeAll
    static void init() {
        ctx = SpringApplication.run(
                ComplyMindApplication.class,
                "--spring.main.web-application-type=none"
        );
        institutionRepo = ctx.getBean(InstitutionRepository.class);
        frameworkRepo = ctx.getBean(ComplianceFrameworkRepository.class);
        requirementRepo = ctx.getBean(RequirementRepository.class);
        dependencyRepo = ctx.getBean(RequirementDependencyRepository.class);
        appRepo = ctx.getBean(ComplianceApplicationRepository.class);
        appReqRepo = ctx.getBean(ApplicationRequirementRepository.class);
        userService = ctx.getBean(UserService.class);
        appService = ctx.getBean(ComplianceApplicationService.class);
        storageService = ctx.getBean(EvidenceStorageService.class);
        evidenceService = ctx.getBean(EvidenceReviewService.class);
        rippleService = ctx.getBean(RippleEngineService.class);
        riskService = ctx.getBean(RiskEngineService.class);
        rootCauseService = ctx.getBean(RootCauseInvestigatorService.class);
        correctiveActionService = ctx.getBean(CorrectiveActionService.class);
        ingestionService = ctx.getBean(PublicDataIngestionService.class);

        // Seed test admin
        if (userService.getUserCount() == 0) {
            testAdmin = userService.createInitialAdmin("qa_tester", "qa@complymind.ai", "Secret123!", "QA Engineer");
        } else {
            testAdmin = userService.getAllUsers().get(0);
        }

        // Authenticate in SecurityContext
        com.complymind.security.CustomUserDetails userDetails = new com.complymind.security.CustomUserDetails(testAdmin);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        // Seed test institution
        testInstitution = new Institution();
        testInstitution.setCode("TST-INST-999");
        testInstitution.setName("National Institute of Technical Testing");
        testInstitution.setType(InstitutionType.ENGINEERING_AND_TECHNOLOGY);
        testInstitution.setState("Karnataka");
        testInstitution.setCity("Bengaluru");
        testInstitution.setDemo(false);
        testInstitution = institutionRepo.save(testInstitution);

        // Seed test framework with 3 chained requirements
        testFramework = new ComplianceFramework();
        testFramework.setCode("TST-FW-2026");
        testFramework.setName("Test Regulatory Framework");
        testFramework.setCycleYear("2026-2027");
        testFramework.setDemo(false);
        testFramework = frameworkRepo.save(testFramework);

        Requirement r1 = new Requirement();
        r1.setFramework(testFramework);
        r1.setCode("TST-REQ-01");
        r1.setTitle("Land Ownership Title Deed");
        r1.setCategory("INFRASTRUCTURE");
        r1.setMandatory(true);
        r1.setDefaultDeadlineDays(30);
        r1.setDemo(false);
        r1 = requirementRepo.save(r1);

        Requirement r2 = new Requirement();
        r2.setFramework(testFramework);
        r2.setCode("TST-REQ-02");
        r2.setTitle("Building Safety Certificate");
        r2.setCategory("INFRASTRUCTURE");
        r2.setMandatory(true);
        r2.setDefaultDeadlineDays(45);
        r2.setDemo(false);
        r2 = requirementRepo.save(r2);

        Requirement r3 = new Requirement();
        r3.setFramework(testFramework);
        r3.setCode("TST-REQ-03");
        r3.setTitle("Fire Safety Clearance NOC");
        r3.setCategory("SAFETY");
        r3.setMandatory(true);
        r3.setDefaultDeadlineDays(60);
        r3.setDemo(false);
        r3 = requirementRepo.save(r3);

        // Dependency chain: REQ-01 -> REQ-02 (HARD_BLOCKER), REQ-02 -> REQ-03 (HARD_BLOCKER)
        RequirementDependency dep1 = new RequirementDependency();
        dep1.setSourceRequirement(r1);
        dep1.setDependentRequirement(r2);
        dep1.setDependencyType(DependencyType.HARD_BLOCKER);
        dep1.setDescription("Building safety requires valid land ownership title.");
        dep1.setDemo(false);
        dependencyRepo.save(dep1);

        RequirementDependency dep2 = new RequirementDependency();
        dep2.setSourceRequirement(r2);
        dep2.setDependentRequirement(r3);
        dep2.setDependencyType(DependencyType.HARD_BLOCKER);
        dep2.setDescription("Fire safety inspection requires certified building plan.");
        dep2.setDemo(false);
        dependencyRepo.save(dep2);
    }

    @AfterAll
    static void tearDown() {
        if (ctx != null && ctx.isActive()) {
            ctx.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("A. Register Application: Full validation, unique ID, and auto-attached requirements")
    void testRegisterApplication() {
        // 1. Validation: blank title throws BusinessValidationException
        assertThrows(BusinessValidationException.class, () ->
                appService.createApplication(testInstitution.getId(), testFramework.getId(), "   ", "2026-2027", testAdmin)
        );

        // 2. Validation: invalid institution throws ResourceNotFoundException
        assertThrows(Exception.class, () ->
                appService.createApplication(999999L, testFramework.getId(), "Valid Title", "2026-2027", testAdmin)
        );

        // 3. Successful registration
        ComplianceApplication app = appService.createApplication(
                testInstitution.getId(),
                testFramework.getId(),
                "Annual Technical Approval 2026-27",
                "2026-2027",
                testAdmin
        );

        assertNotNull(app.getId());
        assertNotNull(app.getApplicationNumber());
        assertTrue(app.getApplicationNumber().startsWith("APP-TST-INST-999-"));
        assertEquals(ApplicationStatus.DRAFT, app.getStatus());

        // Verify requirements auto-attached
        List<ApplicationRequirement> reqs = appReqRepo.findByApplicationId(app.getId());
        assertEquals(3, reqs.size(), "All 3 framework requirements must be auto-attached");

        // Verify initial risk calculated
        assertNotNull(app.getRiskScore());
        assertNotNull(app.getRiskBand());
    }

    @Test
    @Order(2)
    @DisplayName("E. Evidence Management: Validate file signatures, Tika extraction, and private storage")
    void testEvidenceLifecycle() {
        List<ComplianceApplication> apps = appRepo.findByInstitutionId(testInstitution.getId());
        assertFalse(apps.isEmpty());
        ComplianceApplication app = apps.get(0);
        List<ApplicationRequirement> reqs = appReqRepo.findByApplicationId(app.getId());
        ApplicationRequirement landReq = reqs.stream()
                .filter(r -> r.getRequirement().getCode().equals("TST-REQ-01"))
                .findFirst().orElseThrow();

        // 1. Invalid file: corrupted / fake PDF header throws BusinessValidationException
        byte[] fakePdfBytes = "THIS IS NOT A VALID PDF FILE".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fakeFile = new MockMultipartFile("file", "fake.pdf", "application/pdf", fakePdfBytes);
        assertThrows(BusinessValidationException.class, () ->
                storageService.storeFile(fakeFile)
        );

        // 2. Valid PDF upload with true %PDF magic bytes
        byte[] validPdfBytes = "%PDF-1.4\n1 0 obj\n<< /Title (Land Title Deed) >>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile validFile = new MockMultipartFile("file", "land_deed.pdf", "application/pdf", validPdfBytes);

        EvidenceDocument doc = evidenceService.uploadEvidence(app.getId(), landReq.getId(), validFile, testAdmin);
        assertNotNull(doc.getId());
        assertEquals("land_deed.pdf", doc.getOriginalFilename());
        assertEquals(1, doc.getVersionNumber());
        assertEquals(ReviewStatus.PENDING, doc.getReviewStatus());
        assertNotNull(doc.getSha256Checksum());

        // Requirement should transition to UNDER_REVIEW
        ApplicationRequirement reloadedReq = appReqRepo.findById(landReq.getId()).orElseThrow();
        assertEquals(RequirementStatus.UNDER_REVIEW, reloadedReq.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("B. Compliance Dependency Graph: Propagate downstream blockers via BFS")
    void testRippleEnginePropagation() {
        List<ComplianceApplication> apps = appRepo.findByInstitutionId(testInstitution.getId());
        ComplianceApplication app = apps.get(0);
        List<ApplicationRequirement> reqs = appReqRepo.findByApplicationId(app.getId());
        ApplicationRequirement landReq = reqs.stream()
                .filter(r -> r.getRequirement().getCode().equals("TST-REQ-01"))
                .findFirst().orElseThrow();

        // Reviewer REJECTS land requirement (root failure)
        List<EvidenceDocument> evs = evidenceService.getEvidenceForRequirement(landReq.getId());
        assertFalse(evs.isEmpty());
        evidenceService.recordDecision(evs.get(0).getId(), ReviewStatus.REJECTED, "Land deed lacks encumbrance clearance certificate", testAdmin);

        // Reload requirements
        ApplicationRequirement reloadedLand = appReqRepo.findById(landReq.getId()).orElseThrow();
        assertEquals(RequirementStatus.REJECTED, reloadedLand.getStatus());

        // Run Ripple propagation
        RippleAnalysisResult ripple = rippleService.recalculateAndPersist(app.getId());

        // TST-REQ-01 is rejected -> TST-REQ-02 must be BLOCKED -> TST-REQ-03 must be BLOCKED
        List<ApplicationRequirement> afterRippleReqs = appReqRepo.findByApplicationId(app.getId());
        ApplicationRequirement req2 = afterRippleReqs.stream().filter(r -> r.getRequirement().getCode().equals("TST-REQ-02")).findFirst().orElseThrow();
        ApplicationRequirement req3 = afterRippleReqs.stream().filter(r -> r.getRequirement().getCode().equals("TST-REQ-03")).findFirst().orElseThrow();

        assertEquals(RequirementStatus.BLOCKED, req2.getStatus(), "TST-REQ-02 must be BLOCKED downstream");
        assertEquals(RequirementStatus.BLOCKED, req3.getStatus(), "TST-REQ-03 must be BLOCKED downstream");

        // Verify Cytoscape graph model
        DependencyGraph graph = rippleService.buildGraphForApplication(app.getId());
        assertTrue(graph.getRootIssues().contains("TST-REQ-01"));
        assertFalse(graph.isHasCycle());
    }

    @Test
    @Order(4)
    @DisplayName("D. Root Cause Investigator: Identify originating blocker and propose corrective action")
    void testRootCauseInvestigation() {
        List<ComplianceApplication> apps = appRepo.findByInstitutionId(testInstitution.getId());
        ComplianceApplication app = apps.get(0);

        List<RootCauseGroup> rootCauses = rootCauseService.investigateRootCauses(app.getId());
        assertFalse(rootCauses.isEmpty(), "Root causes must be detected for rejected land requirement");

        RootCauseGroup root = rootCauses.get(0);
        assertEquals("TST-REQ-01", root.getRootRequirementCode());
        assertEquals("Land Ownership Title Deed", root.getRootRequirementTitle());
        assertTrue(root.getAffectedRequirementsCount() >= 2, "Must identify downstream blocked requirements");
        assertNotNull(root.getSuggestedCorrectiveAction());
    }

    @Test
    @Order(5)
    @DisplayName("C. Risk Engine: Deterministic score responds to blocked dependencies")
    void testRiskEngineEvaluation() {
        List<ComplianceApplication> apps = appRepo.findByInstitutionId(testInstitution.getId());
        ComplianceApplication app = apps.get(0);

        RiskBreakdown breakdown = riskService.calculateRisk(app.getId());
        assertNotNull(breakdown);
        assertTrue(breakdown.getOverallScore() > 0.0);
        assertTrue(breakdown.getBlockedDependencyScore() > 0.0, "Blocked dependency component must be elevated");
        assertFalse(breakdown.getContributingFactors().isEmpty());

        RiskAssessment assessment = riskService.evaluateAndPersist(app.getId());
        assertNotNull(assessment.getId());
        assertEquals(assessment.getOverallScore(), appRepo.findById(app.getId()).orElseThrow().getRiskScore());
    }

    @Test
    @Order(6)
    @DisplayName("G. Corrective Actions: Resolution unblocks downstream requirements and recalculates risk")
    void testCorrectiveActionResolutionWorkflow() {
        List<ComplianceApplication> apps = appRepo.findByInstitutionId(testInstitution.getId());
        ComplianceApplication app = apps.get(0);

        // 1. Create corrective action
        CorrectiveAction action = correctiveActionService.createAction(
                app.getId(),
                null,
                "Acquire Revenue Dept Encumbrance Certificate",
                "Submit certified copy of 30-year non-encumbrance certificate.",
                "TST-REQ-01: Land Ownership Title Deed",
                testAdmin.getId(),
                LocalDateTime.now().plusDays(10),
                Priority.CRITICAL
        );

        assertNotNull(action.getId());
        assertEquals(ActionStatus.OPEN, action.getStatus());

        // 2. Advance to IN_PROGRESS
        correctiveActionService.updateStatus(action.getId(), ActionStatus.IN_PROGRESS, "Applied to Sub-Registrar Office");

        // 3. Submit for verification
        correctiveActionService.updateStatus(action.getId(), ActionStatus.SUBMITTED_FOR_VERIFICATION, "Revenue certificate received and uploaded");

        // 4. Verify action
        CorrectiveAction verified = correctiveActionService.verifyAction(action.getId(), testAdmin, true, "Verified authentic revenue seal");
        assertEquals(ActionStatus.VERIFIED, verified.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("F. Public Data Ingestion: Upsert public catalog with source attribution and duplicate detection")
    void testPublicDataIngestion() {
        // 1. Ingest official AICTE handbook statutory catalog
        DataIngestionResult catalogResult = ingestionService.ingestVerifiedAictePublicCatalog();
        assertNotNull(catalogResult);
        assertEquals(DataClassification.OFFICIAL_SOURCE_VERIFIED, catalogResult.getClassification());
        assertTrue(catalogResult.getTotalRecords() >= 10);
        assertTrue(catalogResult.getAddedCount() + catalogResult.getUpdatedCount() >= 10);

        // 2. Ingest CSV dataset with duplicate detection
        String csvContent = "code,name,type,aictePermanentId,state,city\n" +
                "TST-INGEST-01,National Polytechnic of Engineering,ENGINEERING_AND_TECHNOLOGY,1-99887766,Maharashtra,Pune\n" +
                "TST-INGEST-01,National Polytechnic of Engineering (Updated),ENGINEERING_AND_TECHNOLOGY,1-99887766,Maharashtra,Pune\n";

        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "institutions.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        DataIngestionResult csvResult = ingestionService.ingestInstitutionsFromCsv(
                csvFile,
                "https://data.gov.in/dataset/technical-colleges",
                DataClassification.PUBLIC_SOURCE_UNVERIFIED
        );

        assertEquals(2, csvResult.getTotalRecords());
        assertEquals(1, csvResult.getAddedCount());
        assertEquals(1, csvResult.getUpdatedCount(), "Duplicate code must trigger upsert update rather than blind insertion");
        assertTrue(institutionRepo.existsByCode("TST-INGEST-01"));

        Institution loaded = institutionRepo.findByCode("TST-INGEST-01").orElseThrow();
        assertEquals("National Polytechnic of Engineering (Updated)", loaded.getName());
    }
}
