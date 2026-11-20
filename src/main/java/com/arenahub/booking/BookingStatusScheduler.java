package com.arenahub.booking;

import com.arenahub.audit.AuditService;
import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import com.arenahub.loyalty.LoyaltyService;
import com.arenahub.loyalty.PointsLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class BookingStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingStatusScheduler.class);

    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final LoyaltyService loyaltyService;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final AuditService auditService;

    public BookingStatusScheduler(BookingRepository bookingRepository,
                                  StationRepository stationRepository,
                                  LoyaltyService loyaltyService,
                                  PointsLedgerRepository pointsLedgerRepository,
                                  AuditService auditService) {
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
        this.loyaltyService = loyaltyService;
        this.pointsLedgerRepository = pointsLedgerRepository;
        this.auditService = auditService;
    }

    @Scheduled(fixedRate = 60000)
    public void runTransitions() {
        LocalDateTime now = LocalDateTime.now();
        transitionConfirmedToActive(now);
        transitionActiveToCompleted(now);
    }

    @Transactional
    public int transitionConfirmedToActive(LocalDateTime now) {
        List<Booking> toActivate = bookingRepository.findConfirmedToActivate(now);
        int count = 0;
        for (Booking booking : toActivate) {
            String oldStatus = booking.getStatus();
            booking.setStatus("ACTIVE");
            bookingRepository.save(booking);

            Station station = booking.getStation();
            if (station != null && !"MAINTENANCE".equalsIgnoreCase(station.getStatus())) {
                station.setStatus("OCCUPIED");
                stationRepository.save(station);
            }

            Long userId = booking.getUser() != null ? booking.getUser().getId() : null;
            auditService.log(
                    userId,
                    "BOOKING_ACTIVE",
                    "bookings",
                    booking.getId(),
                    "{\"status\":\"" + oldStatus + "\"}",
                    "{\"status\":\"ACTIVE\"}"
            );
            count++;
        }
        if (count > 0) {
            log.info("Transitioned {} bookings from CONFIRMED to ACTIVE", count);
        }
        return count;
    }

    @Transactional
    public int transitionActiveToCompleted(LocalDateTime now) {
        List<Booking> toComplete = bookingRepository.findActiveToComplete(now);
        int count = 0;
        for (Booking booking : toComplete) {
            String oldStatus = booking.getStatus();
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);

            Station station = booking.getStation();
            if (station != null && "OCCUPIED".equalsIgnoreCase(station.getStatus())) {
                station.setStatus("AVAILABLE");
                stationRepository.save(station);
            }

            Long userId = booking.getUser() != null ? booking.getUser().getId() : null;
            if (userId != null && !pointsLedgerRepository.existsByBookingId(booking.getId())) {
                loyaltyService.addPoints(
                        userId,
                        10,
                        "EARNED_BOOKING",
                        "Completed booking #" + booking.getId() + " at " + (station != null ? station.getStationNumber() : "station"),
                        booking
                );
            }

            auditService.log(
                    userId,
                    "BOOKING_COMPLETED",
                    "bookings",
                    booking.getId(),
                    "{\"status\":\"" + oldStatus + "\"}",
                    "{\"status\":\"COMPLETED\"}"
            );
            count++;
        }
        if (count > 0) {
            log.info("Transitioned {} bookings from ACTIVE to COMPLETED", count);
        }
        return count;
    }
}
