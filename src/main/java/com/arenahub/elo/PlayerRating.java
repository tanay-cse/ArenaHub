package com.arenahub.elo;

import com.arenahub.auth.User;
import com.arenahub.catalog.Game;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_ratings", uniqueConstraints = {
    @UniqueConstraint(name = "uk_player_ratings_user_game", columnNames = {"user_id", "game_id"})
})
public class PlayerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "current_rating", nullable = false)
    private Integer currentRating = 1200;

    @Column(name = "matches_played", nullable = false)
    private Integer matchesPlayed = 0;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    public PlayerRating() {
    }

    public PlayerRating(User user, Game game, Integer currentRating, Integer matchesPlayed) {
        this.user = user;
        this.game = game;
        this.currentRating = currentRating != null ? currentRating : 1200;
        this.matchesPlayed = matchesPlayed != null ? matchesPlayed : 0;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
        if (this.currentRating == null) {
            this.currentRating = 1200;
        }
        if (this.matchesPlayed == null) {
            this.matchesPlayed = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Integer getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(Integer currentRating) {
        this.currentRating = currentRating;
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
