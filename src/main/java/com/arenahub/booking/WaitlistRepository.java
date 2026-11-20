package com.arenahub.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WaitlistEntry> findFirstByStationIdAndRequestedDateAndRequestedHourAndStatusOrderByCreatedAtAsc(
            Long stationId, LocalDate requestedDate, Byte requestedHour, String status);

    boolean existsByUserIdAndStationIdAndRequestedDateAndRequestedHour(
            Long userId, Long stationId, LocalDate requestedDate, Byte requestedHour);

    long countByStatus(String status);
}
