package com.arenahub.booking;

import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class StatusBoardController {

    private final StationRepository stationRepository;
    private final BookingRepository bookingRepository;

    public StatusBoardController(StationRepository stationRepository, BookingRepository bookingRepository) {
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
    }

    public record StationStatusDTO(
            Long stationId,
            String stationNumber,
            String stationType,
            BigDecimal hourlyRate,
            String status,
            String endTime,
            String activeUser
    ) {}

    @GetMapping("/status-board")
    public String showStatusBoard(Model model) {
        List<StationStatusDTO> stations = getStationStatuses();
        model.addAttribute("stations", stations);
        return "booking/status-board";
    }

    @GetMapping("/status-board/data")
    @ResponseBody
    public List<StationStatusDTO> getStatusBoardData() {
        return getStationStatuses();
    }

    private List<StationStatusDTO> getStationStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Station> allStations = stationRepository.findAll();
        List<Booking> activeBookings = bookingRepository.findCurrentlyActiveBookings(now);

        Map<Long, Booking> bookingByStation = new HashMap<>();
        for (Booking b : activeBookings) {
            bookingByStation.put(b.getStation().getId(), b);
        }

        List<StationStatusDTO> list = new ArrayList<>();
        for (Station s : allStations) {
            String status;
            String endTime = null;
            String userName = null;

            if ("MAINTENANCE".equalsIgnoreCase(s.getStatus())) {
                status = "MAINTENANCE";
            } else if (bookingByStation.containsKey(s.getId())) {
                status = "ACTIVE";
                Booking b = bookingByStation.get(s.getId());
                endTime = b.getEndTime().toString();
                userName = b.getUser() != null ? b.getUser().getFullName() : null;
            } else {
                status = "AVAILABLE";
            }

            list.add(new StationStatusDTO(
                    s.getId(),
                    s.getStationNumber(),
                    s.getStationType(),
                    s.getHourlyRate(),
                    status,
                    endTime,
                    userName
            ));
        }
        return list;
    }
}
