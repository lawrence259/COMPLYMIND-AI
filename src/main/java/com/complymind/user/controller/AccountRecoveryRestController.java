package com.complymind.user.controller;

import com.complymind.common.dto.ApiResponse;
import com.complymind.user.entity.PasswordResetToken;
import com.complymind.user.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Account Recovery", description = "In-page self-service password reset & username retrieval endpoints")
public class AccountRecoveryRestController {

    private final PasswordResetService passwordResetService;

    public AccountRecoveryRestController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset via Gmail or local verification token")
    public ApiResponse<Map<String, Object>> requestPasswordReset(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String identifier = payload.get("identifier");
        String baseUrl = getBaseUrl(request);
        PasswordResetService.ResetInitiationResult result = passwordResetService.initiatePasswordReset(identifier, baseUrl);

        return ApiResponse.ok(Map.of(
                "liveEmailSent", result.isLiveEmailSent(),
                "maskedEmail", result.getMaskedEmail(),
                "token", result.getToken(),
                "resetLink", result.getResetLink()
        ));
    }

    @PostMapping("/verify-and-reset")
    @Operation(summary = "Validate token and complete password update on the same page")
    public ApiResponse<String> verifyAndReset(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");

        passwordResetService.completePasswordReset(token, newPassword, confirmPassword);
        return ApiResponse.ok("Password successfully updated. You may now sign in.");
    }

    @PostMapping("/forgot-username")
    @Operation(summary = "Retrieve registered username using registered institutional email")
    public ApiResponse<Map<String, String>> recoverUsername(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String username = passwordResetService.findUsernameByEmail(email);

        return ApiResponse.ok(Map.of(
                "username", username,
                "message", "Account found! Your registered username is: " + username
        ));
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception e) {
        return ApiResponse.error(e.getMessage() != null ? e.getMessage() : "An error occurred during account recovery.");
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(request.getContextPath());
        return url.toString();
    }
}
