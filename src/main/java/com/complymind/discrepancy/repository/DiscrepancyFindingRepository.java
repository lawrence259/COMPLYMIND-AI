package com.complymind.discrepancy.repository;

import com.complymind.discrepancy.entity.DiscrepancyFinding;
import com.complymind.discrepancy.entity.FindingReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscrepancyFindingRepository extends JpaRepository<DiscrepancyFinding, Long> {

    @Query("SELECT d FROM DiscrepancyFinding d " +
           "LEFT JOIN FETCH d.applicationRequirement ar " +
           "LEFT JOIN FETCH ar.requirement " +
           "LEFT JOIN FETCH d.evidenceDocument " +
           "WHERE d.application.id = :appId " +
           "ORDER BY d.createdAt DESC")
    List<DiscrepancyFinding> findByApplicationIdWithDetails(@Param("appId") Long applicationId);

    List<DiscrepancyFinding> findByApplicationIdAndReviewStatus(Long applicationId, FindingReviewStatus reviewStatus);

    long countByApplicationIdAndReviewStatus(Long applicationId, FindingReviewStatus reviewStatus);

    void deleteByApplicationId(Long applicationId);
}
