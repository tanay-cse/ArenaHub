package com.arenahub.elo;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Game;
import com.arenahub.catalog.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EloService {

    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public EloService(MatchRepository matchRepository,
                      MatchResultRepository matchResultRepository,
                      PlayerRatingRepository playerRatingRepository,
                      UserRepository userRepository,
                      GameRepository gameRepository) {
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
        this.playerRatingRepository = playerRatingRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional
    public Match recordMatch(Long userId, Long opponentId, Long gameId, String matchType, int yourScore, int opponentScore) {
        if (userId == null || opponentId == null) {
            throw new IllegalArgumentException("User and opponent IDs must be specified.");
        }
        if (userId.equals(opponentId)) {
            throw new IllegalArgumentException("Opponent cannot be yourself.");
        }
        if (gameId == null) {
            throw new IllegalArgumentException("Game must be selected.");
        }
        if (matchType == null || (!matchType.equals("CASUAL") && !matchType.equals("RANKED"))) {
            throw new IllegalArgumentException("Match type must be CASUAL or RANKED.");
        }
        if (yourScore < 0 || opponentScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        User opponent = userRepository.findById(opponentId)
                .orElseThrow(() -> new IllegalArgumentException("Opponent not found with ID: " + opponentId));

        if (!Boolean.TRUE.equals(opponent.getIsActive())) {
            throw new IllegalArgumentException("Opponent account is not active.");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + gameId));

        String userOutcome;
        String opponentOutcome;
        if (yourScore > opponentScore) {
            userOutcome = "WIN";
            opponentOutcome = "LOSS";
        } else if (yourScore < opponentScore) {
            userOutcome = "LOSS";
            opponentOutcome = "WIN";
        } else {
            userOutcome = "DRAW";
            opponentOutcome = "DRAW";
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Insert match as COMPLETED (scheduled_at and completed_at = now)
        Match match = new Match();
        match.setGame(game);
        match.setMatchType(matchType);
        match.setStatus("COMPLETED");
        match.setScheduledAt(now);
        match.setCompletedAt(now);
        Match savedMatch = matchRepository.save(match);

        // 2. Compute ELO ratings for both players
        PlayerRating playerRatingA = playerRatingRepository.findByUserIdAndGameId(user.getId(), game.getId())
                .orElseGet(() -> new PlayerRating(user, game, 1200, 0));
        PlayerRating playerRatingB = playerRatingRepository.findByUserIdAndGameId(opponent.getId(), game.getId())
                .orElseGet(() -> new PlayerRating(opponent, game, 1200, 0));

        int ratingA = playerRatingA.getCurrentRating();
        int ratingB = playerRatingB.getCurrentRating();

        int matchesPlayedA = playerRatingA.getMatchesPlayed() + 1;
        int matchesPlayedB = playerRatingB.getMatchesPlayed() + 1;

        int kA = matchesPlayedA < 30 ? 32 : 16;
        int kB = matchesPlayedB < 30 ? 32 : 16;

        double expectedA = 1.0 / (1.0 + Math.pow(10.0, (double) (ratingB - ratingA) / 400.0));
        double expectedB = 1.0 / (1.0 + Math.pow(10.0, (double) (ratingA - ratingB) / 400.0));

        double actualA;
        if ("WIN".equals(userOutcome)) {
            actualA = 1.0;
        } else if ("DRAW".equals(userOutcome)) {
            actualA = 0.5;
        } else {
            actualA = 0.0;
        }

        double actualB;
        if ("WIN".equals(opponentOutcome)) {
            actualB = 1.0;
        } else if ("DRAW".equals(opponentOutcome)) {
            actualB = 0.5;
        } else {
            actualB = 0.0;
        }

        int newRatingA = (int) Math.round(ratingA + kA * (actualA - expectedA));
        int newRatingB = (int) Math.round(ratingB + kB * (actualB - expectedB));

        // Clamp at 0 minimum
        newRatingA = Math.max(0, newRatingA);
        newRatingB = Math.max(0, newRatingB);

        int deltaA = newRatingA - ratingA;
        int deltaB = newRatingB - ratingB;

        // 3. Insert two match_results rows with rating_change
        MatchResult userResult = new MatchResult(savedMatch, user, userOutcome, yourScore, deltaA);
        MatchResult opponentResult = new MatchResult(savedMatch, opponent, opponentOutcome, opponentScore, deltaB);
        matchResultRepository.save(userResult);
        matchResultRepository.save(opponentResult);

        // 4. Update player ratings
        playerRatingA.setCurrentRating(newRatingA);
        playerRatingA.setMatchesPlayed(matchesPlayedA);
        playerRatingRepository.save(playerRatingA);

        playerRatingB.setCurrentRating(newRatingB);
        playerRatingB.setMatchesPlayed(matchesPlayedB);
        playerRatingRepository.save(playerRatingB);

        return savedMatch;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard(Long gameId) {
        List<Object[]> rows = playerRatingRepository.findLeaderboardForGame(gameId);
        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long pUserId = ((Number) row[0]).longValue();
            String playerName = (String) row[1];
            String playerEmail = (String) row[2];
            int currentRating = ((Number) row[3]).intValue();
            long matchesPlayed = ((Number) row[4]).longValue();
            long wins = ((Number) row[5]).longValue();
            long losses = ((Number) row[6]).longValue();
            long draws = ((Number) row[7]).longValue();
            double winRate = matchesPlayed > 0 ? ((double) wins * 100.0 / matchesPlayed) : 0.0;

            entries.add(new LeaderboardEntry(
                    rank++,
                    pUserId,
                    playerName,
                    playerEmail,
                    currentRating,
                    matchesPlayed,
                    wins,
                    losses,
                    draws,
                    Math.round(winRate * 10.0) / 10.0
            ));
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public List<MatchHistoryItem> getUserMatchHistory(Long userId) {
        List<MatchResult> userResults = matchResultRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<MatchHistoryItem> items = new ArrayList<>();

        for (MatchResult mr : userResults) {
            Match match = mr.getMatch();
            String gameTitle = match.getGame() != null ? match.getGame().getTitle() : "Unknown Game";
            String matchType = match.getMatchType();
            LocalDateTime playedAt = match.getCompletedAt() != null ? match.getCompletedAt() : mr.getCreatedAt();

            MatchResult oppResult = matchResultRepository.findFirstByMatchIdAndUserIdNot(match.getId(), userId).orElse(null);
            String oppName = oppResult != null && oppResult.getUser() != null ? oppResult.getUser().getFullName() : "Unknown";
            String oppEmail = oppResult != null && oppResult.getUser() != null ? oppResult.getUser().getEmail() : "";
            Integer oppScore = oppResult != null ? oppResult.getScore() : 0;

            items.add(new MatchHistoryItem(
                    match.getId(),
                    gameTitle,
                    matchType,
                    oppName,
                    oppEmail,
                    mr.getOutcome(),
                    mr.getScore(),
                    oppScore,
                    mr.getRatingChange(),
                    playedAt
            ));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public StreakInfo computeStreak(Long userId) {
        List<MatchResult> recentResults = matchResultRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (recentResults.isEmpty()) {
            return new StreakInfo("NONE", 0, "No matches played");
        }

        String firstOutcome = recentResults.get(0).getOutcome();
        if ("DRAW".equalsIgnoreCase(firstOutcome)) {
            return new StreakInfo("DRAW", 1, "Draw in last match");
        }
        if (!"WIN".equalsIgnoreCase(firstOutcome) && !"LOSS".equalsIgnoreCase(firstOutcome)) {
            return new StreakInfo("NONE", 0, "No active streak");
        }

        int count = 0;
        for (MatchResult mr : recentResults) {
            if (firstOutcome.equalsIgnoreCase(mr.getOutcome())) {
                count++;
            } else {
                break;
            }
        }

        if ("WIN".equalsIgnoreCase(firstOutcome)) {
            return new StreakInfo("WIN", count, count + (count == 1 ? " Win" : " Wins"));
        } else {
            return new StreakInfo("LOSS", count, count + (count == 1 ? " Loss" : " Losses"));
        }
    }

    public record LeaderboardEntry(
            int rank,
            Long userId,
            String playerName,
            String playerEmail,
            int currentRating,
            long matchesPlayed,
            long wins,
            long losses,
            long draws,
            double winRate
    ) {}

    public record MatchHistoryItem(
            Long matchId,
            String gameTitle,
            String matchType,
            String opponentName,
            String opponentEmail,
            String outcome,
            Integer yourScore,
            Integer opponentScore,
            int ratingChange,
            LocalDateTime playedAt
    ) {}

    public record StreakInfo(
            String type,
            int count,
            String display
    ) {}
}
