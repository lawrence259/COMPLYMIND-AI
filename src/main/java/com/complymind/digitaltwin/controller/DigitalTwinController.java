package com.complymind.digitaltwin.controller;

import com.complymind.common.exception.BusinessValidationException;
import com.complymind.digitaltwin.model.InstitutionalDigitalTwin;
import com.complymind.digitaltwin.service.DigitalTwinService;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.service.InstitutionService;
import com.complymind.security.CustomUserDetails;
import com.complymind.security.SecurityUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DigitalTwinController {

    private final DigitalTwinService digitalTwinService;
    private final InstitutionService institutionService;

    public DigitalTwinController(DigitalTwinService digitalTwinService,
                                 InstitutionService institutionService) {
        this.digitalTwinService = digitalTwinService;
        this.institutionService = institutionService;
    }

    @GetMapping("/institutions/{id}/digital-twin")
    public String showDigitalTwin(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        if (!SecurityUtils.canAccessInstitution(id)) {
            throw new BusinessValidationException("Access denied to institution resources.");
        }
        Institution inst = institutionService.getInstitutionById(id);
        InstitutionalDigitalTwin twin = digitalTwinService.computeDigitalTwin(id);

        model.addAttribute("institution", inst);
        model.addAttribute("twin", twin);
        model.addAttribute("activeMenu", "institutions");

        return "digitaltwin/view";
    }
}
