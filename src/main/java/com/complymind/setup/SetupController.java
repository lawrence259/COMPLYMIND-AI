package com.complymind.setup;

import com.complymind.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SetupController {

    private final UserService userService;

    public SetupController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/setup")
    public String showSetupPage(Model model) {
        if (userService.getUserCount() > 0) {
            return "redirect:/login";
        }
        return "setup";
    }

    @PostMapping("/setup/create")
    public String createInitialAdmin(@RequestParam String username,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam String fullName,
                                     RedirectAttributes redirectAttributes) {
        if (userService.getUserCount() > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Setup is already completed.");
            return "redirect:/login";
        }

        try {
            userService.createInitialAdmin(username, email, password, fullName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Initial administrator account created successfully. Please login to continue.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/setup";
        }
    }
}
