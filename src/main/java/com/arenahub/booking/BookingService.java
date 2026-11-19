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

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final WaitlistService waitlistService;
    private final PricingService pricingService;

    public BookingService(BookingRepository bookingRepository,
                          StationRepository stationRepository,
                          UserRepository userRepository,
                          WaitlistService waitlistService,
                          PricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.waitlistService = waitlistService;
        this.pricingService = pricingService;
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

        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.", ex);
        }
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

        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);

        // Notify next in waitlist for this slot
        LocalDate date = booking.getStartTime().toLocalDate();
        int hour = booking.getStartTime().getHour();
        waitlistService.notifyNext(booking.getStation().getId(), date, hour);

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

        booking.setStation(station);
        booking.setStartTime(normalizedStartTime);
        booking.setEndTime(normalizedEndTime);
        booking.setTotalPrice(pricingService.calculatePrice(newStationId, normalizedStartTime.getHour()));

        try {
            return bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.", ex);
        }
    }
}
