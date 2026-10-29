package com.complymind.framework.repository;

import com.complymind.framework.entity.ComplianceFramework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceFrameworkRepository extends JpaRepository<ComplianceFramework, Long> {
    Optional<ComplianceFramework> findByCode(String code);
    List<ComplianceFramework> findByIsActiveTrue();
    List<ComplianceFramework> findByIsDemo(boolean isDemo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM compliance_frameworks WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
