package com.arenahub.booking;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.CatalogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final CatalogService catalogService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService,
                             CatalogService catalogService,
                             UserRepository userRepository) {
        this.bookingService = bookingService;
        this.catalogService = catalogService;
        this.userRepository = userRepository;
    }

    @GetMapping({"", "/new"})
    public String showBookingPage(@RequestParam(value = "stationId", required = false) Long stationId,
                                  @RequestParam(value = "station_id", required = false) Long stationIdAlt,
                                  @RequestParam(value = "date", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam(value = "hour", required = false) Integer hour,
                                  Model model) {
        Long targetStationId = stationId != null ? stationId : stationIdAlt;
        model.addAttribute("stations", catalogService.getAllStations());
        if (targetStationId != null) {
            model.addAttribute("selectedStationId", targetStationId);
        }
        if (date != null && hour != null) {
            LocalDateTime prefilled = date.atTime(hour, 0);
            model.addAttribute("prefilledStartTime", prefilled.toString());
        }
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
            return "redirect:/bookings";
        } catch (SlotAlreadyBookedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("unavailableStationId", stationId);
            redirectAttributes.addFlashAttribute("unavailableDate", startTime.toLocalDate());
            redirectAttributes.addFlashAttribute("unavailableHour", startTime.getHour());
            redirectAttributes.addFlashAttribute("showWaitlistButton", true);
            return "redirect:/bookings/new";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to create booking: " + ex.getMessage());
            return "redirect:/bookings";
        }
    }

    @GetMapping("/mine")
    public String showMyBookings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));

        List<Booking> bookings = bookingService.getBookingsForUser(user.getId());
        model.addAttribute("bookings", bookings);
        return "booking/mine";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));

            bookingService.cancelBooking(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully.");
        } catch (BookingNotCancellableException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to cancel booking: " + ex.getMessage());
        }
        return "redirect:/bookings/mine";
    }

    @GetMapping("/{id}/reschedule")
    public String showRescheduleForm(@PathVariable("id") Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));

        Booking booking = bookingService.getBookingById(id);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings/mine";
        }

        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        boolean isStaffOrAdmin = "ADMIN".equalsIgnoreCase(roleName) || "STAFF".equalsIgnoreCase(roleName)
                || "ROLE_ADMIN".equalsIgnoreCase(roleName) || "ROLE_STAFF".equalsIgnoreCase(roleName);
        boolean isOwner = booking.getUser() != null && booking.getUser().getId().equals(user.getId());

        if (!isOwner && !isStaffOrAdmin) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to reschedule this booking.");
            return "redirect:/bookings/mine";
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot reschedule a booking that has already started or passed.");
            return "redirect:/bookings/mine";
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "COMPLETED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot reschedule a booking that is " + booking.getStatus() + ".");
            return "redirect:/bookings/mine";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("stations", catalogService.getAllStations());
        return "booking/reschedule";
    }

    @PostMapping("/{id}/reschedule")
    public String rescheduleBooking(@PathVariable("id") Long id,
                                    @RequestParam(value = "station_id", required = false) Long stationId,
                                    @RequestParam(value = "stationId", required = false) Long stationIdFallback,
                                    @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(value = "hour", required = false) Integer hour,
                                    @RequestParam(value = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Long targetStationId = stationId != null ? stationId : stationIdFallback;
        if (targetStationId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a station.");
            return "redirect:/bookings/" + id + "/reschedule";
        }

        LocalDateTime newStartTime;
        if (date != null && hour != null) {
            newStartTime = date.atTime(hour, 0);
        } else if (startTime != null) {
            newStartTime = startTime;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid date and hour.");
            return "redirect:/bookings/" + id + "/reschedule";
        }

        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));

            bookingService.rescheduleBooking(id, user.getId(), targetStationId, newStartTime);
            redirectAttributes.addFlashAttribute("successMessage", "Booking rescheduled successfully!");
            return "redirect:/bookings/mine";
        } catch (SlotAlreadyBookedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/" + id + "/reschedule";
        } catch (BookingNotCancellableException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/" + id + "/reschedule";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to reschedule booking: " + ex.getMessage());
            return "redirect:/bookings/" + id + "/reschedule";
        }
    }
}
