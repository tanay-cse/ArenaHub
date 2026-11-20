package com.arenahub.booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    Optional<Booking> findByCheckinCode(String checkinCode);

    boolean existsByCheckinCode(String checkinCode);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.station.id = :stationId AND b.startTime = :startTime AND b.status <> :status")
    boolean existsActiveBooking(@Param("stationId") Long stationId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("status") String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b FROM Booking b WHERE b.station.id = :stationId AND b.startTime >= :startOfDay AND b.startTime < :endOfDay AND b.status <> 'CANCELLED'")
    List<Booking> findActiveBookingsForStationAndDate(@Param("stationId") Long stationId,
                                                    @Param("startOfDay") LocalDateTime startOfDay,
                                                    @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.startTime <= :now")
    List<Booking> findConfirmedToActivate(@Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.status = 'ACTIVE' AND b.endTime <= :now")
    List<Booking> findActiveToComplete(@Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.status = 'ACTIVE' AND b.startTime <= :now AND b.endTime > :now")
    List<Booking> findCurrentlyActiveBookings(@Param("now") LocalDateTime now);
}
