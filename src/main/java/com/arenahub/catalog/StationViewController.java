package com.arenahub.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StationViewController {

    private final CatalogService catalogService;

    public StationViewController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/stations")
    public String viewStations(Model model) {
        model.addAttribute("stations", catalogService.getAllStationsWithGames());
        return "catalog/stations";
    }
}
