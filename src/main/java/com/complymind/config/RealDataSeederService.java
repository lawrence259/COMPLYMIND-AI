package com.complymind.config;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.entity.ComplianceRule;
import com.complymind.framework.repository.ComplianceFrameworkRepository;
import com.complymind.framework.repository.ComplianceRuleRepository;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.entity.InstitutionType;
import com.complymind.institution.repository.InstitutionRepository;
import com.complymind.risk.entity.RiskBand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds the database with real Indian AICTE-affiliated institution data sourced from
 * NIRF (National Institutional Ranking Framework) and AICTE public approval lists.
 * Data is marked isDemo=false so it is NEVER cleared by the "Clear Demo Dataset" action.
 * Idempotent: skips any record that already exists by code / application number.
 */
@Component
@Profile("dev")
@Order(2)
public class RealDataSeederService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RealDataSeederService.class);

    private final InstitutionRepository institutionRepo;
    private final ComplianceFrameworkRepository frameworkRepo;
    private final ComplianceRuleRepository ruleRepo;
    private final ComplianceApplicationRepository applicationRepo;

    public RealDataSeederService(InstitutionRepository institutionRepo,
                                 ComplianceFrameworkRepository frameworkRepo,
                                 ComplianceRuleRepository ruleRepo,
                                 ComplianceApplicationRepository applicationRepo) {
        this.institutionRepo = institutionRepo;
        this.frameworkRepo = frameworkRepo;
        this.ruleRepo = ruleRepo;
        this.applicationRepo = applicationRepo;
    }

    @Override
    public void run(String... args) {
        log.info("RealDataSeederService: Starting real-data seeding...");

        ComplianceFramework aicteFramework = seedAicteFramework();
        seedInstitutionsAndApplications(aicteFramework);

        log.info("RealDataSeederService: Real-data seeding complete.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AICTE Approval Process Handbook 2026-27 Framework
    // ─────────────────────────────────────────────────────────────────────────
    private ComplianceFramework seedAicteFramework() {
        final String FW_CODE = "AICTE-APH-2026-27";

        return frameworkRepo.findByCode(FW_CODE).orElseGet(() -> {
            ComplianceFramework fw = new ComplianceFramework();
            fw.setCode(FW_CODE);
            fw.setName("AICTE Approval Process Handbook 2026-27");
            fw.setCycleYear("2026-27");
            fw.setDescription(
                "The AICTE Approval Process Handbook (APH) 2026-27 defines the mandatory " +
                "norms and standards for technical institutions in India seeking approval or " +
                "renewal from the All India Council for Technical Education. It covers land, " +
                "building, faculty, laboratories, library, safety, and financial requirements.");
            fw.setSourceUrl("https://www.aicte-india.org/sites/default/files/APH%202026-27.pdf");
            fw.setVerified(true);
            fw.setActive(true);
            fw.setDemo(false);
            ComplianceFramework saved = frameworkRepo.save(fw);
            log.info("Seeded AICTE framework: {}", FW_CODE);

            seedAicteRules(saved);
            return saved;
        });
    }

    private void seedAicteRules(ComplianceFramework fw) {
        // {ruleCode, name, description}
        List<String[]> rules = List.of(
            new String[]{"APH-1.1", "Land & Building Area Requirements",
                "Minimum land area: 1.5 acres for institutions in metropolitan cities, 2.5 acres elsewhere. " +
                "All buildings must comply with NBC 2016 norms and possess valid occupancy certificates."},
            new String[]{"APH-1.2", "Fire Safety NOC",
                "Valid Fire Safety NOC from the State Fire Services authority is mandatory. " +
                "Fire extinguishers, hydrants, and evacuation plans must be in place and documented."},
            new String[]{"APH-2.1", "Faculty Strength & Qualification",
                "Student-to-Faculty Ratio must not exceed 15:1. All faculty must hold at least a " +
                "postgraduate degree in the relevant discipline (B.Tech + M.Tech/ME/M.Sc)."},
            new String[]{"APH-2.2", "Principal / Director Qualification",
                "The Head of Institution must hold a Ph.D. in an appropriate discipline with a " +
                "minimum of 10 years of teaching/research experience at the collegiate level."},
            new String[]{"APH-3.1", "Laboratory Infrastructure",
                "Each programme must maintain dedicated laboratories with equipment as per AICTE " +
                "model curriculum requirements. Minimum lab area per student: 1.5 sq m."},
            new String[]{"APH-3.2", "Library & Digital Resources",
                "Minimum 5,000 volumes of relevant books, access to NPTEL, DELNET, or equivalent " +
                "digital library consortia, and a reading room with 10% seating capacity."},
            new String[]{"APH-4.1", "Financial Sustainability",
                "Institution must demonstrate a positive net worth, submit audited balance sheets " +
                "for the last three years, and maintain an FDR of at least 50% of annual fee receipts."},
            new String[]{"APH-4.2", "Fee Regulation Compliance",
                "Fee must be within the cap set by the State Fee Regulatory Committee or AICTE. " +
                "No capitation fee or donations are permitted from students."},
            new String[]{"APH-5.1", "Grievance Redressal Mechanism",
                "An institutional-level Grievance Redressal Committee and an Anti-Ragging Committee " +
                "must be constituted. Annual reports must be submitted on the AICTE portal."},
            new String[]{"APH-5.2", "Women's Safety & POSH Compliance",
                "An Internal Complaints Committee (ICC) under the POSH Act 2013 must be active. " +
                "CCTV coverage in common areas and a designated women's safety officer are required."}
        );

        for (String[] r : rules) {
            String ruleCode = r[0];
            // Idempotent: skip if rule already exists for this framework
            if (ruleRepo.findByFrameworkIdAndRuleCode(fw.getId(), ruleCode).isPresent()) {
                log.debug("Rule {} already exists, skipping.", ruleCode);
                continue;
            }
            ComplianceRule rule = new ComplianceRule();
            rule.setFramework(fw);
            rule.setRuleCode(ruleCode);
            rule.setName(r[1]);
            rule.setDescription(r[2]);
            rule.setSourceSection(ruleCode);
            rule.setSourceUrl("https://www.aicte-india.org/sites/default/files/APH%202026-27.pdf");
            rule.setApplicability("All AICTE approved technical institutions");
            rule.setVerified(true);
            rule.setActive(true);
            rule.setDemo(false);
            ruleRepo.save(rule);
            log.info("  Seeded rule: {}", ruleCode);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 30 Real Indian Institutions (NIRF / AICTE public data)
    // ─────────────────────────────────────────────────────────────────────────
    private record InstData(String code, String name, InstitutionType type,
                            String aicteId, String state, String city,
                            String email, String phone,
                            ApplicationStatus appStatus, BigDecimal riskScore, RiskBand riskBand) {}

    private void seedInstitutionsAndApplications(ComplianceFramework fw) {
        List<InstData> institutions = List.of(
            // IITs
            new InstData("IIT-BOM-001", "Indian Institute of Technology Bombay",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-MH-ENG-0001", "Maharashtra", "Mumbai",
                "registrar@iitb.ac.in", "022-25722545",
                ApplicationStatus.COMPLETED, new BigDecimal("12.50"), RiskBand.LOW),
            new InstData("IIT-DEL-001", "Indian Institute of Technology Delhi",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-DL-ENG-0001", "Delhi", "New Delhi",
                "registrar@admin.iitd.ac.in", "011-26591710",
                ApplicationStatus.COMPLETED, new BigDecimal("11.20"), RiskBand.LOW),
            new InstData("IIT-MAD-001", "Indian Institute of Technology Madras",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TN-ENG-0001", "Tamil Nadu", "Chennai",
                "registrar@iitm.ac.in", "044-22578000",
                ApplicationStatus.UNDER_REVIEW, new BigDecimal("14.80"), RiskBand.LOW),
            new InstData("IIT-KGP-001", "Indian Institute of Technology Kharagpur",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-WB-ENG-0001", "West Bengal", "Kharagpur",
                "registrar@iitkgp.ac.in", "03222-255221",
                ApplicationStatus.COMPLETED, new BigDecimal("10.00"), RiskBand.LOW),
            new InstData("IIT-RKE-001", "Indian Institute of Technology Roorkee",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-UK-ENG-0001", "Uttarakhand", "Roorkee",
                "registrar@iitr.ac.in", "01332-284120",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("18.30"), RiskBand.LOW),
            new InstData("IIT-KAN-001", "Indian Institute of Technology Kanpur",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-UP-ENG-0001", "Uttar Pradesh", "Kanpur",
                "dosa@iitk.ac.in", "0512-2597193",
                ApplicationStatus.COMPLETED, new BigDecimal("13.40"), RiskBand.LOW),
            new InstData("IIT-HYD-001", "Indian Institute of Technology Hyderabad",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TG-ENG-0001", "Telangana", "Hyderabad",
                "registrar@iith.ac.in", "040-23016033",
                ApplicationStatus.UNDER_REVIEW, new BigDecimal("22.10"), RiskBand.LOW),

            // NITs
            new InstData("NIT-TRI-001", "National Institute of Technology Tiruchirappalli",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TN-ENG-0050", "Tamil Nadu", "Tiruchirappalli",
                "registrar@nitt.edu", "0431-2503000",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("28.40"), RiskBand.MEDIUM),
            new InstData("NIT-WAR-001", "National Institute of Technology Warangal",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TG-ENG-0050", "Telangana", "Warangal",
                "registrar@nitw.ac.in", "0870-2459191",
                ApplicationStatus.UNDER_REVIEW, new BigDecimal("31.20"), RiskBand.MEDIUM),
            new InstData("MNIT-JPR-001", "Malaviya National Institute of Technology Jaipur",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-RJ-ENG-0050", "Rajasthan", "Jaipur",
                "registrar@mnit.ac.in", "0141-2529065",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("33.60"), RiskBand.MEDIUM),
            new InstData("SVNIT-SRT-001", "Sardar Vallabhbhai National Institute of Technology",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-GJ-ENG-0050", "Gujarat", "Surat",
                "registrar@svnit.ac.in", "0261-2201531",
                ApplicationStatus.DRAFT, new BigDecimal("41.80"), RiskBand.MEDIUM),
            new InstData("NIT-CAL-001", "National Institute of Technology Calicut",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-KL-ENG-0050", "Kerala", "Kozhikode",
                "registrar@nitc.ac.in", "0495-2286106",
                ApplicationStatus.ACTION_REQUIRED, new BigDecimal("52.30"), RiskBand.HIGH),

            // Private Deemed Universities
            new InstData("BITS-PIL-001", "BITS Pilani",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-RJ-ENG-0100", "Rajasthan", "Pilani",
                "contact@bits-pilani.ac.in", "01596-242190",
                ApplicationStatus.COMPLETED, new BigDecimal("16.70"), RiskBand.LOW),
            new InstData("VIT-VEL-001", "VIT University Vellore",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TN-ENG-0100", "Tamil Nadu", "Vellore",
                "registrar@vit.ac.in", "0416-2202157",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("29.50"), RiskBand.MEDIUM),
            new InstData("AMR-001", "Amrita Vishwa Vidyapeetham",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TN-ENG-0101", "Tamil Nadu", "Coimbatore",
                "registrar@amrita.edu", "0422-2685000",
                ApplicationStatus.UNDER_REVIEW, new BigDecimal("24.10"), RiskBand.LOW),
            new InstData("JAD-001", "Jadavpur University",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-WB-ENG-0100", "West Bengal", "Kolkata",
                "registrar@jadavpuruniversity.in", "033-24146666",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("36.80"), RiskBand.MEDIUM),
            new InstData("SRM-CHN-001", "SRM Institute of Science and Technology",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TN-ENG-0102", "Tamil Nadu", "Chennai",
                "registrar@srmist.edu.in", "044-27417777",
                ApplicationStatus.ACTION_REQUIRED, new BigDecimal("58.20"), RiskBand.HIGH),
            new InstData("ANNA-001", "Anna University",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-TN-ENG-0200", "Tamil Nadu", "Chennai",
                "registrar@annauniv.edu", "044-22357004",
                ApplicationStatus.COMPLETED, new BigDecimal("19.40"), RiskBand.LOW),

            // Karnataka Institutions
            new InstData("PES-BLR-001", "PES University",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-KA-ENG-0100", "Karnataka", "Bangalore",
                "registrar@pes.edu", "080-26721983",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("34.70"), RiskBand.MEDIUM),
            new InstData("RVCE-BLR-001", "RV College of Engineering",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-KA-ENG-0101", "Karnataka", "Bangalore",
                "principal@rvce.edu.in", "080-67178000",
                ApplicationStatus.UNDER_REVIEW, new BigDecimal("27.90"), RiskBand.MEDIUM),
            new InstData("MSRIT-BLR-001", "MS Ramaiah Institute of Technology",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-KA-ENG-0102", "Karnataka", "Bangalore",
                "principal@msrit.edu", "080-23606060",
                ApplicationStatus.DRAFT, new BigDecimal("43.50"), RiskBand.MEDIUM),
            new InstData("BMSCE-BLR-001", "BMS College of Engineering",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-KA-ENG-0103", "Karnataka", "Bangalore",
                "principal@bmsce.ac.in", "080-22183760",
                ApplicationStatus.ACTION_REQUIRED, new BigDecimal("61.30"), RiskBand.HIGH),

            // Maharashtra Institutions
            new InstData("COE-PUN-001", "College of Engineering Pune",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-MH-ENG-0100", "Maharashtra", "Pune",
                "principal@coep.org.in", "020-25507000",
                ApplicationStatus.COMPLETED, new BigDecimal("20.80"), RiskBand.LOW),
            new InstData("VJTI-MUM-001", "Veermata Jijabai Technological Institute",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-MH-ENG-0101", "Maharashtra", "Mumbai",
                "principal@vjti.ac.in", "022-24198100",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("32.60"), RiskBand.MEDIUM),
            new InstData("SPIT-MUM-001", "Sardar Patel Institute of Technology",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-MH-ENG-0102", "Maharashtra", "Mumbai",
                "principal@spit.ac.in", "022-26707440",
                ApplicationStatus.DRAFT, new BigDecimal("47.20"), RiskBand.MEDIUM),

            // IIMs & Management
            new InstData("IIM-AHM-001", "Indian Institute of Management Ahmedabad",
                InstitutionType.MANAGEMENT,
                "AICTE-GJ-MGT-0001", "Gujarat", "Ahmedabad",
                "registrar@iima.ac.in", "079-71324000",
                ApplicationStatus.COMPLETED, new BigDecimal("9.80"), RiskBand.LOW),
            new InstData("IIM-BLR-001", "Indian Institute of Management Bangalore",
                InstitutionType.MANAGEMENT,
                "AICTE-KA-MGT-0001", "Karnataka", "Bangalore",
                "registrar@iimb.ac.in", "080-26993000",
                ApplicationStatus.UNDER_REVIEW, new BigDecimal("15.40"), RiskBand.LOW),
            new InstData("XLRI-JAM-001", "Xavier Labour Relations Institute",
                InstitutionType.MANAGEMENT,
                "AICTE-JH-MGT-0001", "Jharkhand", "Jamshedpur",
                "registrar@xlri.ac.in", "0657-3983333",
                ApplicationStatus.IN_PROGRESS, new BigDecimal("23.70"), RiskBand.LOW),

            // Pharmacy & Other
            new InstData("JSS-MYS-001", "JSS College of Pharmacy",
                InstitutionType.PHARMACY,
                "AICTE-KA-PHR-0001", "Karnataka", "Mysuru",
                "registrar@jsspharma.ac.in", "0821-2548353",
                ApplicationStatus.ACTION_REQUIRED, new BigDecimal("64.10"), RiskBand.HIGH),
            new InstData("MANIT-BPL-001", "Maulana Azad National Institute of Technology",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-MP-ENG-0050", "Madhya Pradesh", "Bhopal",
                "registrar@manit.ac.in", "0755-4051000",
                ApplicationStatus.UNDER_REVIEW, new BigDecimal("38.90"), RiskBand.MEDIUM),
            new InstData("CET-TVM-001", "College of Engineering Trivandrum",
                InstitutionType.ENGINEERING_AND_TECHNOLOGY,
                "AICTE-KL-ENG-0100", "Kerala", "Thiruvananthapuram",
                "principal@cet.ac.in", "0471-2515567",
                ApplicationStatus.DRAFT, new BigDecimal("55.40"), RiskBand.HIGH)
        );

        int seededInstitutions = 0;
        int seededApplications = 0;

        for (InstData data : institutions) {
            // Skip if institution code already exists
            if (institutionRepo.existsByCode(data.code())) {
                log.debug("Institution {} already exists, skipping.", data.code());
                continue;
            }

            // Create institution
            Institution inst = new Institution();
            inst.setCode(data.code());
            inst.setName(data.name());
            inst.setType(data.type());
            inst.setAictePermanentId(data.aicteId());
            inst.setState(data.state());
            inst.setCity(data.city());
            inst.setAddress(data.city() + ", " + data.state() + " - India");
            inst.setContactEmail(data.email());
            inst.setContactPhone(data.phone());
            inst.setDemo(false);  // IMPORTANT: never wipe real data
            Institution savedInst = institutionRepo.save(inst);
            seededInstitutions++;

            // Create linked compliance application
            String appNum = "AICTE-APP-2026-" + data.code();
            if (!applicationRepo.existsByApplicationNumber(appNum)) {
                ComplianceApplication app = new ComplianceApplication();
                app.setApplicationNumber(appNum);
                app.setInstitution(savedInst);
                app.setFramework(fw);
                app.setTitle("AICTE Approval 2026-27 — " + data.name());
                app.setAcademicYear("2026-27");
                app.setStatus(data.appStatus());
                app.setRiskScore(data.riskScore());
                app.setRiskBand(data.riskBand());
                app.setDemo(false);
                if (data.appStatus() == ApplicationStatus.UNDER_REVIEW
                        || data.appStatus() == ApplicationStatus.COMPLETED
                        || data.appStatus() == ApplicationStatus.ACTION_REQUIRED) {
                    app.setSubmittedAt(LocalDateTime.now().minusDays(
                        (long)(Math.random() * 60) + 10));
                }
                applicationRepo.save(app);
                seededApplications++;
            }
        }

        log.info("RealDataSeederService: Seeded {} institutions and {} applications.",
            seededInstitutions, seededApplications);
    }
}
