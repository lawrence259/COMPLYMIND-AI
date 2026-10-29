package com.complymind.common.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * Sends a rich HTML password reset email via Gmail SMTP.
     * Returns true if successfully delivered via SMTP, or false if offline/mock fallback was used.
     */
    public boolean sendPasswordResetEmail(String toEmail, String resetLink, String recipientName) {
        if (mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailUsername, "COMPLYMIND AI Notification");
                helper.setTo(toEmail);
                helper.setSubject("[COMPLYMIND AI] Password Reset Verification");

                String htmlContent = buildResetEmailHtml(recipientName, resetLink);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Password reset email successfully sent via Gmail SMTP to: {}", toEmail);
                return true;
            } catch (Exception e) {
                log.warn("Gmail SMTP delivery failed (check credentials/app password): {}. Using safe fallback.", e.getMessage());
            }
        }

        // Safe Fallback for Jury Demonstrations & Local Sandbox
        log.info("\n" +
                "====================================================================\n" +
                "  [COMPLYMIND GMAIL PROTOTYPE DISPATCHER]\n" +
                "  Recipient: {}\n" +
                "  Subject: Password Reset Verification Request\n" +
                "  One-Time Reset Link: {}\n" +
                "  Validity: 30 minutes\n" +
                "  Notice: To route live emails via your Gmail account, set:\n" +
                "          SPRING_MAIL_USERNAME=your.email@gmail.com\n" +
                "          SPRING_MAIL_PASSWORD=your-google-app-password\n" +
                "====================================================================",
                toEmail, resetLink);

        return false;
    }

    /**
     * Sends a username reminder email via Gmail SMTP.
     */
    public boolean sendUsernameReminderEmail(String toEmail, String username, String recipientName) {
        if (mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailUsername, "COMPLYMIND AI Notification");
                helper.setTo(toEmail);
                helper.setSubject("[COMPLYMIND AI] Account Username Reminder");

                String htmlContent = buildUsernameEmailHtml(recipientName, username);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Username reminder email sent via Gmail SMTP to: {}", toEmail);
                return true;
            } catch (Exception e) {
                log.warn("Gmail SMTP delivery failed: {}. Using fallback.", e.getMessage());
            }
        }

        log.info("\n" +
                "====================================================================\n" +
                "  [COMPLYMIND GMAIL USERNAME REMINDER]\n" +
                "  Recipient: {}\n" +
                "  Subject: Username Reminder Notification\n" +
                "  Registered Username: {}\n" +
                "====================================================================",
                toEmail, username);

        return false;
    }

    private String buildUsernameEmailHtml(String recipientName, String username) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f1f5f9; padding: 24px; color: #0f172a;'>" +
                "  <div style='max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 12px rgba(0,0,0,0.06);'>" +
                "    <div style='background: #0b1329; padding: 24px 32px; text-align: center;'>" +
                "      <h2 style='color: #ffffff; margin: 0; font-size: 20px; letter-spacing: 0.5px;'>COMPLYMIND <span style='color: #38bdf8;'>AI</span></h2>" +
                "      <p style='color: #94a3b8; font-size: 12px; margin: 4px 0 0;'>AICTE Approval &amp; Compliance Intelligence</p>" +
                "    </div>" +
                "    <div style='padding: 32px;'>" +
                "      <h3 style='margin-top: 0; font-size: 18px; color: #0f172a;'>Account Username Retrieval</h3>" +
                "      <p style='font-size: 14px; line-height: 1.6; color: #475569;'>Hello <strong>" + (recipientName != null ? recipientName : "User") + "</strong>,</p>" +
                "      <p style='font-size: 14px; line-height: 1.6; color: #475569;'>Here is your registered username for the COMPLYMIND AI platform:</p>" +
                "      <div style='text-align: center; margin: 24px 0; padding: 16px; background: #f8fafc; border: 1px dashed #0284c7; border-radius: 8px;'>" +
                "        <span style='font-size: 20px; font-weight: bold; color: #1d4ed8; letter-spacing: 1px;'>" + username + "</span>" +
                "      </div>" +
                "      <p style='font-size: 12px; color: #64748b; line-height: 1.5;'>You can now use this username to sign in to your dashboard.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private String buildResetEmailHtml(String recipientName, String resetLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f1f5f9; padding: 24px; color: #0f172a;'>" +
                "  <div style='max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 12px rgba(0,0,0,0.06);'>" +
                "    <div style='background: #0b1329; padding: 24px 32px; text-align: center;'>" +
                "      <h2 style='color: #ffffff; margin: 0; font-size: 20px; letter-spacing: 0.5px;'>COMPLYMIND <span style='color: #38bdf8;'>AI</span></h2>" +
                "      <p style='color: #94a3b8; font-size: 12px; margin: 4px 0 0;'>AICTE Approval &amp; Compliance Intelligence</p>" +
                "    </div>" +
                "    <div style='padding: 32px;'>" +
                "      <h3 style='margin-top: 0; font-size: 18px; color: #0f172a;'>Password Reset Request</h3>" +
                "      <p style='font-size: 14px; line-height: 1.6; color: #475569;'>Hello <strong>" + (recipientName != null ? recipientName : "User") + "</strong>,</p>" +
                "      <p style='font-size: 14px; line-height: 1.6; color: #475569;'>A request was received to reset your password for COMPLYMIND AI. Click the secure verification button below to set a new password:</p>" +
                "      <div style='text-align: center; margin: 28px 0;'>" +
                "        <a href='" + resetLink + "' style='background: #1d4ed8; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: bold; font-size: 14px; display: inline-block;'>Reset My Password</a>" +
                "      </div>" +
                "      <p style='font-size: 12px; color: #64748b; line-height: 1.5;'>This verification link is single-use and will expire in <strong>30 minutes</strong>. If you did not request this password change, no action is required.</p>" +
                "      <div style='border-top: 1px solid #e2e8f0; margin-top: 24px; padding-top: 16px; font-size: 11px; color: #94a3b8; word-break: break-all;'>" +
                "        Or copy-paste this direct URL into your browser:<br>" +
                "        <a href='" + resetLink + "' style='color: #1d4ed8;'>" + resetLink + "</a>" +
                "      </div>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}
