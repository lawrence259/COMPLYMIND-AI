package com.complymind.requirement.repository;

import com.complymind.requirement.entity.ApplicationRequirement;
import com.complymind.requirement.entity.RequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRequirementRepository extends JpaRepository<ApplicationRequirement, Long> {
    @Query("SELECT ar FROM ApplicationRequirement ar JOIN FETCH ar.requirement WHERE ar.application.id = :applicationId")
    List<ApplicationRequirement> findByApplicationId(@Param("applicationId") Long applicationId);
    Optional<ApplicationRequirement> findByApplicationIdAndRequirementId(Long applicationId, Long requirementId);
    List<ApplicationRequirement> findByApplicationIdAndStatus(Long applicationId, RequirementStatus status);
    long countByStatus(RequirementStatus status);

    @Query("SELECT ar.status, COUNT(ar) FROM ApplicationRequirement ar GROUP BY ar.status")
    List<Object[]> countGroupedByStatus();

    // Use enum constant reference — avoids string comparison issues with H2 in PostgreSQL mode
    @Query("SELECT ar FROM ApplicationRequirement ar WHERE ar.deadline < :now AND ar.status <> com.complymind.requirement.entity.RequirementStatus.SATISFIED")
    List<ApplicationRequirement> findOverdueRequirements(@Param("now") LocalDateTime now);

    List<ApplicationRequirement> findByIsDemo(boolean isDemo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM application_requirements WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
