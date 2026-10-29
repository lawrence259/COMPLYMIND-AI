package com.complymind;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.dashboard.model.DashboardStats;
import com.complymind.dashboard.service.DashboardService;
import com.complymind.demo.service.DemoDatasetService;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.repository.ComplianceFrameworkRepository;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.entity.InstitutionType;
import com.complymind.institution.repository.InstitutionRepository;
import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import com.complymind.user.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComplyMindApplicationTests {

    private static ConfigurableApplicationContext ctx;
    private static DashboardService dashboardService;
    private static UserService userService;
    private static DemoDatasetService demoDatasetService;
    private static InstitutionRepository institutionRepository;
    private static ComplianceFrameworkRepository frameworkRepository;
    private static ComplianceApplicationRepository applicationRepository;

    @BeforeAll
    static void init() {
        ctx = SpringApplication.run(
                ComplyMindApplication.class,
                "--spring.main.web-application-type=none"
        );
        dashboardService = ctx.getBean(DashboardService.class);
        userService = ctx.getBean(UserService.class);
        demoDatasetService = ctx.getBean(DemoDatasetService.class);
        institutionRepository = ctx.getBean(InstitutionRepository.class);
        frameworkRepository = ctx.getBean(ComplianceFrameworkRepository.class);
        applicationRepository = ctx.getBean(ComplianceApplicationRepository.class);
    }

    @AfterAll
    static void tearDown() {
        if (ctx != null && ctx.isActive()) {
            ctx.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Context & Flyway: Application context starts and schema initializes")
    void contextLoads() {
        assertNotNull(dashboardService);
        assertNotNull(userService);
        assertNotNull(demoDatasetService);
    }

    @Test
    @Order(2)
    @DisplayName("Empty Database: Dashboard reports 0 applications without fabrication")
    void testEmptyDatabaseDashboard() {
        DashboardStats stats = dashboardService.getDashboardStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getTotalApplications(), "Initial total applications must be exactly 0");
        assertTrue(stats.isEmpty(), "Initial stats should report empty state");
    }

    @Test
    @Order(3)
    @DisplayName("First-Time Admin Setup: Creates administrator securely")
    void testInitialAdminSetup() {
        assertEquals(0, userService.getUserCount());

        User admin = userService.createInitialAdmin("superadmin", "superadmin@complymind.ai", "Secret123!", "Lead Administrator");
        assertNotNull(admin.getId());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals("superadmin", admin.getUsername());

        // Verify duplicate creation throws exception
        assertThrows(Exception.class, () ->
                userService.createInitialAdmin("another", "another@complymind.ai", "Secret123!", "Another Admin")
        );
    }

    @Test
    @Order(4)
    @DisplayName("Synthetic Demo Dataset: Load, ripple calculation, and safe cleanup")
    void testDemoDatasetLifecycle() {
        // 1. Initially empty applications
        assertEquals(0, dashboardService.getDashboardStatistics().getTotalApplications());

        // 2. Load demo dataset
        demoDatasetService.loadDemoDataset();

        DashboardStats demoStats = dashboardService.getDashboardStatistics();
        assertFalse(demoStats.isEmpty(), "Dashboard should now reflect loaded demo dataset");
        assertTrue(demoStats.getTotalApplications() > 0, "Demo application should be recorded in DB");
        assertTrue(demoStats.getBlockedRequirementsCount() > 0, "Downstream ripple engine should have blocked requirements");

        // 3. Remove demo dataset
        demoDatasetService.removeDemoDataset();

        DashboardStats postCleanStats = dashboardService.getDashboardStatistics();
        assertEquals(0, postCleanStats.getTotalApplications(), "All demo records should be cleanly removed");
    }

    @Test
    @Order(5)
    @DisplayName("Genuine Data Isolation: Real institution and application persist untouched across demo load/remove")
    void testGenuineDataPersistsAcrossDemoLifecycle() {
        // 1. Create genuine institution (isDemo = false)
        Institution genuineInst = new Institution();
        genuineInst.setCode("INST-GENUINE-001");
        genuineInst.setName("Indian Institute of Engineering & Technology");
        genuineInst.setType(InstitutionType.ENGINEERING_AND_TECHNOLOGY);
        genuineInst.setState("Delhi");
        genuineInst.setCity("New Delhi");
        genuineInst.setDemo(false);
        genuineInst = institutionRepository.save(genuineInst);
        assertNotNull(genuineInst.getId());

        // 2. Create genuine framework (isDemo = false)
        ComplianceFramework genuineFw = new ComplianceFramework();
        genuineFw.setCode("AICTE-GENUINE-2026");
        genuineFw.setName("Official AICTE Norms 2026");
        genuineFw.setCycleYear("2026-2027");
        genuineFw.setDemo(false);
        genuineFw = frameworkRepository.save(genuineFw);
        assertNotNull(genuineFw.getId());

        // 3. Create genuine application (isDemo = false)
        ComplianceApplication genuineApp = new ComplianceApplication();
        genuineApp.setApplicationNumber("APP-GENUINE-2026-001");
        genuineApp.setInstitution(genuineInst);
        genuineApp.setFramework(genuineFw);
        genuineApp.setTitle("Annual Approval Extension 2026-27");
        genuineApp.setAcademicYear("2026-2027");
        genuineApp.setStatus(ApplicationStatus.DRAFT);
        genuineApp.setDemo(false);
        genuineApp = applicationRepository.save(genuineApp);
        assertNotNull(genuineApp.getId());

        // Dashboard now reflects exactly 1 genuine application
        assertEquals(1, dashboardService.getDashboardStatistics().getTotalApplications());

        // 4. Load demo dataset -> total should now be 2 (1 genuine + 1 demo)
        demoDatasetService.loadDemoDataset();
        assertEquals(2, dashboardService.getDashboardStatistics().getTotalApplications());

        // 5. Remove demo dataset -> demo is cleaned up, genuine application MUST remain
        demoDatasetService.removeDemoDataset();

        DashboardStats postCleanStats = dashboardService.getDashboardStatistics();
        assertEquals(1, postCleanStats.getTotalApplications(), "Genuine application must survive demo cleanup");

        // Verify genuine application entity still in database with all its fields intact
        assertTrue(applicationRepository.existsByApplicationNumber("APP-GENUINE-2026-001"));
        ComplianceApplication loaded = applicationRepository.findByApplicationNumber("APP-GENUINE-2026-001").orElseThrow();
        assertEquals("Annual Approval Extension 2026-27", loaded.getTitle());
        assertFalse(loaded.isDemo(), "Genuine application must not be flagged as demo");
        assertEquals(genuineInst.getId(), loaded.getInstitution().getId(), "Genuine application must point to genuine institution");

        // Verify genuine institution is also intact in database
        assertTrue(institutionRepository.existsByCode("INST-GENUINE-001"));
        Institution loadedInst = institutionRepository.findByCode("INST-GENUINE-001").orElseThrow();
        assertEquals("Indian Institute of Engineering & Technology", loadedInst.getName());
        assertFalse(loadedInst.isDemo());

        // Clean up genuine records at end of test to keep database clean
        applicationRepository.delete(loaded);
        frameworkRepository.delete(genuineFw);
        institutionRepository.delete(loadedInst);
        assertEquals(0, dashboardService.getDashboardStatistics().getTotalApplications());
    }
}
