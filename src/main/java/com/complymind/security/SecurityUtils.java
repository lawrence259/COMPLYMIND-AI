package com.complymind.security;

import com.complymind.user.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of((CustomUserDetails) auth.getPrincipal());
        }
        return Optional.empty();
    }

    public static String getCurrentUsername() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUsername)
                .orElse("SYSTEM");
    }

    public static String getCurrentRoleName() {
        return getCurrentUserDetails()
                .map(u -> u.getRole().name())
                .orElse("SYSTEM");
    }

    public static boolean hasRole(UserRole role) {
        return getCurrentUserDetails()
                .map(u -> u.getRole() == role)
                .orElse(false);
    }

    public static boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    public static Long getCurrentInstitutionId() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getInstitutionId)
                .orElse(null);
    }

    public static boolean canAccessInstitution(Long institutionId) {
        if (isAdmin() || hasRole(UserRole.REVIEWER) || hasRole(UserRole.COMPLIANCE_OFFICER)) {
            return true;
        }
        Long userInstId = getCurrentInstitutionId();
        return userInstId != null && userInstId.equals(institutionId);
    }
}
