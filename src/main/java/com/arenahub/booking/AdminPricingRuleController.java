package com.arenahub.booking;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/pricing-rules")
public class AdminPricingRuleController {

    private final PricingService pricingService;

    public AdminPricingRuleController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping
    public String listRules(Model model) {
        model.addAttribute("rules", pricingService.getAllRules());
        if (!model.containsAttribute("rule")) {
            PricingRule rule = new PricingRule();
            rule.setIsActive(true);
            model.addAttribute("rule", rule);
        }
        return "booking/admin-pricing-rules";
    }

    @PostMapping
    public String saveRule(@ModelAttribute("rule") PricingRule rule, RedirectAttributes redirectAttributes) {
        try {
            pricingService.saveRule(rule);
            redirectAttributes.addFlashAttribute("successMessage", "Pricing rule saved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving pricing rule: " + ex.getMessage());
        }
        return "redirect:/admin/pricing-rules";
    }

    @GetMapping("/edit/{id}")
    public String editRule(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return pricingService.getRuleById(id)
                .map(rule -> {
                    model.addAttribute("rules", pricingService.getAllRules());
                    model.addAttribute("rule", rule);
                    return "booking/admin-pricing-rules";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Pricing rule not found.");
                    return "redirect:/admin/pricing-rules";
                });
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateRule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pricingService.deactivateRule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pricing rule deactivated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deactivating pricing rule: " + ex.getMessage());
        }
        return "redirect:/admin/pricing-rules";
    }

    @PostMapping("/toggle/{id}")
    public String toggleRule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pricingService.toggleRuleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pricing rule status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating pricing rule status: " + ex.getMessage());
        }
        return "redirect:/admin/pricing-rules";
    }
}
