package com.arenahub.tournament;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class TournamentMatchController {

    private final TournamentService tournamentService;
    private final UserRepository userRepository;

    public TournamentMatchController(TournamentService tournamentService, UserRepository userRepository) {
        this.tournamentService = tournamentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/matches/{id}/report")
    public String showReportForm(@PathVariable("id") Long id,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User currentUser = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));

            boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());
            TournamentService.MatchReportDetails details = tournamentService.getMatchForReporting(id, currentUser.getId(), isAdmin);

            model.addAttribute("details", details);
            model.addAttribute("currentUser", currentUser);
            return "tournament/match-report";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/tournaments";
        }
    }

    @PostMapping("/matches/{id}/report")
    public String submitReport(@PathVariable("id") Long id,
                               @RequestParam(value = "player1Id", required = false) Long player1Id,
                               @RequestParam(value = "player1Score", required = false) Integer player1Score,
                               @RequestParam(value = "player2Id", required = false) Long player2Id,
                               @RequestParam(value = "player2Score", required = false) Integer player2Score,
                               @RequestParam(value = "yourScore", required = false) Integer yourScore,
                               @RequestParam(value = "opponentScore", required = false) Integer opponentScore,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User currentUser = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));
            boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

            // Handle yourScore / opponentScore mapping if submitted by a player
            if (player1Score == null && player2Score == null && yourScore != null && opponentScore != null) {
                TournamentService.MatchReportDetails details = tournamentService.getMatchForReporting(id, currentUser.getId(), isAdmin);
                if (details.player1Id().equals(currentUser.getId())) {
                    player1Id = details.player1Id();
                    player1Score = yourScore;
                    player2Id = details.player2Id();
                    player2Score = opponentScore;
                } else {
                    player2Id = details.player2Id();
                    player2Score = yourScore;
                    player1Id = details.player1Id();
                    player1Score = opponentScore;
                }
            }

            tournamentService.reportMatchResult(id, player1Id, player1Score, player2Id, player2Score, currentUser.getId(), isAdmin);
            redirectAttributes.addFlashAttribute("successMessage", "Match result recorded successfully!");

            if (isAdmin) {
                return "redirect:/admin/matches/disputes";
            }
            return "redirect:/matches/mine";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error recording match result: " + ex.getMessage());
            return "redirect:/matches/" + id + "/report";
        }
    }

    @PostMapping("/matches/{id}/dispute")
    public String raiseDispute(@PathVariable("id") Long id,
                               @RequestParam("reason") String reason,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User currentUser = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));
            boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

            tournamentService.raiseDispute(id, reason, currentUser.getId(), isAdmin);
            redirectAttributes.addFlashAttribute("successMessage", "Dispute raised successfully. An administrator will review and resolve the scores.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error raising dispute: " + ex.getMessage());
        }

        return "redirect:/matches/mine";
    }

    @GetMapping("/admin/matches/disputes")
    public String listDisputes(Model model) {
        List<TournamentService.DisputedMatchDTO> disputes = tournamentService.getDisputedMatches();
        model.addAttribute("disputes", disputes);
        return "tournament/admin-disputes";
    }
}
