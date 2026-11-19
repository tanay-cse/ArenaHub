package com.arenahub.booking;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           BookingRepository bookingRepository,
                           StationRepository stationRepository,
                           UserRepository userRepository) {
        this.waitlistRepository = waitlistRepository;
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntry> getWaitlistForUser(Long userId) {
        return waitlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public WaitlistEntry join(Long userId, Long stationId, LocalDate date, int hour) {
        if (hour < 10 || hour > 21) {
            throw new IllegalArgumentException("Waitlist hour must be between 10 and 21.");
        }

        LocalDateTime slotStart = date.atTime(hour, 0);
        if (!slotStart.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Waitlist is only available for future time slots.");
        }

        if (waitlistRepository.existsByUserIdAndStationIdAndRequestedDateAndRequestedHour(userId, stationId, date, (byte) hour)) {
            throw new IllegalStateException("You are already on the waitlist for this station and time slot.");
        }

        // Only allowed when that exact slot is currently booked (not available)
        boolean isBooked = bookingRepository.existsActiveBooking(stationId, slotStart, "CANCELLED");
        if (!isBooked) {
            throw new IllegalArgumentException("Waitlist is only allowed when that exact slot is currently booked.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setStation(station);
        entry.setRequestedDate(date);
        entry.setRequestedHour(hour);
        entry.setStatus("WAITING");
        entry.setCreatedAt(LocalDateTime.now());

        try {
            return waitlistRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("You are already on the waitlist for this station and time slot.", ex);
        }
    }

    @Transactional
    public Optional<WaitlistEntry> notifyNext(Long stationId, LocalDate date, int hour) {
        Optional<WaitlistEntry> oldest = waitlistRepository
                .findFirstByStationIdAndRequestedDateAndRequestedHourAndStatusOrderByCreatedAtAsc(
                        stationId, date, (byte) hour, "WAITING"
                );
        if (oldest.isPresent()) {
            WaitlistEntry entry = oldest.get();
            entry.setStatus("NOTIFIED");
            entry.setNotifiedAt(LocalDateTime.now());
            return Optional.of(waitlistRepository.save(entry));
        }
        return Optional.empty();
    }
}
