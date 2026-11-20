package com.arenahub.elo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByStatus(String status);

    List<Match> findByTournamentId(Long tournamentId);

    long countByStatus(String status);
}
