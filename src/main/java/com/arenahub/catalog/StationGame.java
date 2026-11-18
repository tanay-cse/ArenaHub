package com.arenahub.catalog;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "station_games", uniqueConstraints = {
    @UniqueConstraint(name = "uk_station_game", columnNames = {"station_id", "game_id"})
})
public class StationGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "installed_at", nullable = false, updatable = false)
    private LocalDateTime installedAt = LocalDateTime.now();

    public StationGame() {
    }

    public StationGame(Station station, Game game) {
        this.station = station;
        this.game = game;
        this.installedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public LocalDateTime getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(LocalDateTime installedAt) {
        this.installedAt = installedAt;
    }

    @PrePersist
    public void onPrePersist() {
        if (installedAt == null) {
            installedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationGame that = (StationGame) o;
        return Objects.equals(station != null ? station.getId() : null, that.station != null ? that.station.getId() : null) &&
               Objects.equals(game != null ? game.getId() : null, that.game != null ? that.game.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station != null ? station.getId() : null, game != null ? game.getId() : null);
    }
}
