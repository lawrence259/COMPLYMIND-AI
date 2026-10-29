package com.complymind.user.controller;

import com.complymind.user.entity.PasswordResetToken;
import com.complymind.user.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String identifier,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        try {
            String baseUrl = getBaseUrl(request);
            PasswordResetService.ResetInitiationResult result = passwordResetService.initiatePasswordReset(identifier, baseUrl);

            if (result.isLiveEmailSent()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "A secure password reset email has been dispatched via Gmail to your address (" + result.getMaskedEmail() + "). Please check your inbox and spam folder.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Password reset link generated for " + result.getMaskedEmail() + "!");
                redirectAttributes.addFlashAttribute("demoResetLink", result.getResetLink());
            }

            return "redirect:/forgot-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(required = false) String token,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        try {
            PasswordResetToken resetToken = passwordResetService.validateToken(token);
            model.addAttribute("token", token);
            model.addAttribute("username", resetToken.getUser().getUsername());
            return "reset-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token,
                                      @RequestParam String newPassword,
                                      @RequestParam String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.completePasswordReset(token, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password reset successful! Please log in with your new credentials.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
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
