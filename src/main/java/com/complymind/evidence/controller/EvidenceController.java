package com.complymind.evidence.controller;

import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.entity.ReviewStatus;
import com.complymind.evidence.service.EvidenceReviewService;
import com.complymind.evidence.service.EvidenceStorageService;
import com.complymind.security.SecurityUtils;
import com.complymind.user.entity.User;
import com.complymind.user.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;

@Controller
@RequestMapping("/evidence")
public class EvidenceController {

    private final EvidenceReviewService evidenceService;
    private final EvidenceStorageService storageService;
    private final UserService userService;

    public EvidenceController(EvidenceReviewService evidenceService,
                              EvidenceStorageService storageService,
                              UserService userService) {
        this.evidenceService = evidenceService;
        this.storageService = storageService;
        this.userService = userService;
    }

    @PostMapping("/upload")
    public String uploadEvidence(@RequestParam Long applicationId,
                                 @RequestParam Long appReqId,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            User uploader = SecurityUtils.getCurrentUserDetails()
                    .map(u -> userService.getUserById(u.getId()))
                    .orElse(null);

            EvidenceDocument doc = evidenceService.uploadEvidence(applicationId, appReqId, file, uploader);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Document '" + doc.getOriginalFilename() + "' uploaded successfully! Extracted " +
                            (doc.getExtractedText() != null ? doc.getExtractedText().length() : 0) + " characters for review.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload failed: " + e.getMessage());
        }
        return "redirect:/applications/" + applicationId;
    }

    @GetMapping("/review/{id}")
    public String showReviewPage(@PathVariable Long id, Model model) {
        EvidenceDocument doc = evidenceService.getEvidenceById(id);
        model.addAttribute("evidence", doc);
        model.addAttribute("statuses", ReviewStatus.values());
        return "evidence/review";
    }

    @PostMapping("/review/{id}")
    public String recordReviewDecision(@PathVariable Long id,
                                       @RequestParam ReviewStatus decision,
                                       @RequestParam(required = false) String comments,
                                       RedirectAttributes redirectAttributes) {
        try {
            User reviewer = SecurityUtils.getCurrentUserDetails()
                    .map(u -> userService.getUserById(u.getId()))
                    .orElseThrow(() -> new RuntimeException("Authenticated user required for review"));

            EvidenceDocument doc = evidenceService.getEvidenceById(id);
            evidenceService.recordDecision(id, decision, comments != null ? comments : "", reviewer);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Verification decision recorded as " + decision + " for document: " + doc.getOriginalFilename());
            return "redirect:/applications/" + doc.getApplication().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Review failed: " + e.getMessage());
            return "redirect:/evidence/review/" + id;
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        try {
            EvidenceDocument doc = evidenceService.getEvidenceById(id);
            Path filePath = storageService.load(doc.getStoredFilename());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(doc.getMimeType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getOriginalFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long id) {
        try {
            EvidenceDocument doc = evidenceService.getEvidenceById(id);
            Path filePath = storageService.load(doc.getStoredFilename());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(doc.getMimeType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getOriginalFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
