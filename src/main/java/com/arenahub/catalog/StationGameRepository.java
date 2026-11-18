package com.arenahub.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationGameRepository extends JpaRepository<StationGame, Long> {
    Optional<StationGame> findByStationIdAndGameId(Long stationId, Long gameId);
    boolean existsByStationIdAndGameId(Long stationId, Long gameId);
    List<StationGame> findByStationId(Long stationId);
    void deleteByStationIdAndGameId(Long stationId, Long gameId);
}
