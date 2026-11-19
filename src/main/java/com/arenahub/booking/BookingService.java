package com.arenahub.booking;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          StationRepository stationRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    @Transactional
    public Booking createBooking(String userEmail, Long stationId, LocalDateTime startTime) {
        LocalDateTime normalizedStartTime = startTime.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime normalizedEndTime = normalizedStartTime.plusHours(1);

        // Pre-check for active booking on the same station and slot
        if (bookingRepository.existsActiveBooking(stationId, normalizedStartTime, "CANCELLED")) {
            throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStation(station);
        booking.setStartTime(normalizedStartTime);
        booking.setEndTime(normalizedEndTime);
        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(station.getHourlyRate());

        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException("Slot already booked for this station. Please choose another time or station.", ex);
        }
    }
}
