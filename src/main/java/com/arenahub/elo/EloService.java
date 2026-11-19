package com.arenahub.elo;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Game;
import com.arenahub.catalog.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        // 2. Insert two match_results rows (outcome WIN/LOSS/DRAW from the scores)
        MatchResult userResult = new MatchResult(savedMatch, user, userOutcome, yourScore, 0);
        MatchResult opponentResult = new MatchResult(savedMatch, opponent, opponentOutcome, opponentScore, 0);
        matchResultRepository.save(userResult);
        matchResultRepository.save(opponentResult);

        // 3. Create missing player_ratings rows (rating 1200) and increment matches_played for both players
        updatePlayerRating(user, game);
        updatePlayerRating(opponent, game);

        return savedMatch;
    }

    private void updatePlayerRating(User player, Game game) {
        PlayerRating rating = playerRatingRepository.findByUserIdAndGameId(player.getId(), game.getId())
                .orElseGet(() -> new PlayerRating(player, game, 1200, 0));

        rating.setMatchesPlayed(rating.getMatchesPlayed() + 1);
        playerRatingRepository.save(rating);
    }
}
