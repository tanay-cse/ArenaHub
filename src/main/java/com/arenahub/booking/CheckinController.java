package com.arenahub.booking;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkin")
public class CheckinController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public CheckinController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showCheckinScanner(Model model) {
        return "booking/checkin";
    }

    @PostMapping("/{code}")
    public String processCheckin(@PathVariable("code") String code,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        Long userId = null;
        if (userDetails != null) {
            userId = userRepository.findByEmail(userDetails.getUsername())
                    .map(User::getId)
                    .orElse(null);
        }

        try {
            Booking booking = bookingService.checkin(code, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Check-in successful! Station " + booking.getStation().getStationNumber()
                            + " reservation confirmed (Booking #" + booking.getId() + ").");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to complete check-in: " + ex.getMessage());
        }

        return "redirect:/checkin";
    }

    @PostMapping
    public String processManualCheckin(@RequestParam("code") String code,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        return processCheckin(code, userDetails, redirectAttributes);
    }
}
