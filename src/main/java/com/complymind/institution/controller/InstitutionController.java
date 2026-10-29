package com.complymind.institution.controller;

import com.complymind.institution.entity.Institution;
import com.complymind.institution.entity.InstitutionType;
import com.complymind.institution.service.InstitutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    public String listInstitutions(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        Page<Institution> institutionPage = institutionService.getInstitutions(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        model.addAttribute("institutions", institutionPage);
        return "institutions/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("institution", new Institution());
        model.addAttribute("types", InstitutionType.values());
        return "institutions/create";
    }

    @PostMapping("/create")
    public String createInstitution(@ModelAttribute Institution institution,
                                    RedirectAttributes redirectAttributes) {
        try {
            institutionService.createInstitution(institution);
            redirectAttributes.addFlashAttribute("successMessage", "Institution registered successfully!");
            return "redirect:/institutions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/institutions/new";
        }
    }

    @GetMapping("/{id}")
    public String viewInstitution(@PathVariable Long id, Model model) {
        Institution institution = institutionService.getInstitutionById(id);
        model.addAttribute("institution", institution);
        return "institutions/view";
    }
}
