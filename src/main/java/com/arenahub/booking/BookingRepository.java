package com.arenahub.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.station.id = :stationId AND b.startTime = :startTime AND b.status <> :status")
    boolean existsActiveBooking(@Param("stationId") Long stationId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("status") String status);
}
