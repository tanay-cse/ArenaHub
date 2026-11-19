package com.arenahub.loyalty;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rewards")
public class AdminRewardController {

    private final RewardService rewardService;

    public AdminRewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public String listRewards(Model model) {
        model.addAttribute("rewards", rewardService.getAllRewards());
        if (!model.containsAttribute("reward")) {
            model.addAttribute("reward", new Reward());
        }
        return "loyalty/admin-rewards";
    }

    @PostMapping
    public String saveReward(@ModelAttribute("reward") Reward reward, RedirectAttributes redirectAttributes) {
        try {
            rewardService.saveReward(reward);
            redirectAttributes.addFlashAttribute("successMessage", "Reward saved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving reward: " + ex.getMessage());
        }
        return "redirect:/admin/rewards";
    }

    @GetMapping("/edit/{id}")
    public String editReward(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return rewardService.getRewardById(id)
                .map(reward -> {
                    model.addAttribute("rewards", rewardService.getAllRewards());
                    model.addAttribute("reward", reward);
                    return "loyalty/admin-rewards";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Reward not found.");
                    return "redirect:/admin/rewards";
                });
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateReward(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rewardService.deactivateReward(id);
            redirectAttributes.addFlashAttribute("successMessage", "Reward deactivated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deactivating reward: " + ex.getMessage());
        }
        return "redirect:/admin/rewards";
    }

    @PostMapping("/toggle/{id}")
    public String toggleReward(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rewardService.toggleRewardActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Reward status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating reward status: " + ex.getMessage());
        }
        return "redirect:/admin/rewards";
    }
}
