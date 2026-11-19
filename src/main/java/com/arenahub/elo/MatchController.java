package com.arenahub.elo;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.GameRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class MatchController {

    private final EloService eloService;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public MatchController(EloService eloService,
                           UserRepository userRepository,
                           GameRepository gameRepository) {
        this.eloService = eloService;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    @GetMapping("/matches/new")
    public String showRecordMatchForm(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));

        List<User> opponents = userRepository.findByIsActiveTrue().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .toList();

        model.addAttribute("games", gameRepository.findByIsActiveTrue());
        model.addAttribute("opponents", opponents);

        return "elo/match-new";
    }

    @PostMapping("/matches/new")
    public String recordMatch(@RequestParam("gameId") Long gameId,
                              @RequestParam("opponentId") Long opponentId,
                              @RequestParam("matchType") String matchType,
                              @RequestParam("yourScore") Integer yourScore,
                              @RequestParam("opponentScore") Integer opponentScore,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User currentUser = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));

            eloService.recordMatch(currentUser.getId(), opponentId, gameId, matchType, yourScore, opponentScore);
            redirectAttributes.addFlashAttribute("successMessage", "Match recorded successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error recording match: " + ex.getMessage());
        }

        return "redirect:/matches/new";
    }
}
