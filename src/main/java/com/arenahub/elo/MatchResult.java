package com.arenahub.elo;

import com.arenahub.auth.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_results", uniqueConstraints = {
    @UniqueConstraint(name = "uk_match_results_match_user", columnNames = {"match_id", "user_id"})
})
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "score")
    private Integer score;

    @Column(name = "rating_change", nullable = false)
    private Integer ratingChange = 0;

    @Column(name = "rank_placement")
    private Integer rankPlacement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MatchResult() {
    }

    public MatchResult(Match match, User user, String outcome, Integer score, Integer ratingChange) {
        this.match = match;
        this.user = user;
        this.outcome = outcome;
        this.score = score;
        this.ratingChange = ratingChange != null ? ratingChange : 0;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.ratingChange == null) {
            this.ratingChange = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getRatingChange() {
        return ratingChange;
    }

    public void setRatingChange(Integer ratingChange) {
        this.ratingChange = ratingChange;
    }

    public Integer getRankPlacement() {
        return rankPlacement;
    }

    public void setRankPlacement(Integer rankPlacement) {
        this.rankPlacement = rankPlacement;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
