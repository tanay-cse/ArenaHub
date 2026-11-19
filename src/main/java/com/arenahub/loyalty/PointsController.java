package com.arenahub.loyalty;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class PointsController {

    private final LoyaltyService loyaltyService;
    private final UserRepository userRepository;

    public PointsController(LoyaltyService loyaltyService, UserRepository userRepository) {
        this.loyaltyService = loyaltyService;
        this.userRepository = userRepository;
    }

    @GetMapping("/points")
    public String viewPoints(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));

        int balance = loyaltyService.getBalance(user.getId());
        List<PointsLedger> history = loyaltyService.getHistory(user.getId());

        model.addAttribute("balance", balance);
        model.addAttribute("history", history);
        return "loyalty/points";
    }
}
