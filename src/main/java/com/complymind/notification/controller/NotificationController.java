package com.complymind.notification.controller;

import com.complymind.common.dto.ApiResponse;
import com.complymind.notification.entity.Notification;
import com.complymind.notification.service.NotificationService;
import com.complymind.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listNotifications(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "15") int size,
                                    Model model) {
        Long userId = SecurityUtils.getCurrentUserDetails().map(u -> u.getId()).orElse(0L);
        Page<Notification> notifications = notificationService.getPagedNotificationsForUser(userId, PageRequest.of(page, size));
        long unreadCount = notificationService.getUnreadCount(userId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/mark-all-read")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        Long userId = SecurityUtils.getCurrentUserDetails().map(u -> u.getId()).orElse(0L);
        notificationService.markAllAsRead(userId);
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/notifications";
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ApiResponse<Long> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserDetails().map(u -> u.getId()).orElse(0L);
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }
}
