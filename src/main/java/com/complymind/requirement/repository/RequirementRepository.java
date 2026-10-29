package com.complymind.requirement.repository;

import com.complymind.requirement.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    List<Requirement> findByFrameworkId(Long frameworkId);
    Optional<Requirement> findByFrameworkIdAndCode(Long frameworkId, String code);
    List<Requirement> findByIsDemo(boolean isDemo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM requirements WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
