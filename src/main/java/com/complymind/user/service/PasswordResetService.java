package com.complymind.user.service;

import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.common.service.EmailService;
import com.complymind.user.entity.PasswordResetToken;
import com.complymind.user.entity.User;
import com.complymind.user.repository.PasswordResetTokenRepository;
import com.complymind.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder,
                                AuditService auditService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public static class ResetInitiationResult {
        private final boolean liveEmailSent;
        private final String resetLink;
        private final String maskedEmail;
        private final String token;

        public ResetInitiationResult(boolean liveEmailSent, String resetLink, String maskedEmail, String token) {
            this.liveEmailSent = liveEmailSent;
            this.resetLink = resetLink;
            this.maskedEmail = maskedEmail;
            this.token = token;
        }

        public boolean isLiveEmailSent() {
            return liveEmailSent;
        }

        public String getResetLink() {
            return resetLink;
        }

        public String getMaskedEmail() {
            return maskedEmail;
        }

        public String getToken() {
            return token;
        }
    }

    @Transactional
    public ResetInitiationResult initiatePasswordReset(String identifier, String baseUrl) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new BusinessValidationException("Username or email address is required.");
        }

        String query = identifier.trim();
        Optional<User> userOpt = userRepository.findByUsername(query);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(query);
        }

        if (userOpt.isEmpty()) {
            throw new BusinessValidationException("No registered account found matching: " + identifier);
        }

        User user = userOpt.get();

        // Invalidate old tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate unique token valid for 30 minutes
        String tokenStr = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken resetToken = new PasswordResetToken(tokenStr, user, expiry);
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/reset-password?token=" + tokenStr;

        boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), resetLink, user.getFullName());

        auditService.recordEvent(
                "PASSWORD_RESET_REQUESTED",
                "User",
                user.getId().toString(),
                "Password reset requested for " + user.getUsername() + ". Gmail delivery status: " + (emailSent ? "SUCCESS" : "LOCAL_FALLBACK")
        );

        return new ResetInitiationResult(emailSent, resetLink, maskEmail(user.getEmail()), tokenStr);
    }

    @Transactional(readOnly = true)
    public String findUsernameByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessValidationException("Email address is required.");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new BusinessValidationException("No registered account found with email: " + email.trim()));

        emailService.sendUsernameReminderEmail(user.getEmail(), user.getUsername(), user.getFullName());
        return user.getUsername();
    }

    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessValidationException("Password reset token is missing.");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new BusinessValidationException("Invalid or unrecognized password reset token."));

        if (resetToken.isUsed()) {
            throw new BusinessValidationException("This password reset link has already been used. Please request a new one.");
        }

        if (resetToken.isExpired()) {
            throw new BusinessValidationException("This password reset link has expired (validity is 30 minutes). Please request a new one.");
        }

        return resetToken;
    }

    @Transactional
    public void completePasswordReset(String token, String newPassword, String confirmPassword) {
        PasswordResetToken resetToken = validateToken(token);

        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessValidationException("New password must be at least 6 characters long.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessValidationException("New password and confirm password do not match.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        auditService.recordEvent(
                "PASSWORD_RESET_COMPLETED",
                "User",
                user.getId().toString(),
                "Password successfully updated via secure token verification."
        );
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "registered address";
        int atIdx = email.indexOf("@");
        String name = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
