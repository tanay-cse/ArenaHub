package com.arenahub.elo;

import com.arenahub.catalog.Game;
import com.arenahub.tournament.Tournament;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "bracket_position")
    private Integer bracketPosition;

    @Column(name = "match_type", nullable = false, length = 30)
    private String matchType;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SCHEDULED";

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchResult> results = new ArrayList<>();

    public Match() {
    }

    public Match(Game game, String matchType, String status, LocalDateTime scheduledAt, LocalDateTime completedAt) {
        this.game = game;
        this.matchType = matchType;
        this.status = status != null ? status : "SCHEDULED";
        this.scheduledAt = scheduledAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Match getNextMatch() {
        return nextMatch;
    }

    public void setNextMatch(Match nextMatch) {
        this.nextMatch = nextMatch;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Integer getBracketPosition() {
        return bracketPosition;
    }

    public void setBracketPosition(Integer bracketPosition) {
        this.bracketPosition = bracketPosition;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<MatchResult> getResults() {
        return results;
    }

    public void setResults(List<MatchResult> results) {
        this.results = results;
    }
}
