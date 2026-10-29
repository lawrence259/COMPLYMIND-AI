package com.complymind.dashboard.controller;

import com.complymind.dashboard.model.DashboardStats;
import com.complymind.dashboard.service.DashboardService;
import com.complymind.security.SecurityUtils;
import com.complymind.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        if (userService.getUserCount() == 0) {
            return "redirect:/setup";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login(Model model) {
        if (userService.getUserCount() == 0) {
            return "redirect:/setup";
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (userService.getUserCount() == 0) {
            return "redirect:/setup";
        }
        DashboardStats stats = dashboardService.getDashboardStatistics();
        model.addAttribute("stats", stats);
        model.addAttribute("currentUsername", SecurityUtils.getCurrentUsername());
        model.addAttribute("currentRole", SecurityUtils.getCurrentRoleName());
        return "dashboard";
    }
}
