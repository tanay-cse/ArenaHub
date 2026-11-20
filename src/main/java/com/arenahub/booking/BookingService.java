package com.arenahub.booking;

import com.arenahub.audit.AuditService;
import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import com.arenahub.loyalty.LoyaltyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final WaitlistService waitlistService;
    private final PricingService pricingService;
    private final LoyaltyService loyaltyService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public BookingService(BookingRepository bookingRepository,
                          StationRepository stationRepository,
                          UserRepository userRepository,
                          WaitlistService waitlistService,
                          PricingService pricingService,
                          LoyaltyService loyaltyService,
                          AuditService auditService,
                          ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.waitlistService = waitlistService;
        this.pricingService = pricingService;
        this.loyaltyService = loyaltyService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    @Transactional
    public Booking createBooking(String userEmail, Long stationId, LocalDateTime startTime) {
        LocalDateTime normalizedStartTime = startTime.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime normalizedEndTime = normalizedStartTime.plusHours(1);

        if (!normalizedStartTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking slot must be in the future.");
        }

        int hour = normalizedStartTime.getHour();
        if (hour < 10 || hour > 21) {
            throw new IllegalArgumentException("Booking slots must start between 10:00 and 21:00.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));

        if ("MAINTENANCE".equalsIgnoreCase(station.getStatus())) {
            throw new IllegalArgumentException("Station is currently under maintenance.");
        }

        // Pre-check for active booking on the same station and slot
        if (bookingRepository.existsActiveBooking(stationId, normalizedStartTime, "CANCELLED")) {
            throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStation(station);
        booking.setStartTime(normalizedStartTime);
        booking.setEndTime(normalizedEndTime);
        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(pricingService.calculatePrice(stationId, normalizedStartTime.getHour()));
        booking.setCheckinCode(generateCheckinCode());

        Booking saved;
        try {
            saved = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.", ex);
        }

        loyaltyService.addPoints(
                user.getId(),
                10,
                "EARNED_BOOKING",
                "Booking #" + saved.getId() + " at " + station.getStationNumber(),
                saved
        );

        return saved;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, Long requestingUserId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotCancellableException("Booking not found: " + bookingId));

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new BookingNotCancellableException("User not found: " + requestingUserId));

        String roleName = requestingUser.getRole() != null ? requestingUser.getRole().getName() : "";
        boolean isStaffOrAdmin = "ADMIN".equalsIgnoreCase(roleName) || "STAFF".equalsIgnoreCase(roleName)
                || "ROLE_ADMIN".equalsIgnoreCase(roleName) || "ROLE_STAFF".equalsIgnoreCase(roleName);
        boolean isOwner = booking.getUser() != null && booking.getUser().getId().equals(requestingUserId);

        if (!isOwner && !isStaffOrAdmin) {
            throw new BookingNotCancellableException("You are not authorized to cancel this booking.");
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingNotCancellableException("Cannot cancel a booking that has already started or passed.");
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "COMPLETED".equalsIgnoreCase(booking.getStatus())) {
            throw new BookingNotCancellableException("Booking is already " + booking.getStatus() + ".");
        }

        String oldStatus = booking.getStatus();
        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);

        // Notify next in waitlist for this slot
        LocalDate date = booking.getStartTime().toLocalDate();
        int hour = booking.getStartTime().getHour();
        waitlistService.notifyNext(booking.getStation().getId(), date, hour);

        auditService.log(requestingUserId, "CANCEL_BOOKING", "bookings", saved.getId(), toJson(Map.of("status", oldStatus)), toJson(Map.of("status", "CANCELLED")));

        return saved;
    }

    @Transactional
    public Booking rescheduleBooking(Long bookingId, Long requestingUserId, Long newStationId, LocalDateTime newStartTime) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotCancellableException("Booking not found: " + bookingId));

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new BookingNotCancellableException("User not found: " + requestingUserId));

        String roleName = requestingUser.getRole() != null ? requestingUser.getRole().getName() : "";
        boolean isStaffOrAdmin = "ADMIN".equalsIgnoreCase(roleName) || "STAFF".equalsIgnoreCase(roleName)
                || "ROLE_ADMIN".equalsIgnoreCase(roleName) || "ROLE_STAFF".equalsIgnoreCase(roleName);
        boolean isOwner = booking.getUser() != null && booking.getUser().getId().equals(requestingUserId);

        if (!isOwner && !isStaffOrAdmin) {
            throw new BookingNotCancellableException("You are not authorized to reschedule this booking.");
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingNotCancellableException("Cannot reschedule a booking that has already started or passed.");
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "COMPLETED".equalsIgnoreCase(booking.getStatus())) {
            throw new BookingNotCancellableException("Cannot reschedule a booking that is " + booking.getStatus() + ".");
        }

        LocalDateTime normalizedStartTime = newStartTime.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime normalizedEndTime = normalizedStartTime.plusHours(1);

        if (!normalizedStartTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking slot must be in the future.");
        }

        int hour = normalizedStartTime.getHour();
        if (hour < 10 || hour > 21) {
            throw new IllegalArgumentException("Booking slots must start between 10:00 and 21:00.");
        }

        Station station = stationRepository.findById(newStationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + newStationId));

        if ("MAINTENANCE".equalsIgnoreCase(station.getStatus())) {
            throw new IllegalArgumentException("Station is currently under maintenance.");
        }

        // Check if destination slot is taken by another booking
        if (bookingRepository.existsActiveBooking(newStationId, normalizedStartTime, "CANCELLED")) {
            if (!newStationId.equals(booking.getStation().getId()) || !normalizedStartTime.equals(booking.getStartTime())) {
                throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.");
            }
        }

        Long oldStationId = booking.getStation().getId();
        LocalDateTime oldStartTime = booking.getStartTime();
        LocalDateTime oldEndTime = booking.getEndTime();
        BigDecimal oldPrice = booking.getTotalPrice();

        booking.setStation(station);
        booking.setStartTime(normalizedStartTime);
        booking.setEndTime(normalizedEndTime);
        booking.setTotalPrice(pricingService.calculatePrice(newStationId, normalizedStartTime.getHour()));

        Booking saved;
        try {
            saved = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.", ex);
        }

        loyaltyService.addPoints(
                saved.getUser().getId(),
                10,
                "EARNED_BOOKING",
                "Booking #" + saved.getId() + " at " + station.getStationNumber(),
                saved
        );

        auditService.log(requestingUserId, "RESCHEDULE_BOOKING", "bookings", saved.getId(),
                toJson(Map.of("stationId", oldStationId, "startTime", oldStartTime.toString(), "endTime", oldEndTime.toString(), "totalPrice", oldPrice)),
                toJson(Map.of("stationId", station.getId(), "startTime", normalizedStartTime.toString(), "endTime", normalizedEndTime.toString(), "totalPrice", saved.getTotalPrice())));

        return saved;
    }

    @Transactional
    public Booking checkin(String code, Long requestingUserId) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Check-in code must not be empty.");
        }
        Booking booking = bookingRepository.findByCheckinCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid check-in code. No matching reservation found."));

        if (booking.getCheckedInAt() != null) {
            throw new IllegalStateException("Booking #" + booking.getId() + " has already been checked in at "
                    + booking.getCheckedInAt().toLocalTime().toString().substring(0, 5) + ".");
        }

        String status = booking.getStatus();
        if (!"CONFIRMED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Cannot check in: reservation is currently " + status + ".");
        }

        LocalDate today = LocalDate.now();
        if (!booking.getStartTime().toLocalDate().isEqual(today)) {
            throw new IllegalStateException("Check-in is only available on the day of reservation ("
                    + booking.getStartTime().toLocalDate() + ").");
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setCheckedInAt(now);
        Booking updated = bookingRepository.save(booking);

        auditService.log(requestingUserId, "BOOKING_CHECKIN", "bookings", updated.getId(),
                null, toJson(Map.of("checkinCode", code.trim().toUpperCase(), "checkedInAt", now.toString())));

        return updated;
    }

    private String generateCheckinCode() {
        String characters = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder(10);
            for (int j = 0; j < 10; j++) {
                sb.append(characters.charAt(random.nextInt(characters.length())));
            }
            String code = sb.toString();
            if (!bookingRepository.existsByCheckinCode(code)) {
                return code;
            }
        }
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
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
