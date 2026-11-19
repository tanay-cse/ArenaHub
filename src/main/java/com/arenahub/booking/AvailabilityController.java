package com.arenahub.booking;

import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class AvailabilityController {

    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;

    public AvailabilityController(BookingRepository bookingRepository,
                                  StationRepository stationRepository) {
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
    }

    @GetMapping("/stations/{id}/availability")
    public String getAvailability(@PathVariable("id") Long id,
                                  @RequestParam(value = "date", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Model model) {
        Station station = stationRepository.findById(id).orElse(null);
        if (station == null) {
            return "redirect:/stations";
        }

        if (date == null) {
            date = LocalDate.now();
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isMaintenance = "MAINTENANCE".equalsIgnoreCase(station.getStatus());

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<Booking> activeBookings = bookingRepository.findActiveBookingsForStationAndDate(id, startOfDay, endOfDay);
        Set<Integer> bookedHours = activeBookings.stream()
                .map(b -> b.getStartTime().getHour())
                .collect(Collectors.toSet());

        List<AvailabilitySlot> slots = new ArrayList<>();
        for (int hour = 10; hour <= 21; hour++) {
            LocalDateTime slotStart = date.atTime(hour, 0);
            String timeLabel = String.format("%02d:00 – %02d:00", hour, hour + 1);
            String status;
            boolean available = false;

            if (isMaintenance) {
                status = "MAINTENANCE";
            } else if (slotStart.isBefore(now)) {
                status = "PAST";
            } else if (bookedHours.contains(hour)) {
                status = "BOOKED";
            } else {
                status = "AVAILABLE";
                available = true;
            }

            slots.add(new AvailabilitySlot(hour, timeLabel, status, available));
        }

        model.addAttribute("station", station);
        model.addAttribute("date", date);
        model.addAttribute("slots", slots);

        return "booking/availability";
    }

    public static class AvailabilitySlot {
        private final int hour;
        private final String timeLabel;
        private final String status;
        private final boolean available;

        public AvailabilitySlot(int hour, String timeLabel, String status, boolean available) {
            this.hour = hour;
            this.timeLabel = timeLabel;
            this.status = status;
            this.available = available;
        }

        public int getHour() {
            return hour;
        }

        public String getTimeLabel() {
            return timeLabel;
        }

        public String getStatus() {
            return status;
        }

        public boolean isAvailable() {
            return available;
        }
    }
}
