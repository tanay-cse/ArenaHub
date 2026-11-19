package com.arenahub.elo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRatingRepository extends JpaRepository<PlayerRating, Long> {

    Optional<PlayerRating> findByUserIdAndGameId(Long userId, Long gameId);

    @Query(value = """
        SELECT 
            u.id,
            u.full_name,
            u.email,
            pr.current_rating,
            CAST(COALESCE(pms.matches_played, pr.matches_played) AS SIGNED) AS matches_played,
            CAST(COALESCE(pms.wins, 0) AS SIGNED) AS wins,
            CAST(COALESCE(pms.losses, 0) AS SIGNED) AS losses,
            CAST(COALESCE(pms.draws, 0) AS SIGNED) AS draws
        FROM player_ratings pr
        JOIN users u ON pr.user_id = u.id
        LEFT JOIN player_match_stats pms ON pms.user_id = pr.user_id AND pms.game_id = pr.game_id
        WHERE pr.game_id = :gameId
        ORDER BY pr.current_rating DESC
        """, nativeQuery = true)
    List<Object[]> findLeaderboardForGame(@Param("gameId") Long gameId);
}
