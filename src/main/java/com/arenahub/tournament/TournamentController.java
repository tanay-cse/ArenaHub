package com.arenahub.tournament;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

@Controller
public class TournamentController {

    private final TournamentService tournamentService;
    private final UserRepository userRepository;

    public TournamentController(TournamentService tournamentService, UserRepository userRepository) {
        this.tournamentService = tournamentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/tournaments")
    public String viewTournaments(Principal principal, Model model) {
        model.addAttribute("tournaments", tournamentService.getAllTournaments());

        Set<Long> joinedIds = Collections.emptySet();
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                joinedIds = tournamentService.getJoinedTournamentIds(user.getId());
            }
        }
        model.addAttribute("joinedTournamentIds", joinedIds);

        return "tournament/tournaments";
    }

    @PostMapping("/tournaments/{id}/join")
    public String joinTournament(@PathVariable("id") Long id,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getName()));

            tournamentService.joinTournament(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Successfully joined the tournament!");
        } catch (TournamentAlreadyJoinedException | TournamentFullException | IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while joining the tournament.");
        }

        return "redirect:/tournaments";
    }
}
