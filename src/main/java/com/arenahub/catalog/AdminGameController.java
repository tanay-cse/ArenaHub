package com.arenahub.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/games")
public class AdminGameController {

    private final CatalogService catalogService;

    public AdminGameController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String listGames(Model model) {
        model.addAttribute("games", catalogService.getAllGames());
        if (!model.containsAttribute("game")) {
            model.addAttribute("game", new Game());
        }
        return "catalog/admin-games";
    }

    @PostMapping
    public String saveGame(@ModelAttribute("game") Game game, RedirectAttributes redirectAttributes) {
        try {
            catalogService.saveGame(game);
            redirectAttributes.addFlashAttribute("successMessage", "Game saved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving game: " + ex.getMessage());
        }
        return "redirect:/admin/games";
    }

    @GetMapping("/edit/{id}")
    public String editGame(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return catalogService.getGameById(id)
                .map(game -> {
                    model.addAttribute("games", catalogService.getAllGames());
                    model.addAttribute("game", game);
                    return "catalog/admin-games";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Game not found.");
                    return "redirect:/admin/games";
                });
    }

    @PostMapping("/toggle/{id}")
    public String toggleGame(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            catalogService.toggleGameActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Game active status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error toggling game status: " + ex.getMessage());
        }
        return "redirect:/admin/games";
    }
}
