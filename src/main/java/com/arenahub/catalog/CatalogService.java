package com.arenahub.catalog;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {

    private final GameRepository gameRepository;
    private final StationRepository stationRepository;
    private final StationGameRepository stationGameRepository;

    public CatalogService(GameRepository gameRepository,
                          StationRepository stationRepository,
                          StationGameRepository stationGameRepository) {
        this.gameRepository = gameRepository;
        this.stationRepository = stationRepository;
        this.stationGameRepository = stationGameRepository;
    }

    // --- Station operations ---
    @Transactional(readOnly = true)
    public List<Station> getAllStationsWithGames() {
        return stationRepository.findAllWithGames();
    }

    @Transactional(readOnly = true)
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Station> getStationById(Long id) {
        return stationRepository.findById(id);
    }

    @Transactional
    public Station saveStation(Station station) {
        return stationRepository.save(station);
    }

    @Transactional
    public void deleteStation(Long id) {
        try {
            stationRepository.deleteById(id);
            stationRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Station cannot be deleted because it is referenced by existing bookings or other records.", ex);
        }
    }

    // --- Game operations ---
    @Transactional(readOnly = true)
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Game> getActiveGames() {
        return gameRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Game> getGameById(Long id) {
        return gameRepository.findById(id);
    }

    @Transactional
    public Game saveGame(Game game) {
        return gameRepository.save(game);
    }

    @Transactional
    public void toggleGameActive(Long id) {
        gameRepository.findById(id).ifPresent(game -> {
            game.setIsActive(!Boolean.TRUE.equals(game.getIsActive()));
            gameRepository.save(game);
        });
    }

    // --- Station Games installation operations ---
    @Transactional
    public void installGameOnStation(Long stationId, Long gameId) {
        if (!stationGameRepository.existsByStationIdAndGameId(stationId, gameId)) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
            StationGame sg = new StationGame(station, game);
            stationGameRepository.save(sg);
        }
    }

    @Transactional
    public void removeGameFromStation(Long stationId, Long gameId) {
        stationGameRepository.deleteByStationIdAndGameId(stationId, gameId);
    }
}
