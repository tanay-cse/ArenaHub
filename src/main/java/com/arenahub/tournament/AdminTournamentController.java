package com.arenahub.tournament;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/tournaments")
public class AdminTournamentController {

    private final TournamentService tournamentService;
    private final CatalogService catalogService;
    private final UserRepository userRepository;

    public AdminTournamentController(TournamentService tournamentService,
                                   CatalogService catalogService,
                                   UserRepository userRepository) {
        this.tournamentService = tournamentService;
        this.catalogService = catalogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("tournament")) {
            Tournament tournament = new Tournament();
            tournament.setFormat("SINGLE_ELIMINATION");
            tournament.setStatus("REGISTRATION_OPEN");
            model.addAttribute("tournament", tournament);
        }
        model.addAttribute("games", catalogService.getActiveGames());
        return "tournament/admin-tournament-new";
    }

    @PostMapping("/new")
    public String createTournament(@ModelAttribute("tournament") Tournament tournament,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = null;
            if (principal != null) {
                User admin = userRepository.findByEmail(principal.getName()).orElse(null);
                if (admin != null) {
                    adminUserId = admin.getId();
                }
            }

            tournamentService.createTournament(tournament, adminUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Tournament '" + tournament.getName() + "' created successfully.");
            return "redirect:/tournaments";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating tournament: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("tournament", tournament);
            return "redirect:/admin/tournaments/new";
        }
    }
}
