package com.arenahub.tournament;

import com.arenahub.audit.AuditLog;
import com.arenahub.audit.AuditLogRepository;
import com.arenahub.audit.AuditService;
import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Game;
import com.arenahub.catalog.GameRepository;
import com.arenahub.elo.Match;
import com.arenahub.elo.MatchRepository;
import com.arenahub.elo.MatchResult;
import com.arenahub.elo.MatchResultRepository;
import com.arenahub.elo.PlayerRating;
import com.arenahub.elo.PlayerRatingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final PlayerRatingRepository playerRatingRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final AuditLogRepository auditLogRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             TournamentPlayerRepository tournamentPlayerRepository,
                             UserRepository userRepository,
                             GameRepository gameRepository,
                             AuditService auditService,
                             ObjectMapper objectMapper,
                             PlayerRatingRepository playerRatingRepository,
                             MatchRepository matchRepository,
                             MatchResultRepository matchResultRepository,
                             AuditLogRepository auditLogRepository) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentPlayerRepository = tournamentPlayerRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.playerRatingRepository = playerRatingRepository;
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAllWithPlayers();
    }

    @Transactional(readOnly = true)
    public Optional<Tournament> getTournamentById(Long id) {
        return tournamentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean isUserRegistered(Long tournamentId, Long userId) {
        if (tournamentId == null || userId == null) {
            return false;
        }
        return tournamentPlayerRepository.existsByTournamentIdAndUserId(tournamentId, userId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getJoinedTournamentIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return tournamentPlayerRepository.findByUserId(userId).stream()
                .map(tp -> tp.getTournament().getId())
                .collect(Collectors.toSet());
    }

    public Tournament createTournament(Tournament tournament, Long adminUserId) {
        if (tournament.getName() == null || tournament.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tournament name is required.");
        }
        if (tournament.getGame() == null || tournament.getGame().getId() == null) {
            throw new IllegalArgumentException("Game must be selected.");
        }
        com.arenahub.catalog.Game game = gameRepository.findById(tournament.getGame().getId())
                .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + tournament.getGame().getId()));
        tournament.setGame(game);

        if (tournament.getMaxParticipants() == null || tournament.getMaxParticipants() <= 1) {
            throw new IllegalArgumentException("Max participants must be greater than 1.");
        }
        if (tournament.getStartTime() == null) {
            throw new IllegalArgumentException("Start time is required.");
        }

        Tournament saved = tournamentRepository.save(tournament);

        try {
            Map<String, Object> logPayload = new LinkedHashMap<>();
            logPayload.put("id", saved.getId());
            logPayload.put("name", saved.getName());
            logPayload.put("gameId", saved.getGame().getId());
            logPayload.put("gameTitle", saved.getGame().getTitle());
            logPayload.put("format", saved.getFormat());
            logPayload.put("status", saved.getStatus());
            logPayload.put("maxParticipants", saved.getMaxParticipants());
            logPayload.put("entryFee", saved.getEntryFee());
            logPayload.put("prizePool", saved.getPrizePool());
            logPayload.put("startTime", saved.getStartTime().toString());

            String newValueJson = objectMapper.writeValueAsString(logPayload);
            auditService.log(adminUserId, "CREATE_TOURNAMENT", "tournament", saved.getId(), null, newValueJson);
        } catch (Exception ex) {
            // Jackson serialization issue or audit failure shouldn't silently corrupt, but log
            throw new RuntimeException("Failed to generate audit log for tournament creation: " + ex.getMessage(), ex);
        }

        return saved;
    }

    public TournamentPlayer joinTournament(Long tournamentId, Long userId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + tournamentId));

        if (!"REGISTRATION_OPEN".equals(tournament.getStatus())) {
            throw new IllegalStateException("Registration is closed for this tournament.");
        }

        if (tournamentPlayerRepository.existsByTournamentIdAndUserId(tournamentId, userId)) {
            throw new TournamentAlreadyJoinedException("You have already registered for this tournament.");
        }

        long currentCount = tournamentPlayerRepository.countByTournamentId(tournamentId);
        if (currentCount >= tournament.getMaxParticipants()) {
            throw new TournamentFullException("This tournament is full (" + tournament.getMaxParticipants() + " max participants).");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        TournamentPlayer player = new TournamentPlayer(tournament, user);
        try {
            return tournamentPlayerRepository.saveAndFlush(player);
        } catch (DataIntegrityViolationException ex) {
            throw new TournamentAlreadyJoinedException("You have already registered for this tournament.");
        }
    }

    @Transactional
    public void generateBracket(Long tournamentId, String mode, Long adminId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + tournamentId));

        if (!"REGISTRATION_OPEN".equals(tournament.getStatus())) {
            throw new IllegalStateException("Bracket can only be generated when tournament status is REGISTRATION_OPEN.");
        }

        List<TournamentPlayer> players = tournamentPlayerRepository.findByTournamentId(tournamentId);
        int n = players.size();
        if (n < 2) {
            throw new IllegalStateException("At least 2 players are required to generate bracket.");
        }

        String sortMode = (mode != null && !mode.trim().isEmpty()) ? mode.trim().toUpperCase() : "RANDOM";

        // 1. Order by chosen mode
        switch (sortMode) {
            case "ELO" -> {
                Long gameId = tournament.getGame().getId();
                players.sort((p1, p2) -> {
                    int r1 = playerRatingRepository.findByUserIdAndGameId(p1.getUser().getId(), gameId)
                            .map(PlayerRating::getCurrentRating).orElse(1200);
                    int r2 = playerRatingRepository.findByUserIdAndGameId(p2.getUser().getId(), gameId)
                            .map(PlayerRating::getCurrentRating).orElse(1200);
                    if (r1 != r2) {
                        return Integer.compare(r2, r1); // DESC
                    }
                    return p1.getId().compareTo(p2.getId());
                });
            }
            case "MANUAL" -> {
                players.sort((p1, p2) -> {
                    Integer s1 = p1.getSeed();
                    Integer s2 = p2.getSeed();
                    if (s1 != null && s2 != null) {
                        int cmp = s1.compareTo(s2);
                        if (cmp != 0) return cmp;
                    } else if (s1 != null) {
                        return -1;
                    } else if (s2 != null) {
                        return 1;
                    }
                    if (p1.getRegisteredAt() != null && p2.getRegisteredAt() != null) {
                        int cmp = p1.getRegisteredAt().compareTo(p2.getRegisteredAt());
                        if (cmp != 0) return cmp;
                    }
                    return p1.getId().compareTo(p2.getId());
                });
            }
            case "RANDOM" -> Collections.shuffle(players);
            default -> throw new IllegalArgumentException("Invalid bracket mode: " + mode + ". Allowed modes are RANDOM, ELO, MANUAL.");
        }

        // Assign seed numbers 1..N
        for (int i = 0; i < n; i++) {
            TournamentPlayer tp = players.get(i);
            tp.setSeed(i + 1);
            tournamentPlayerRepository.save(tp);
        }

        // 2. Pad to next power of two
        int size = 1;
        while (size < n) {
            size *= 2;
        }

        int totalRounds = 0;
        int temp = size;
        while (temp > 1) {
            totalRounds++;
            temp /= 2;
        }

        LocalDateTime now = LocalDateTime.now();

        // 3. Create every round's matches up front chained with next_match_id
        Map<Integer, Map<Integer, Match>> matchesByRound = new HashMap<>();

        for (int r = totalRounds; r >= 1; r--) {
            int matchesInRound = size / (1 << r);
            Map<Integer, Match> roundMatches = new HashMap<>();

            for (int pos = 1; pos <= matchesInRound; pos++) {
                Match match = new Match();
                match.setTournament(tournament);
                match.setGame(tournament.getGame());
                match.setMatchType("TOURNAMENT");
                match.setRoundNumber(r);
                match.setBracketPosition(pos);
                match.setScheduledAt(tournament.getStartTime().plusHours(r - 1));

                if (r < totalRounds) {
                    int parentPos = (pos + 1) / 2;
                    Match nextMatch = matchesByRound.get(r + 1).get(parentPos);
                    match.setNextMatch(nextMatch);
                } else {
                    match.setNextMatch(null);
                }

                if (r > 1) {
                    match.setStatus("SCHEDULED");
                    match.setCompletedAt(null);
                    match = matchRepository.save(match);
                }

                roundMatches.put(pos, match);
            }
            matchesByRound.put(r, roundMatches);
        }

        // 4. Assign Round 1 pairings and byes
        List<Integer> bracketSeeds = generateBracketSeeds(size);
        Map<Integer, User> userBySeed = new HashMap<>();
        for (TournamentPlayer tp : players) {
            userBySeed.put(tp.getSeed(), tp.getUser());
        }

        int round1Matches = size / 2;
        for (int pos = 1; pos <= round1Matches; pos++) {
            Match match = matchesByRound.get(1).get(pos);

            int seedA = bracketSeeds.get((pos - 1) * 2);
            int seedB = bracketSeeds.get((pos - 1) * 2 + 1);

            User userA = userBySeed.get(seedA);
            User userB = userBySeed.get(seedB);

            if (userB == null) {
                // BYE: single match_results row (outcome='WIN'), match status='COMPLETED', completed_at=now
                match.setStatus("COMPLETED");
                match.setCompletedAt(now);
                match = matchRepository.save(match);

                MatchResult mrA = new MatchResult(match, userA, "WIN", null, 0);
                matchResultRepository.save(mrA);

                // Advance bye winner automatically like a played match
                if (match.getNextMatch() != null) {
                    MatchResult nextMr = new MatchResult(match.getNextMatch(), userA, null, null, 0);
                    matchResultRepository.save(nextMr);
                }
            } else {
                // REAL PAIRING: two match_results rows (outcome=null, score=null), match status='SCHEDULED'
                match.setStatus("SCHEDULED");
                match.setCompletedAt(null);
                match = matchRepository.save(match);

                MatchResult mrA = new MatchResult(match, userA, null, null, 0);
                MatchResult mrB = new MatchResult(match, userB, null, null, 0);
                matchResultRepository.save(mrA);
                matchResultRepository.save(mrB);
            }
        }

        // 5. Update tournament status
        tournament.setStatus("IN_PROGRESS");
        tournamentRepository.save(tournament);

        // 6. Log via AuditService
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("tournamentId", tournament.getId());
            summary.put("tournamentName", tournament.getName());
            summary.put("mode", sortMode);
            summary.put("registeredPlayers", n);
            summary.put("bracketSize", size);
            summary.put("totalRounds", totalRounds);
            summary.put("byes", size - n);

            String bracketSummaryJson = objectMapper.writeValueAsString(summary);
            auditService.log(adminId, "GENERATE_BRACKET", "tournament", tournament.getId(), null, bracketSummaryJson);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize audit log for bracket generation: " + ex.getMessage(), ex);
        }
    }

    private List<Integer> generateBracketSeeds(int size) {
        List<Integer> seeds = new ArrayList<>();
        seeds.add(1);
        seeds.add(2);
        while (seeds.size() < size) {
            List<Integer> next = new ArrayList<>();
            int currentSize = seeds.size() * 2;
            for (int s : seeds) {
                next.add(s);
                next.add(currentSize + 1 - s);
            }
            seeds = next;
        }
        return seeds;
    }

    @Transactional(readOnly = true)
    public MatchReportDetails getMatchForReporting(Long matchId, Long currentUserId, boolean isAdmin) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        if (!"TOURNAMENT".equals(match.getMatchType())) {
            throw new IllegalArgumentException("Match is not a TOURNAMENT match.");
        }

        List<MatchResult> results = matchResultRepository.findByMatchId(matchId);
        if (results.size() < 2) {
            throw new IllegalStateException("This match does not have 2 players assigned yet.");
        }

        MatchResult r1 = results.get(0);
        MatchResult r2 = results.get(1);

        boolean isPlayer = currentUserId != null &&
                (currentUserId.equals(r1.getUser().getId()) || currentUserId.equals(r2.getUser().getId()));

        if (!isAdmin && !isPlayer) {
            throw new AccessDeniedException("You are not authorized to view or report this match.");
        }

        String disputeReason = null;
        if ("DISPUTED".equals(match.getStatus())) {
            disputeReason = auditLogRepository
                    .findFirstByEntityNameAndEntityIdAndActionOrderByCreatedAtDesc("match", matchId, "DISPUTE_RAISED")
                    .map(AuditLog::getNewValue)
                    .map(json -> {
                        try {
                            return (String) objectMapper.readValue(json, Map.class).get("reason");
                        } catch (Exception e) {
                            return json;
                        }
                    }).orElse("No reason specified");
        }

        return new MatchReportDetails(
                match.getId(),
                match.getGame() != null ? match.getGame().getTitle() : "Unknown Game",
                match.getTournament() != null ? match.getTournament().getName() : "Tournament Match",
                match.getRoundNumber(),
                match.getBracketPosition(),
                match.getStatus(),
                r1.getUser().getId(),
                r1.getUser().getFullName(),
                r1.getUser().getEmail(),
                r1.getScore(),
                r1.getOutcome(),
                r2.getUser().getId(),
                r2.getUser().getFullName(),
                r2.getUser().getEmail(),
                r2.getScore(),
                r2.getOutcome(),
                isPlayer,
                isAdmin,
                disputeReason
        );
    }

    @Transactional
    public void reportMatchResult(Long matchId, Long player1Id, Integer player1Score,
                                  Long player2Id, Integer player2Score,
                                  Long reportingUserId, boolean isAdmin) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        if (!"TOURNAMENT".equals(match.getMatchType())) {
            throw new IllegalArgumentException("Match is not a TOURNAMENT match.");
        }

        if (player1Score == null || player2Score == null) {
            throw new IllegalArgumentException("Both scores are required.");
        }
        if (player1Score < 0 || player2Score < 0) {
            throw new IllegalArgumentException("Scores cannot be negative.");
        }
        if (player1Score.equals(player2Score)) {
            throw new IllegalArgumentException("Tournament elimination matches cannot end in a tie. Please record the final score.");
        }

        boolean wasDisputed = "DISPUTED".equals(match.getStatus());
        if (!isAdmin && !"SCHEDULED".equals(match.getStatus())) {
            throw new IllegalStateException("Match cannot be reported because its status is " + match.getStatus());
        }
        if (isAdmin && !"SCHEDULED".equals(match.getStatus()) && !wasDisputed) {
            throw new IllegalStateException("Match cannot be reported because its status is " + match.getStatus());
        }

        List<MatchResult> results = matchResultRepository.findByMatchId(matchId);
        if (results.size() < 2) {
            throw new IllegalStateException("Match does not have 2 players assigned.");
        }

        MatchResult r1 = null;
        MatchResult r2 = null;
        for (MatchResult r : results) {
            if (player1Id != null && r.getUser().getId().equals(player1Id)) {
                r1 = r;
            } else if (player2Id != null && r.getUser().getId().equals(player2Id)) {
                r2 = r;
            }
        }
        if (r1 == null || r2 == null) {
            r1 = results.get(0);
            r2 = results.get(1);
        }

        boolean isPlayer = reportingUserId != null &&
                (reportingUserId.equals(r1.getUser().getId()) || reportingUserId.equals(r2.getUser().getId()));
        if (!isAdmin && !isPlayer) {
            throw new AccessDeniedException("You are not authorized to report scores for this match.");
        }

        Map<String, Object> oldValues = new LinkedHashMap<>();
        if (wasDisputed) {
            oldValues.put("matchId", matchId);
            oldValues.put("player1", Map.of("userId", r1.getUser().getId(), "name", r1.getUser().getFullName(), "score", String.valueOf(r1.getScore()), "outcome", String.valueOf(r1.getOutcome())));
            oldValues.put("player2", Map.of("userId", r2.getUser().getId(), "name", r2.getUser().getFullName(), "score", String.valueOf(r2.getScore()), "outcome", String.valueOf(r2.getOutcome())));
        }

        User user1 = r1.getUser();
        User user2 = r2.getUser();
        String outcome1 = player1Score > player2Score ? "WIN" : "LOSS";
        String outcome2 = player1Score > player2Score ? "LOSS" : "WIN";

        User winner = "WIN".equals(outcome1) ? user1 : user2;
        User loser = "WIN".equals(outcome1) ? user2 : user1;

        // Apply ELO update
        Game game = match.getGame();
        PlayerRating playerRating1 = playerRatingRepository.findByUserIdAndGameId(user1.getId(), game.getId())
                .orElseGet(() -> new PlayerRating(user1, game, 1200, 0));
        PlayerRating playerRating2 = playerRatingRepository.findByUserIdAndGameId(user2.getId(), game.getId())
                .orElseGet(() -> new PlayerRating(user2, game, 1200, 0));

        int rating1 = playerRating1.getCurrentRating();
        int rating2 = playerRating2.getCurrentRating();

        int matchesPlayed1 = playerRating1.getMatchesPlayed() + 1;
        int matchesPlayed2 = playerRating2.getMatchesPlayed() + 1;

        int k1 = matchesPlayed1 < 30 ? 32 : 16;
        int k2 = matchesPlayed2 < 30 ? 32 : 16;

        double expected1 = 1.0 / (1.0 + Math.pow(10.0, (double) (rating2 - rating1) / 400.0));
        double expected2 = 1.0 / (1.0 + Math.pow(10.0, (double) (rating1 - rating2) / 400.0));

        double actual1 = "WIN".equals(outcome1) ? 1.0 : 0.0;
        double actual2 = "WIN".equals(outcome2) ? 1.0 : 0.0;

        int newRating1 = Math.max(0, (int) Math.round(rating1 + k1 * (actual1 - expected1)));
        int newRating2 = Math.max(0, (int) Math.round(rating2 + k2 * (actual2 - expected2)));

        int delta1 = newRating1 - rating1;
        int delta2 = newRating2 - rating2;

        playerRating1.setCurrentRating(newRating1);
        playerRating1.setMatchesPlayed(matchesPlayed1);
        playerRatingRepository.save(playerRating1);

        playerRating2.setCurrentRating(newRating2);
        playerRating2.setMatchesPlayed(matchesPlayed2);
        playerRatingRepository.save(playerRating2);

        r1.setScore(player1Score);
        r1.setOutcome(outcome1);
        r1.setRatingChange(delta1);
        matchResultRepository.save(r1);

        r2.setScore(player2Score);
        r2.setOutcome(outcome2);
        r2.setRatingChange(delta2);
        matchResultRepository.save(r2);

        LocalDateTime now = LocalDateTime.now();
        match.setStatus("COMPLETED");
        match.setCompletedAt(now);
        matchRepository.save(match);

        // Advance winner via next_match_id
        if (match.getNextMatch() != null) {
            Match nextMatch = match.getNextMatch();
            List<MatchResult> nextResults = matchResultRepository.findByMatchId(nextMatch.getId());

            nextResults.stream()
                    .filter(nr -> nr.getUser().getId().equals(loser.getId()))
                    .findFirst()
                    .ifPresent(matchResultRepository::delete);

            boolean alreadyIn = nextResults.stream().anyMatch(nr -> nr.getUser().getId().equals(winner.getId()));
            if (!alreadyIn) {
                MatchResult nextMr = new MatchResult(nextMatch, winner, null, null, 0);
                matchResultRepository.save(nextMr);
            }
        } else {
            // Final match: set tournament.status = 'COMPLETED'
            if (match.getTournament() != null) {
                Tournament tournament = match.getTournament();
                tournament.setStatus("COMPLETED");
                tournament.setEndTime(now);
                tournamentRepository.save(tournament);
            }
        }

        // Audit log if DISPUTE_RESOLVED
        if (wasDisputed) {
            try {
                Map<String, Object> newValues = new LinkedHashMap<>();
                newValues.put("matchId", matchId);
                newValues.put("resolvedByUserId", reportingUserId);
                newValues.put("player1", Map.of("userId", user1.getId(), "name", user1.getFullName(), "score", player1Score, "outcome", outcome1, "ratingChange", delta1));
                newValues.put("player2", Map.of("userId", user2.getId(), "name", user2.getFullName(), "score", player2Score, "outcome", outcome2, "ratingChange", delta2));

                auditService.log(reportingUserId, "DISPUTE_RESOLVED", "match", matchId,
                        objectMapper.writeValueAsString(oldValues),
                        objectMapper.writeValueAsString(newValues));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to serialize audit log for dispute resolution: " + ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void raiseDispute(Long matchId, String reason, Long reportingUserId, boolean isAdmin) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        if (!"TOURNAMENT".equals(match.getMatchType())) {
            throw new IllegalArgumentException("Only TOURNAMENT matches can be disputed.");
        }

        if (!"COMPLETED".equals(match.getStatus())) {
            throw new IllegalStateException("Only COMPLETED matches can be disputed.");
        }

        List<MatchResult> results = matchResultRepository.findByMatchId(matchId);
        boolean isPlayer = reportingUserId != null &&
                results.stream().anyMatch(r -> reportingUserId.equals(r.getUser().getId()));

        if (!isAdmin && !isPlayer) {
            throw new AccessDeniedException("You are not authorized to dispute this match.");
        }

        match.setStatus("DISPUTED");
        matchRepository.save(match);

        try {
            Map<String, Object> logPayload = new LinkedHashMap<>();
            logPayload.put("reason", (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Score disputed by player");
            logPayload.put("disputedByUserId", reportingUserId);
            logPayload.put("disputedAt", LocalDateTime.now().toString());

            String reasonJson = objectMapper.writeValueAsString(logPayload);
            auditService.log(reportingUserId, "DISPUTE_RAISED", "match", matchId, null, reasonJson);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize audit log for dispute raise: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<DisputedMatchDTO> getDisputedMatches() {
        List<Match> disputedMatches = matchRepository.findByStatus("DISPUTED");
        List<DisputedMatchDTO> dtoList = new ArrayList<>();

        for (Match match : disputedMatches) {
            List<MatchResult> results = matchResultRepository.findByMatchId(match.getId());
            User p1 = results.size() > 0 ? results.get(0).getUser() : null;
            Integer s1 = results.size() > 0 ? results.get(0).getScore() : null;
            String o1 = results.size() > 0 ? results.get(0).getOutcome() : null;

            User p2 = results.size() > 1 ? results.get(1).getUser() : null;
            Integer s2 = results.size() > 1 ? results.get(1).getScore() : null;
            String o2 = results.size() > 1 ? results.get(1).getOutcome() : null;

            String reason = "No reason provided";
            LocalDateTime disputedAt = match.getCompletedAt();

            Optional<AuditLog> disputeLog = auditLogRepository
                    .findFirstByEntityNameAndEntityIdAndActionOrderByCreatedAtDesc("match", match.getId(), "DISPUTE_RAISED");
            if (disputeLog.isPresent()) {
                disputedAt = disputeLog.get().getCreatedAt();
                try {
                    Map map = objectMapper.readValue(disputeLog.get().getNewValue(), Map.class);
                    if (map != null && map.get("reason") != null) {
                        reason = map.get("reason").toString();
                    }
                } catch (Exception ignored) {
                    reason = disputeLog.get().getNewValue();
                }
            }

            dtoList.add(new DisputedMatchDTO(
                    match.getId(),
                    match.getGame() != null ? match.getGame().getTitle() : "Unknown Game",
                    match.getTournament() != null ? match.getTournament().getName() : "Unknown Tournament",
                    match.getRoundNumber(),
                    match.getBracketPosition(),
                    p1 != null ? p1.getId() : null,
                    p1 != null ? p1.getFullName() : "Player 1",
                    p1 != null ? p1.getEmail() : "",
                    s1,
                    o1,
                    p2 != null ? p2.getId() : null,
                    p2 != null ? p2.getFullName() : "Player 2",
                    p2 != null ? p2.getEmail() : "",
                    s2,
                    o2,
                    reason,
                    disputedAt
            ));
        }

        return dtoList;
    }

    @Transactional(readOnly = true)
    public TournamentBracketDTO getTournamentBracket(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + tournamentId));

        List<Match> matches = matchRepository.findByTournamentId(tournamentId);
        matches.sort(Comparator.comparing(Match::getRoundNumber).thenComparing(Match::getBracketPosition));

        Map<Integer, List<Match>> byRound = matches.stream()
                .collect(Collectors.groupingBy(Match::getRoundNumber, TreeMap::new, Collectors.toList()));

        int totalRounds = byRound.isEmpty() ? 0 : byRound.keySet().stream().max(Integer::compareTo).orElse(0);

        List<BracketRoundDTO> roundDTOs = new ArrayList<>();
        for (Map.Entry<Integer, List<Match>> entry : byRound.entrySet()) {
            int roundNum = entry.getKey();
            List<Match> roundMatches = entry.getValue();

            String roundName;
            if (roundNum == totalRounds) {
                roundName = "Final";
            } else if (roundNum == totalRounds - 1) {
                roundName = "Semifinals";
            } else if (roundNum == totalRounds - 2) {
                roundName = "Quarterfinals";
            } else {
                roundName = "Round " + roundNum;
            }

            List<BracketMatchDTO> matchDTOs = new ArrayList<>();
            for (Match m : roundMatches) {
                List<MatchResult> results = matchResultRepository.findByMatchId(m.getId());

                BracketParticipantDTO p1;
                BracketParticipantDTO p2;

                if (results.size() >= 1) {
                    MatchResult r1 = results.get(0);
                    p1 = new BracketParticipantDTO(
                            r1.getUser().getId(),
                            r1.getUser().getFullName(),
                            r1.getScore(),
                            r1.getOutcome(),
                            "WIN".equalsIgnoreCase(r1.getOutcome())
                    );
                } else {
                    p1 = new BracketParticipantDTO(null, "TBD", null, null, false);
                }

                if (results.size() >= 2) {
                    MatchResult r2 = results.get(1);
                    p2 = new BracketParticipantDTO(
                            r2.getUser().getId(),
                            r2.getUser().getFullName(),
                            r2.getScore(),
                            r2.getOutcome(),
                            "WIN".equalsIgnoreCase(r2.getOutcome())
                    );
                } else if (results.size() == 1) {
                    MatchResult r1 = results.get(0);
                    if (roundNum == 1 && "COMPLETED".equals(m.getStatus()) && "WIN".equalsIgnoreCase(r1.getOutcome())) {
                        p2 = new BracketParticipantDTO(null, "BYE", null, null, false);
                    } else {
                        p2 = new BracketParticipantDTO(null, "TBD", null, null, false);
                    }
                } else {
                    p2 = new BracketParticipantDTO(null, "TBD", null, null, false);
                }

                Long nextMatchId = m.getNextMatch() != null ? m.getNextMatch().getId() : null;

                matchDTOs.add(new BracketMatchDTO(
                        m.getId(),
                        m.getRoundNumber(),
                        m.getBracketPosition(),
                        m.getStatus(),
                        nextMatchId,
                        p1,
                        p2
                ));
            }

            roundDTOs.add(new BracketRoundDTO(roundNum, roundName, matchDTOs));
        }

        return new TournamentBracketDTO(
                tournament.getId(),
                tournament.getName(),
                tournament.getGame() != null ? tournament.getGame().getTitle() : "Unknown Game",
                tournament.getFormat(),
                tournament.getStatus(),
                roundDTOs
        );
    }

    public record MatchReportDetails(
            Long matchId,
            String gameTitle,
            String tournamentName,
            Integer roundNumber,
            Integer bracketPosition,
            String status,
            Long player1Id,
            String player1Name,
            String player1Email,
            Integer player1Score,
            String player1Outcome,
            Long player2Id,
            String player2Name,
            String player2Email,
            Integer player2Score,
            String player2Outcome,
            boolean isPlayer,
            boolean isAdmin,
            String disputeReason
    ) {}

    public record DisputedMatchDTO(
            Long matchId,
            String gameTitle,
            String tournamentName,
            Integer roundNumber,
            Integer bracketPosition,
            Long player1Id,
            String player1Name,
            String player1Email,
            Integer player1Score,
            String player1Outcome,
            Long player2Id,
            String player2Name,
            String player2Email,
            Integer player2Score,
            String player2Outcome,
            String disputeReason,
            LocalDateTime disputedAt
    ) {}

    public record BracketParticipantDTO(
            Long userId,
            String name,
            Integer score,
            String outcome,
            boolean isWinner
    ) {}

    public record BracketMatchDTO(
            Long matchId,
            Integer roundNumber,
            Integer bracketPosition,
            String status,
            Long nextMatchId,
            BracketParticipantDTO player1,
            BracketParticipantDTO player2
    ) {}

    public record BracketRoundDTO(
            Integer roundNumber,
            String roundName,
            List<BracketMatchDTO> matches
    ) {}

    public record TournamentBracketDTO(
            Long tournamentId,
            String tournamentName,
            String gameTitle,
            String format,
            String status,
            List<BracketRoundDTO> rounds
    ) {}
}
