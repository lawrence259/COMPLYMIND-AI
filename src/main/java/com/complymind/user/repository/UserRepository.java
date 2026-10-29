package com.complymind.user.repository;

import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);
    List<User> findByInstitutionId(Long institutionId);
    List<User> findByRole(UserRole role);

    /**
     * Bulk-delete demo users by their fixed known usernames.
     * Used by DemoDatasetService.removeDemoDataset() to clean up without traversing the
     * object graph — avoids Hibernate cascade issues on the institution FK.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM users WHERE username IN :usernames", nativeQuery = true)
    void deleteByUsernameIn(@Param("usernames") List<String> usernames);
}
