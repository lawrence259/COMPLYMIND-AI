package com.complymind.demo.controller;

import com.complymind.demo.service.DemoDatasetService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/demo")
public class DemoController {

    private final DemoDatasetService demoDatasetService;

    public DemoController(DemoDatasetService demoDatasetService) {
        this.demoDatasetService = demoDatasetService;
    }

    @PostMapping("/load")
    public String loadDemoDataset(RedirectAttributes redirectAttributes) {
        try {
            demoDatasetService.loadDemoDataset();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Synthetic demonstration dataset loaded successfully! Application APP-NIST-2026-DEMO is now ready for Ripple Engine and Risk Engine exploration.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to load demo dataset: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/clear")
    public String clearDemoDataset(RedirectAttributes redirectAttributes) {
        try {
            demoDatasetService.removeDemoDataset();
            redirectAttributes.addFlashAttribute("successMessage",
                    "All synthetic demonstration records were safely removed. User-created records remain untouched.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to clear demo dataset: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}
