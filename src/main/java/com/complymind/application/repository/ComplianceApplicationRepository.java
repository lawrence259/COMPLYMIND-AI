package com.complymind.application.repository;

import com.complymind.application.entity.ApplicationStatus;
import com.complymind.application.entity.ComplianceApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceApplicationRepository extends JpaRepository<ComplianceApplication, Long> {
    Optional<ComplianceApplication> findByApplicationNumber(String applicationNumber);
    boolean existsByApplicationNumber(String applicationNumber);

    List<ComplianceApplication> findByInstitutionId(Long institutionId);
    Page<ComplianceApplication> findByInstitutionId(Long institutionId, Pageable pageable);

    List<ComplianceApplication> findByStatus(ApplicationStatus status);
    long countByStatus(ApplicationStatus status);

    @Query("SELECT a.status, COUNT(a) FROM ComplianceApplication a GROUP BY a.status")
    List<Object[]> countGroupedByStatus();

    List<ComplianceApplication> findByIsDemo(boolean isDemo);

    @Query("SELECT a FROM ComplianceApplication a JOIN FETCH a.institution JOIN FETCH a.framework WHERE a.id = :id")
    Optional<ComplianceApplication> findByIdWithDetails(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("SELECT a FROM ComplianceApplication a JOIN FETCH a.institution ORDER BY a.updatedAt DESC")
    List<ComplianceApplication> findTop5RecentWithInstitution(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM compliance_applications WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
