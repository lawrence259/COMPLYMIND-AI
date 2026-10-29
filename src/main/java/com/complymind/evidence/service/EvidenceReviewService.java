package com.complymind.evidence.service;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.repository.ComplianceApplicationRepository;
import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.entity.ReviewDecision;
import com.complymind.evidence.entity.ReviewStatus;
import com.complymind.evidence.repository.EvidenceDocumentRepository;
import com.complymind.evidence.repository.ReviewDecisionRepository;
import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.RequirementStatus;
import com.complymind.requirement.repository.ApplicationRequirementRepository;
import com.complymind.ripple.service.RippleEngineService;
import com.complymind.risk.service.RiskEngineService;
import com.complymind.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
public class EvidenceReviewService {

    private final EvidenceDocumentRepository evidenceRepository;
    private final ReviewDecisionRepository reviewDecisionRepository;
    private final ApplicationRequirementRepository appRequirementRepository;
    private final ComplianceApplicationRepository applicationRepository;
    private final EvidenceStorageService storageService;
    private final DocumentParserService parserService;
    private final RippleEngineService rippleEngineService;
    private final RiskEngineService riskEngineService;
    private final AuditService auditService;

    public EvidenceReviewService(EvidenceDocumentRepository evidenceRepository,
                                 ReviewDecisionRepository reviewDecisionRepository,
                                 ApplicationRequirementRepository appRequirementRepository,
                                 ComplianceApplicationRepository applicationRepository,
                                 EvidenceStorageService storageService,
                                 DocumentParserService parserService,
                                 RippleEngineService rippleEngineService,
                                 RiskEngineService riskEngineService,
                                 AuditService auditService) {
        this.evidenceRepository = evidenceRepository;
        this.reviewDecisionRepository = reviewDecisionRepository;
        this.appRequirementRepository = appRequirementRepository;
        this.applicationRepository = applicationRepository;
        this.storageService = storageService;
        this.parserService = parserService;
        this.rippleEngineService = rippleEngineService;
        this.riskEngineService = riskEngineService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EvidenceDocument> getEvidenceForApplication(Long applicationId) {
        return evidenceRepository.findByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public List<EvidenceDocument> getEvidenceForRequirement(Long appReqId) {
        return evidenceRepository.findByApplicationRequirementIdOrderByVersionNumberDesc(appReqId);
    }

    @Transactional(readOnly = true)
    public EvidenceDocument getEvidenceById(Long id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found: " + id));
    }

    @Transactional
    public EvidenceDocument uploadEvidence(Long applicationId, Long appReqId, MultipartFile file, User uploadingUser) {
        ComplianceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        ApplicationRequirement ar = appRequirementRepository.findById(appReqId)
                .orElseThrow(() -> new ResourceNotFoundException("Application requirement not found: " + appReqId));

        String storedFilename = storageService.storeFile(file);
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            fileBytes = new byte[0];
        }

        // Parse text & metadata using Apache Tika & POI
        DocumentParserService.ParseResult parseResult;
        try (InputStream is = file.getInputStream()) {
            parseResult = parserService.extractTextAndMetadata(is, file.getOriginalFilename(), fileBytes);
        } catch (Exception e) {
            parseResult = new DocumentParserService.ParseResult("", java.util.Collections.emptyMap(),
                    parserService.computeSha256(fileBytes), false, e.getMessage());
        }

        // Determine version number
        List<EvidenceDocument> existing = evidenceRepository.findByApplicationRequirementIdOrderByVersionNumberDesc(appReqId);
        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1;

        EvidenceDocument doc = new EvidenceDocument();
        doc.setApplication(app);
        doc.setApplicationRequirement(ar);
        doc.setOriginalFilename(file.getOriginalFilename());
        doc.setStoredFilename(storedFilename);
        doc.setFileSize(file.getSize());
        doc.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        doc.setSha256Checksum(parseResult.getChecksum());
        doc.setVersionNumber(nextVersion);
        doc.setReviewStatus(ReviewStatus.PENDING);
        doc.setExtractedText(parseResult.getText());
        doc.setExtractionStatus(parseResult.isSuccess() ? "SUCCESS" : "ERROR");
        doc.setExtractionError(parseResult.getErrorMessage());
        doc.setUploadedBy(uploadingUser);
        doc.setDemo(app.isDemo());

        EvidenceDocument saved = evidenceRepository.save(doc);

        // Update requirement status to UNDER_REVIEW
        ar.setStatus(RequirementStatus.UNDER_REVIEW);
        appRequirementRepository.save(ar);

        auditService.recordEvent("EVIDENCE_UPLOADED", "EvidenceDocument", saved.getId().toString(),
                "Uploaded " + saved.getOriginalFilename() + " (v" + nextVersion + ") for " + ar.getRequirement().getCode());

        // Re-evaluate ripple and risk
        rippleEngineService.recalculateAndPersist(applicationId);
        riskEngineService.evaluateAndPersist(applicationId);

        return saved;
    }

    @Transactional
    public ReviewDecision recordDecision(Long evidenceId, ReviewStatus decision, String comments, User reviewer) {
        EvidenceDocument doc = getEvidenceById(evidenceId);
        ApplicationRequirement ar = doc.getApplicationRequirement();
        ComplianceApplication app = doc.getApplication();

        doc.setReviewStatus(decision);
        evidenceRepository.save(doc);

        ReviewDecision rd = new ReviewDecision();
        rd.setEvidenceDocument(doc);
        rd.setApplicationRequirement(ar);
        rd.setReviewer(reviewer);
        rd.setDecision(decision);
        rd.setComments(comments);

        ReviewDecision saved = reviewDecisionRepository.save(rd);

        if (decision == ReviewStatus.APPROVED) {
            ar.setStatus(RequirementStatus.SATISFIED);
            ar.setComplianceNotes("Approved by reviewer: " + reviewer.getFullName() + ". Comments: " + comments);
        } else if (decision == ReviewStatus.REJECTED) {
            ar.setStatus(RequirementStatus.REJECTED);
            ar.setComplianceNotes("REJECTED by reviewer: " + reviewer.getFullName() + ". Reason: " + comments);
        } else {
            ar.setStatus(RequirementStatus.PENDING);
            ar.setComplianceNotes("Revision requested: " + comments);
        }
        appRequirementRepository.save(ar);

        auditService.recordEvent("EVIDENCE_REVIEWED", "EvidenceDocument", doc.getId().toString(),
                "Review decision: " + decision + " by " + reviewer.getUsername() + " for " + ar.getRequirement().getCode());

        // Re-calculate ripple engine downstream and update risk scores!
        rippleEngineService.recalculateAndPersist(app.getId());
        riskEngineService.evaluateAndPersist(app.getId());

        return saved;
    }
}
