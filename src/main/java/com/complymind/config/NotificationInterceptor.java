package com.complymind.config;

import com.complymind.notification.service.NotificationService;
import com.complymind.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class NotificationInterceptor implements HandlerInterceptor {

    private final NotificationService notificationService;

    public NotificationInterceptor(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        if (modelAndView != null && !modelAndView.getViewName().startsWith("redirect:")) {
            try {
                Long userId = SecurityUtils.getCurrentUserDetails()
                        .map(u -> u.getId()).orElse(null);
                if (userId != null) {
                    long count = notificationService.getUnreadCount(userId);
                    modelAndView.addObject("unreadNotificationCount", count);
                }
            } catch (Exception e) {
                // Not authenticated or notification service unavailable; skip
            }
        }
    }
}
