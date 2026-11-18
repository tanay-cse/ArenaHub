package com.arenahub.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByStationNumber(String stationNumber);
    boolean existsByStationNumber(String stationNumber);

    @Query("SELECT DISTINCT s FROM Station s LEFT JOIN FETCH s.stationGames sg LEFT JOIN FETCH sg.game ORDER BY s.stationNumber ASC")
    List<Station> findAllWithGames();
}
