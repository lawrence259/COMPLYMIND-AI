package com.complymind.framework.controller;

import com.complymind.framework.entity.ComplianceFramework;
import com.complymind.framework.entity.ComplianceRule;
import com.complymind.framework.service.ComplianceRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/rules")
public class RuleController {

    private final ComplianceRuleService ruleService;

    public RuleController(ComplianceRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public String listRules(@RequestParam(required = false) Long frameworkId, Model model) {
        List<ComplianceFramework> frameworks = ruleService.getAllFrameworks();
        List<ComplianceRule> rules;
        if (frameworkId != null) {
            rules = ruleService.getRulesByFramework(frameworkId);
        } else {
            rules = ruleService.getAllRules();
        }

        model.addAttribute("frameworks", frameworks);
        model.addAttribute("rules", rules);
        model.addAttribute("selectedFrameworkId", frameworkId);
        return "rules/list";
    }

    @GetMapping("/new")
    public String showCreateRuleForm(Model model) {
        model.addAttribute("frameworks", ruleService.getActiveFrameworks());
        model.addAttribute("rule", new ComplianceRule());
        return "rules/create";
    }

    @PostMapping("/create")
    public String createRule(@RequestParam Long frameworkId,
                             @ModelAttribute ComplianceRule rule,
                             RedirectAttributes redirectAttributes) {
        try {
            ruleService.createRule(frameworkId, rule);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rule " + rule.getRuleCode() + " added successfully.");
            return "redirect:/rules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/rules/new";
        }
    }
}
