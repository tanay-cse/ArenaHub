package com.arenahub.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/stations")
public class AdminStationController {

    private final CatalogService catalogService;

    public AdminStationController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String listStations(Model model) {
        model.addAttribute("stations", catalogService.getAllStationsWithGames());
        model.addAttribute("allGames", catalogService.getActiveGames());
        if (!model.containsAttribute("station")) {
            model.addAttribute("station", new Station());
        }
        return "catalog/admin-stations";
    }

    @PostMapping
    public String saveStation(@ModelAttribute("station") Station station, RedirectAttributes redirectAttributes) {
        try {
            catalogService.saveStation(station);
            redirectAttributes.addFlashAttribute("successMessage", "Station saved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving station: " + ex.getMessage());
        }
        return "redirect:/admin/stations";
    }

    @GetMapping("/edit/{id}")
    public String editStation(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return catalogService.getStationById(id)
                .map(station -> {
                    model.addAttribute("stations", catalogService.getAllStationsWithGames());
                    model.addAttribute("allGames", catalogService.getActiveGames());
                    model.addAttribute("station", station);
                    return "catalog/admin-stations";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Station not found.");
                    return "redirect:/admin/stations";
                });
    }

    @PostMapping("/delete/{id}")
    public String deleteStation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            catalogService.deleteStation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Station deleted successfully.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to delete station: related records exist.");
        }
        return "redirect:/admin/stations";
    }

    @PostMapping("/{stationId}/games/install")
    public String installGame(@PathVariable Long stationId, @RequestParam Long gameId, RedirectAttributes redirectAttributes) {
        try {
            catalogService.installGameOnStation(stationId, gameId);
            redirectAttributes.addFlashAttribute("successMessage", "Game installed on station.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not install game: " + ex.getMessage());
        }
        return "redirect:/admin/stations";
    }

    @PostMapping("/{stationId}/games/{gameId}/remove")
    public String removeGame(@PathVariable Long stationId, @PathVariable Long gameId, RedirectAttributes redirectAttributes) {
        try {
            catalogService.removeGameFromStation(stationId, gameId);
            redirectAttributes.addFlashAttribute("successMessage", "Game removed from station.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove game: " + ex.getMessage());
        }
        return "redirect:/admin/stations";
    }
}
