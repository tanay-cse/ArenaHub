package com.arenahub.loyalty;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RewardViewController {

    private final RewardService rewardService;
    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;

    public RewardViewController(RewardService rewardService,
                                UserRepository userRepository,
                                LoyaltyService loyaltyService) {
        this.rewardService = rewardService;
        this.userRepository = userRepository;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/rewards")
    public String viewRewards(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("rewards", rewardService.getActiveRewards());
        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("userBalance", loyaltyService.getBalance(user.getId()));
            });
        }
        return "loyalty/rewards";
    }

    @PostMapping("/rewards/{id}/redeem")
    public String redeemReward(@PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));

            rewardService.redeemReward(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Reward redeemed successfully!");
        } catch (InsufficientPointsException | OutOfStockException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to redeem reward: " + ex.getMessage());
        }

        return "redirect:/rewards";
    }
}
