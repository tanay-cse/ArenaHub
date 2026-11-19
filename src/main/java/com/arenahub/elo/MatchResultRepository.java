package com.arenahub.elo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByUserId(Long userId);

    List<MatchResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<MatchResult> findFirstByMatchIdAndUserIdNot(Long matchId, Long userId);
}
