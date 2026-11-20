package com.arenahub.admin;

import com.arenahub.auth.UserRepository;
import com.arenahub.booking.BookingRepository;
import com.arenahub.booking.WaitlistRepository;
import com.arenahub.catalog.StationRepository;
import com.arenahub.elo.MatchRepository;
import com.arenahub.tournament.TournamentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final BookingRepository bookingRepository;
    private final TournamentRepository tournamentRepository;
    private final WaitlistRepository waitlistRepository;
    private final MatchRepository matchRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    StationRepository stationRepository,
                                    BookingRepository bookingRepository,
                                    TournamentRepository tournamentRepository,
                                    WaitlistRepository waitlistRepository,
                                    MatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
        this.tournamentRepository = tournamentRepository;
        this.waitlistRepository = waitlistRepository;
        this.matchRepository = matchRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        long totalUsers = userRepository.count();
        long totalStations = stationRepository.count();
        long totalBookings = bookingRepository.count();
        long upcomingTournaments = tournamentRepository.countByStartTimeAfterAndStatusNot(LocalDateTime.now(), "CANCELLED");
        long openWaitlistEntries = waitlistRepository.countByStatus("WAITING");
        long pendingDisputes = matchRepository.countByStatus("DISPUTED");

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStations", totalStations);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("upcomingTournaments", upcomingTournaments);
        model.addAttribute("openWaitlistEntries", openWaitlistEntries);
        model.addAttribute("pendingDisputes", pendingDisputes);

        return "admin/dashboard";
    }
}
