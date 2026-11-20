package com.arenahub.booking;

import com.arenahub.audit.AuditService;
import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           BookingRepository bookingRepository,
                           StationRepository stationRepository,
                           UserRepository userRepository,
                           AuditService auditService,
                           ObjectMapper objectMapper) {
        this.waitlistRepository = waitlistRepository;
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
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

        WaitlistEntry saved;
        try {
            saved = waitlistRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("You are already on the waitlist for this station and time slot.", ex);
        }

        auditService.log(userId, "JOIN_WAITLIST", "waitlist", saved.getId(), null, toJson(Map.of("stationId", stationId, "requestedDate", date.toString(), "requestedHour", hour, "status", "WAITING")));

        return saved;
    }

    @Transactional
    public Optional<WaitlistEntry> notifyNext(Long stationId, LocalDate date, int hour) {
        Optional<WaitlistEntry> oldest = waitlistRepository
                .findFirstByStationIdAndRequestedDateAndRequestedHourAndStatusOrderByCreatedAtAsc(
                        stationId, date, (byte) hour, "WAITING"
                );
        if (oldest.isPresent()) {
            WaitlistEntry entry = oldest.get();
            String oldStatus = entry.getStatus();
            entry.setStatus("NOTIFIED");
            entry.setNotifiedAt(LocalDateTime.now());
            WaitlistEntry saved = waitlistRepository.save(entry);
            auditService.log(entry.getUser() != null ? entry.getUser().getId() : null, "AUTO_NOTIFY_WAITLIST", "waitlist", saved.getId(), toJson(Map.of("status", oldStatus)), toJson(Map.of("status", "NOTIFIED", "notifiedAt", saved.getNotifiedAt().toString())));
            return Optional.of(saved);
        }
        return Optional.empty();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
