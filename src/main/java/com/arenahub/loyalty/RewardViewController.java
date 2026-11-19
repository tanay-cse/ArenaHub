package com.arenahub.loyalty;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RewardViewController {

    private final RewardService rewardService;

    public RewardViewController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping("/rewards")
    public String viewRewards(Model model) {
        model.addAttribute("rewards", rewardService.getActiveRewards());
        return "loyalty/rewards";
    }
}
