package com.arenahub.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query("SELECT DISTINCT t FROM Tournament t LEFT JOIN FETCH t.players ORDER BY t.startTime ASC")
    List<Tournament> findAllWithPlayers();

    List<Tournament> findAllByOrderByStartTimeAsc();
}
