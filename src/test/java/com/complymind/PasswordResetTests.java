package com.complymind;

import com.complymind.user.entity.PasswordResetToken;
import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import com.complymind.user.repository.PasswordResetTokenRepository;
import com.complymind.user.repository.UserRepository;
import com.complymind.user.service.PasswordResetService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordResetTests {

    private static ConfigurableApplicationContext ctx;
    private static PasswordResetService passwordResetService;
    private static UserRepository userRepository;
    private static PasswordResetTokenRepository tokenRepository;
    private static PasswordEncoder passwordEncoder;

    @BeforeAll
    static void init() {
        ctx = SpringApplication.run(
                ComplyMindApplication.class,
                "--spring.main.web-application-type=none"
        );
        passwordResetService = ctx.getBean(PasswordResetService.class);
        userRepository = ctx.getBean(UserRepository.class);
        tokenRepository = ctx.getBean(PasswordResetTokenRepository.class);
        passwordEncoder = ctx.getBean(PasswordEncoder.class);
    }

    @AfterAll
    static void tearDown() {
        if (ctx != null && ctx.isActive()) {
            ctx.close();
        }
    }

    @Test
    @DisplayName("Password Reset: Generate token, complete reset, and block token reuse")
    void testPasswordResetLifecycle() {
        // 1. Create a test user
        String username = "pwd_test_user";
        userRepository.findByUsername(username).ifPresent(u -> {
            tokenRepository.deleteByUser(u);
            userRepository.delete(u);
        });

        User user = new User();
        user.setUsername(username);
        user.setEmail("pwd_test@example.com");
        user.setFullName("Password Test Account");
        user.setPasswordHash(passwordEncoder.encode("OldPassword123!"));
        user.setRole(UserRole.INSTITUTION_USER);
        user.setEnabled(true);
        user = userRepository.save(user);

        // 2. Initiate password reset
        PasswordResetService.ResetInitiationResult result = passwordResetService.initiatePasswordReset(
                user.getUsername(),
                "http://localhost:8080"
        );
        assertNotNull(result);
        assertNotNull(result.getResetLink());
        assertTrue(result.getResetLink().contains("token="));

        String token = result.getResetLink().substring(result.getResetLink().indexOf("token=") + 6);

        // 3. Validate token
        PasswordResetToken resetToken = passwordResetService.validateToken(token);
        assertNotNull(resetToken);
        assertEquals(user.getId(), resetToken.getUser().getId());
        assertFalse(resetToken.isUsed());
        assertFalse(resetToken.isExpired());

        // 4. Complete reset with mismatched passwords should fail
        assertThrows(Exception.class, () ->
                passwordResetService.completePasswordReset(token, "NewPassword456!", "MismatchedPassword")
        );

        // 5. Complete reset with short password should fail
        assertThrows(Exception.class, () ->
                passwordResetService.completePasswordReset(token, "123", "123")
        );

        // 6. Complete valid password reset
        passwordResetService.completePasswordReset(token, "NewStrongPassword789!", "NewStrongPassword789!");

        // 7. Verify updated password in database matches new password
        User updated = userRepository.findByUsername(username).orElseThrow();
        assertTrue(passwordEncoder.matches("NewStrongPassword789!", updated.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OldPassword123!", updated.getPasswordHash()));

        // 8. Re-using the same token must fail
        assertThrows(Exception.class, () ->
                passwordResetService.completePasswordReset(token, "AnotherPassword!", "AnotherPassword!")
        );

        // Clean up
        tokenRepository.deleteByUser(updated);
        userRepository.delete(updated);
    }
}
