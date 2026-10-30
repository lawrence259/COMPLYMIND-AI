package com.complymind.discrepancy.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.discrepancy.entity.DiscrepancyFinding;
import com.complymind.discrepancy.entity.DiscrepancyType;
import com.complymind.discrepancy.entity.FindingReviewStatus;
import com.complymind.discrepancy.entity.FindingSeverity;
import com.complymind.discrepancy.repository.DiscrepancyFindingRepository;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.repository.EvidenceDocumentRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.Requirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DiscrepancyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DiscrepancyDetectionService.class);

    private final DiscrepancyFindingRepository findingRepository;
    private final ComplianceApplicationRepository applicationRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final EvidenceDocumentRepository evidenceRepository;
    private final AuditService auditService;

    // Regulatory patterns
    private static final Pattern LAND_AREA_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:acres?|acre)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(?:valid(?:ity)?\\s+(?:till|to|up\\s+to)|expires?\\s+on|expiry\\s+date)[:\\s]*(\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern RATIO_PATTERN = Pattern.compile("(?i)(?:faculty[\\s\\-]*ratio|cadre\\s*ratio|s[:/]f\\s*ratio)[:\\s]*1[:\\s]*([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AICTE_ID_PATTERN = Pattern.compile("\\b(1-\\d{9,11})\\b");

    public DiscrepancyDetectionService(DiscrepancyFindingRepository findingRepository,
                                      ComplianceApplicationRepository applicationRepository,
                                      ApplicationRequirementRepository appRequirementRepository,
                                      EvidenceDocumentRepository evidenceRepository,
                                      AuditService auditService) {
        this.findingRepository = findingRepository;
        this.applicationRepository = applicationRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.evidenceRepository = evidenceRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DiscrepancyFinding> getFindingsForApplication(Long applicationId) {
        return findingRepository.findByApplicationIdWithDetails(applicationId);
    }

    @Transactional
    public List<DiscrepancyFinding> scanApplication(Long applicationId) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        List<ApplicationRequirement> appReqs = appRequirementRepository.findByApplicationId(applicationId);
        List<DiscrepancyFinding> newFindings = new ArrayList<>();

        for (ApplicationRequirement ar : appReqs) {
            Requirement req = ar.getRequirement();
            List<EvidenceDocument> docs = evidenceRepository.findByApplicationRequirementIdOrderByVersionNumberDesc(ar.getId());

            // 1. Check for Unknown / Missing Evidence on Mandatory Requirements
            if (docs.isEmpty()) {
                if (req.isMandatory()) {
                    DiscrepancyFinding f = new DiscrepancyFinding();
                    f.setApplication(app);
                    f.setApplicationRequirement(ar);
                    f.setFindingType(DiscrepancyType.MISSING_MANDATORY_FIELD);
                    f.setFieldName("Mandatory Evidence Portfolio");
                    f.setDeclaredValue("Required for " + req.getCode());
                    f.setExtractedValue("UNKNOWN / NO EVIDENCE FILED");
                    f.setSourceCitation("Institutional Submission Portal");
                    f.setSourcePage(1);
                    f.setConfidenceScore(BigDecimal.valueOf(100.00));
                    f.setRegulatoryReference("AICTE APH 2026-27 Ch. II & Sec. 2.4 - Mandatory Evidentiary Proof");
                    f.setSeverity(FindingSeverity.CRITICAL);
                    f.setReviewStatus(FindingReviewStatus.PENDING_REVIEW);
                    f.setReviewerNotes("Mandatory regulatory evidence is missing. Rule cannot be evaluated as PASS.");
                    newFindings.add(f);
                }
                continue;
            }

            EvidenceDocument latestDoc = docs.get(0);
            String text = latestDoc.getExtractedText();

            // 2. If extraction status was ERROR or text is empty -> Unknown evidence must remain UNKNOWN
            if (text == null || text.isBlank() || "ERROR".equalsIgnoreCase(latestDoc.getExtractionStatus())) {
                DiscrepancyFinding f = new DiscrepancyFinding();
                f.setApplication(app);
                f.setEvidenceDocument(latestDoc);
                f.setApplicationRequirement(ar);
                f.setFindingType(DiscrepancyType.UNVERIFIED_SOURCE);
                f.setFieldName("Text & Metadata Extraction");
                f.setDeclaredValue("Machine-readable document");
                f.setExtractedValue("UNPARSABLE / TEXT_EXTRACTION_FAILED");
                f.setSourceCitation(latestDoc.getOriginalFilename());
                f.setSourcePage(1);
                f.setConfidenceScore(BigDecimal.valueOf(85.00));
                f.setRegulatoryReference("AICTE APH 2026-27 Sec. 1.8 - Verification of Genuine Uploaded Documentation");
                f.setSeverity(FindingSeverity.HIGH);
                f.setReviewStatus(FindingReviewStatus.PENDING_REVIEW);
                f.setReviewerNotes("Text could not be extracted cleanly from uploaded file. Manual officer audit required before PASS.");
                newFindings.add(f);
                continue;
            }

            // 3. Scan Domain Rules based on Requirement Category/Code
            String code = req.getCode().toUpperCase();
            String cat = req.getCategory() != null ? req.getCategory().toUpperCase() : "";

            // Check A: Land Area Discrepancy
            if (code.contains("LAND") || cat.contains("INFRASTRUCTURE")) {
                Matcher m = LAND_AREA_PATTERN.matcher(text);
                if (m.find()) {
                    try {
                        double extractedAcres = Double.parseDouble(m.group(1));
                        double requiredAcres = 2.50; // AICTE base urban norm
                        if (extractedAcres < requiredAcres) {
                            DiscrepancyFinding f = new DiscrepancyFinding();
                            f.setApplication(app);
                            f.setEvidenceDocument(latestDoc);
                            f.setApplicationRequirement(ar);
                            f.setFindingType(DiscrepancyType.VALUE_MISMATCH);
                            f.setFieldName("Land Area (Acres)");
                            f.setDeclaredValue(">= 2.50 Acres");
                            f.setExtractedValue(String.format(Locale.ROOT, "%.2f Acres", extractedAcres));
                            f.setSourceCitation(latestDoc.getOriginalFilename() + ", match snippet: \"" + m.group(0) + "\"");
                            f.setSourcePage(1);
                            f.setConfidenceScore(BigDecimal.valueOf(94.50));
                            f.setRegulatoryReference("AICTE APH 2026-27 Appendix 4 - Land Requirement Norms");
                            f.setSeverity(FindingSeverity.CRITICAL);
                            f.setReviewStatus(FindingReviewStatus.PENDING_REVIEW);
                            f.setReviewerNotes("Extracted deed acreage (" + extractedAcres + ") is below minimum AICTE norm (2.50 acres).");
                            newFindings.add(f);
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Check B: NOC Validity / Expiry Date Discrepancy
            if (code.contains("FIRE") || code.contains("SAFETY") || code.contains("NOC")) {
                Matcher m = DATE_PATTERN.matcher(text);
                if (m.find()) {
                    String dateStr = m.group(1);
                    LocalDate parsedDate = parseDateFlexible(dateStr);
                    if (parsedDate != null && parsedDate.isBefore(LocalDate.now())) {
                        DiscrepancyFinding f = new DiscrepancyFinding();
                        f.setApplication(app);
                        f.setEvidenceDocument(latestDoc);
                        f.setApplicationRequirement(ar);
                        f.setFindingType(DiscrepancyType.EXPIRED_DOCUMENT);
                        f.setFieldName("Statutory Safety Certificate Validity");
                        f.setDeclaredValue("Current / Valid for Academic Year");
                        f.setExtractedValue("Expired on " + parsedDate);
                        f.setSourceCitation(latestDoc.getOriginalFilename() + ", date match: \"" + m.group(0) + "\"");
                        f.setSourcePage(1);
                        f.setConfidenceScore(BigDecimal.valueOf(96.00));
                        f.setRegulatoryReference("AICTE APH 2026-27 Ch. II Clause 2.14 - Fire and Safety Certificate");
                        f.setSeverity(FindingSeverity.CRITICAL);
                        f.setReviewStatus(FindingReviewStatus.PENDING_REVIEW);
                        f.setReviewerNotes("Fire & Life Safety NOC has lapsed. Renewal clearance certificate required.");
                        newFindings.add(f);
                    }
                }
            }

            // Check C: Student-to-Faculty Cadre Ratio Discrepancy
            if (code.contains("FACULTY") || code.contains("STAFF") || cat.contains("FACULTY")) {
                Matcher m = RATIO_PATTERN.matcher(text);
                if (m.find()) {
                    try {
                        int ratioStudent = Integer.parseInt(m.group(1));
                        int maxPermitted = 20; // 1:20 norm
                        if (ratioStudent > maxPermitted) {
                            DiscrepancyFinding f = new DiscrepancyFinding();
                            f.setApplication(app);
                            f.setEvidenceDocument(latestDoc);
                            f.setApplicationRequirement(ar);
                            f.setFindingType(DiscrepancyType.DEFICIENT_RATIO);
                            f.setFieldName("Student-to-Faculty Cadre Ratio");
                            f.setDeclaredValue("1 : " + maxPermitted + " (Approved Cadre)");
                            f.setExtractedValue("1 : " + ratioStudent + " (Actual Extracted)");
                            f.setSourceCitation(latestDoc.getOriginalFilename() + ", clause: \"" + m.group(0) + "\"");
                            f.setSourcePage(1);
                            f.setConfidenceScore(BigDecimal.valueOf(92.00));
                            f.setRegulatoryReference("AICTE APH 2026-27 Appendix 7 - Norms for Faculty Cadre Ratio");
                            f.setSeverity(FindingSeverity.HIGH);
                            f.setReviewStatus(FindingReviewStatus.PENDING_REVIEW);
                            f.setReviewerNotes("Student-to-faculty ratio 1:" + ratioStudent + " violates mandatory 1:20 ceiling.");
                            newFindings.add(f);
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Check D: AICTE Permanent ID Mismatch
            if (app.getInstitution() != null && app.getInstitution().getAictePermanentId() != null) {
                String expectedId = app.getInstitution().getAictePermanentId().trim();
                Matcher m = AICTE_ID_PATTERN.matcher(text);
                if (m.find()) {
                    String foundId = m.group(1);
                    if (!foundId.equalsIgnoreCase(expectedId)) {
                        DiscrepancyFinding f = new DiscrepancyFinding();
                        f.setApplication(app);
                        f.setEvidenceDocument(latestDoc);
                        f.setApplicationRequirement(ar);
                        f.setFindingType(DiscrepancyType.VALUE_MISMATCH);
                        f.setFieldName("AICTE Permanent Institute ID");
                        f.setDeclaredValue(expectedId);
                        f.setExtractedValue(foundId);
                        f.setSourceCitation(latestDoc.getOriginalFilename() + " (Extracted ID: " + foundId + ")");
                        f.setSourcePage(1);
                        f.setConfidenceScore(BigDecimal.valueOf(98.50));
                        f.setRegulatoryReference("AICTE APH 2026-27 Sec. 1.3 - Institutional Identity Verification");
                        f.setSeverity(FindingSeverity.CRITICAL);
                        f.setReviewStatus(FindingReviewStatus.PENDING_REVIEW);
                        f.setReviewerNotes("Uploaded document contains AICTE Permanent ID " + foundId + " which does not match institution ID " + expectedId);
                        newFindings.add(f);
                    }
                }
            }
        }

        // Clean previous pending findings to refresh state without duplicates
        List<DiscrepancyFinding> existingPending = findingRepository.findByApplicationIdAndReviewStatus(applicationId, FindingReviewStatus.PENDING_REVIEW);
        findingRepository.deleteAll(existingPending);

        List<DiscrepancyFinding> saved = findingRepository.saveAll(newFindings);
        auditService.recordEvent("DISCREPANCY_SCAN", "ComplianceApplication", applicationId.toString(),
                "Evidence discrepancy scan completed. Findings detected: " + saved.size());

        return saved;
    }

    @Transactional
    public DiscrepancyFinding reviewFinding(Long findingId, FindingReviewStatus status, String notes, User reviewer) {
        DiscrepancyFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new ResourceNotFoundException("Discrepancy finding not found: " + findingId));

        finding.setReviewStatus(status);
        finding.setReviewerNotes(notes);
        finding.setReviewedBy(reviewer);

        DiscrepancyFinding updated = findingRepository.save(finding);

        auditService.recordEvent("DISCREPANCY_REVIEWED", "DiscrepancyFinding", findingId.toString(),
                "Discrepancy " + finding.getFindingType() + " reviewed as " + status + " by " + reviewer.getUsername());

        return updated;
    }

    private LocalDate parseDateFlexible(String str) {
        if (str == null) return null;
        String[] patterns = {"dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd.MM.yyyy"};
        for (String p : patterns) {
            try {
                return LocalDate.parse(str.trim(), DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        return null;
    }
}
