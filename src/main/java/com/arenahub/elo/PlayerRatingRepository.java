package com.arenahub.elo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRatingRepository extends JpaRepository<PlayerRating, Long> {

    Optional<PlayerRating> findByUserIdAndGameId(Long userId, Long gameId);
}
