package com.complymind.correctiveaction.repository;

import com.complymind.correctiveaction.entity.ActionStatus;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    List<CorrectiveAction> findByApplicationId(Long applicationId);
    List<CorrectiveAction> findByAssignedToId(Long userId);
    List<CorrectiveAction> findByStatus(ActionStatus status);
    long countByStatus(ActionStatus status);

    @Query("SELECT ca FROM CorrectiveAction ca WHERE ca.dueDate < :now AND ca.status NOT IN :excludedStatuses")
    List<CorrectiveAction> findActionsDueBefore(@Param("now") LocalDateTime now,
                                                @Param("excludedStatuses") List<ActionStatus> excludedStatuses);

    long countByDueDateBeforeAndStatusNotIn(LocalDateTime now, List<ActionStatus> excluded);

    List<CorrectiveAction> findByIsDemo(boolean isDemo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM corrective_actions WHERE is_demo = true", nativeQuery = true)
    void deleteByIsDemoTrue();
}
