package com.complymind.evidence.repository;

import com.complymind.evidence.entity.EvidenceDocument;
import com.complymind.evidence.entity.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceDocumentRepository extends JpaRepository<EvidenceDocument, Long> {
    List<EvidenceDocument> findByApplicationId(Long applicationId);
    List<EvidenceDocument> findByApplicationRequirementId(Long applicationRequirementId);
    List<EvidenceDocument> findByApplicationRequirementIdOrderByVersionNumberDesc(Long applicationRequirementId);
    List<EvidenceDocument> findByReviewStatus(ReviewStatus reviewStatus);
    long countByReviewStatus(ReviewStatus reviewStatus);
    List<EvidenceDocument> findByIsDemo(boolean isDemo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM evidence_documents WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
