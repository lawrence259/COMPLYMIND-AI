package com.complymind.evidence.repository;

import com.complymind.evidence.entity.ReviewDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewDecisionRepository extends JpaRepository<ReviewDecision, Long> {
    List<ReviewDecision> findByEvidenceDocumentId(Long evidenceDocumentId);
    List<ReviewDecision> findByApplicationRequirementId(Long applicationRequirementId);
}
