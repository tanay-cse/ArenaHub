package com.arenahub.booking;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final UserRepository userRepository;

    public WaitlistController(WaitlistService waitlistService, UserRepository userRepository) {
        this.waitlistService = waitlistService;
        this.userRepository = userRepository;
    }

    @GetMapping("/waitlist")
    public String showWaitlist(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));

        List<WaitlistEntry> entries = waitlistService.getWaitlistForUser(user.getId());
        model.addAttribute("entries", entries);
        return "booking/waitlist";
    }

    @PostMapping({"/waitlist", "/waitlist/join"})
    public String joinWaitlist(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam Long stationId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam int hour,
                               RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));

            waitlistService.join(user.getId(), stationId, date, hour);
            redirectAttributes.addFlashAttribute("successMessage", "Joined waitlist successfully! You will be notified if this slot becomes available.");
            return "redirect:/waitlist";
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/new";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to join waitlist: " + ex.getMessage());
            return "redirect:/bookings/new";
        }
    }
}
