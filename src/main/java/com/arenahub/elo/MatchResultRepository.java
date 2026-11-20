package com.arenahub.elo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByUserId(Long userId);

    List<MatchResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<MatchResult> findByMatchId(Long matchId);

    Optional<MatchResult> findFirstByMatchIdAndUserIdNot(Long matchId, Long userId);

    @Query("SELECT mr FROM MatchResult mr JOIN FETCH mr.match m JOIN FETCH m.game g WHERE mr.user.id = :userId ORDER BY m.completedAt ASC, mr.id ASC")
    List<MatchResult> findUserResultsChronological(@Param("userId") Long userId);

    @Query("SELECT mr FROM MatchResult mr JOIN mr.match m WHERE mr.user.id = :userId AND m.game.id = :gameId ORDER BY m.completedAt DESC, mr.id DESC")
    List<MatchResult> findRecentResultsByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId, Pageable pageable);

    @Query("SELECT COUNT(mr) FROM MatchResult mr WHERE mr.user.id = :userId AND mr.outcome = 'WIN'")
    long countWinsByUserId(@Param("userId") Long userId);
}
