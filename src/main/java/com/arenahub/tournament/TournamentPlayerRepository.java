package com.arenahub.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, Long> {

    boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);

    long countByTournamentId(Long tournamentId);

    List<TournamentPlayer> findByTournamentId(Long tournamentId);

    List<TournamentPlayer> findByUserId(Long userId);
}
