package com.complymind.ripple.repository;

import com.complymind.ripple.entity.RequirementDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementDependencyRepository extends JpaRepository<RequirementDependency, Long> {
    List<RequirementDependency> findBySourceRequirementId(Long sourceRequirementId);
    List<RequirementDependency> findByDependentRequirementId(Long dependentRequirementId);
    Optional<RequirementDependency> findBySourceRequirementIdAndDependentRequirementId(Long sourceRequirementId, Long dependentRequirementId);

    @Query("SELECT d FROM RequirementDependency d JOIN FETCH d.sourceRequirement JOIN FETCH d.dependentRequirement WHERE d.sourceRequirement.framework.id = :frameworkId")
    List<RequirementDependency> findByFrameworkId(@Param("frameworkId") Long frameworkId);

    List<RequirementDependency> findByIsDemo(boolean isDemo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM requirement_dependencies WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
