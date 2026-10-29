package com.complymind.risk.repository;

import com.complymind.risk.entity.RiskAssessment;
import com.complymind.risk.entity.RiskBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    List<RiskAssessment> findByApplicationIdOrderByCalculatedAtDesc(Long applicationId);
    Optional<RiskAssessment> findFirstByApplicationIdOrderByCalculatedAtDesc(Long applicationId);
    long countByRiskBand(RiskBand riskBand);
}
