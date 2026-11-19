package com.arenahub.booking;

import com.arenahub.catalog.CatalogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final CatalogService catalogService;

    public BookingController(BookingService bookingService, CatalogService catalogService) {
        this.bookingService = bookingService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String showBookingPage(Model model) {
        model.addAttribute("stations", catalogService.getAllStations());
        return "booking/bookings";
    }

    @PostMapping
    public String bookSlot(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam Long stationId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                           RedirectAttributes redirectAttributes) {
        try {
            bookingService.createBooking(userDetails.getUsername(), stationId, startTime);
            redirectAttributes.addFlashAttribute("successMessage", "Booking confirmed successfully!");
        } catch (SlotAlreadyBookedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to create booking: " + ex.getMessage());
        }
        return "redirect:/bookings";
    }
}
