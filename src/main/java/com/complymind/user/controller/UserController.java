package com.complymind.user.controller;

import com.complymind.institution.service.InstitutionService;
import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import com.complymind.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final InstitutionService institutionService;

    public UserController(UserService userService, InstitutionService institutionService) {
        this.userService = userService;
        this.institutionService = institutionService;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("institutions", institutionService.getAllInstitutions());
        return "users/list";
    }

    @PostMapping("/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String fullName,
                             @RequestParam UserRole role,
                             @RequestParam(required = false) Long institutionId,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(username, email, password, fullName, role, institutionId);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + username + "' created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }
}
