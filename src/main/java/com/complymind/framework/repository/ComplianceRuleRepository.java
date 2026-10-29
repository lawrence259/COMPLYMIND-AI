package com.complymind.framework.repository;

import com.complymind.framework.entity.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, Long> {
    List<ComplianceRule> findByFrameworkId(Long frameworkId);
    List<ComplianceRule> findByFrameworkIdAndIsActiveTrue(Long frameworkId);
    Optional<ComplianceRule> findByFrameworkIdAndRuleCode(Long frameworkId, String ruleCode);
    List<ComplianceRule> findByIsDemo(boolean isDemo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM compliance_rules WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
