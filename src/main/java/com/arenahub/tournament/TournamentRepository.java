package com.arenahub.tournament;

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
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query("SELECT DISTINCT t FROM Tournament t LEFT JOIN FETCH t.players ORDER BY t.startTime ASC")
    List<Tournament> findAllWithPlayers();

    List<Tournament> findAllByOrderByStartTimeAsc();

    long countByStartTimeAfterAndStatusNot(LocalDateTime startTime, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tournament t WHERE t.id = :id")
    Optional<Tournament> findByIdForUpdate(@Param("id") Long id);
}
