package com.arenahub.elo;

import com.arenahub.auth.Role;
import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.catalog.Game;
import com.arenahub.catalog.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private PlayerRatingRepository playerRatingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private EloService eloService;

    private User playerA;
    private User playerB;
    private Game game;

    @BeforeEach
    void setUp() {
        Role studentRole = new Role("STUDENT");

        playerA = new User();
        playerA.setId(1L);
        playerA.setEmail("playerA@arenahub.com");
        playerA.setFullName("Player Alpha");
        playerA.setRole(studentRole);
        playerA.setIsActive(true);

        playerB = new User();
        playerB.setId(2L);
        playerB.setEmail("playerB@arenahub.com");
        playerB.setFullName("Player Beta");
        playerB.setRole(studentRole);
        playerB.setIsActive(true);

        game = new Game();
        game.setId(10L);
        game.setTitle("Valorant");

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(playerA));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(playerB));
        lenient().when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        lenient().when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });
    }

    @Test
    @DisplayName("Case 1: Both equal rating 1200 with 0 matches played (K=32) -> Winner gains +16, Loser loses -16")
    void testStandardMatchEqualRatingsK32() {
        // Player A: 1200 rating, 0 matches played -> matchesPlayedA = 0 + 1 = 1 (< 30 => K=32)
        // Player B: 1200 rating, 0 matches played -> matchesPlayedB = 0 + 1 = 1 (< 30 => K=32)
        // Expected outcome:
        // expectedA = 1 / (1 + 10^0) = 0.5, expectedB = 0.5
        // actualA = 1.0 (win), actualB = 0.0 (loss)
        // deltaA = round(32 * (1.0 - 0.5)) = +16 -> newRatingA = 1216
        // deltaB = round(32 * (0.0 - 0.5)) = -16 -> newRatingB = 1184
        PlayerRating prA = new PlayerRating(playerA, game, 1200, 0);
        PlayerRating prB = new PlayerRating(playerB, game, 1200, 0);

        when(playerRatingRepository.findByUserIdAndGameId(1L, 10L)).thenReturn(Optional.of(prA));
        when(playerRatingRepository.findByUserIdAndGameId(2L, 10L)).thenReturn(Optional.of(prB));

        eloService.recordMatch(1L, 2L, 10L, "RANKED", 21, 15);

        // Verify updated ratings
        assertEquals(1216, prA.getCurrentRating(), "Player A should increase from 1200 to 1216 (+16)");
        assertEquals(1, prA.getMatchesPlayed(), "Player A matches played should increment to 1");

        assertEquals(1184, prB.getCurrentRating(), "Player B should decrease from 1200 to 1184 (-16)");
        assertEquals(1, prB.getMatchesPlayed(), "Player B matches played should increment to 1");

        // Verify match results rating changes
        ArgumentCaptor<MatchResult> resultCaptor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository, times(2)).save(resultCaptor.capture());
        List<MatchResult> results = resultCaptor.getAllValues();

        assertEquals(16, results.get(0).getRatingChange(), "Player A rating change in match result should be +16");
        assertEquals("WIN", results.get(0).getOutcome());

        assertEquals(-16, results.get(1).getRatingChange(), "Player B rating change in match result should be -16");
        assertEquals("LOSS", results.get(1).getOutcome());
    }

    @Test
    @DisplayName("Case 2: K-factor transition threshold at 30 matches: 29 matches played becomes 30 (K=16) -> delta +8")
    void testKFactorTransitionAt30Matches() {
        // Player A has 29 matches played beforehand.
        // During match: matchesPlayedA = 29 + 1 = 30.
        // Condition: matchesPlayedA < 30 ? 32 : 16 -> 30 is NOT < 30, so K drops to 16!
        // expectedA = 0.5, actualA = 1.0 (win)
        // deltaA = round(16 * (1.0 - 0.5)) = +8 -> newRatingA = 1208
        PlayerRating prA = new PlayerRating(playerA, game, 1200, 29);

        // Player B has 5 matches played beforehand -> matchesPlayedB = 6 (< 30 => K=32)
        // deltaB = round(32 * (0.0 - 0.5)) = -16 -> newRatingB = 1184
        PlayerRating prB = new PlayerRating(playerB, game, 1200, 5);

        when(playerRatingRepository.findByUserIdAndGameId(1L, 10L)).thenReturn(Optional.of(prA));
        when(playerRatingRepository.findByUserIdAndGameId(2L, 10L)).thenReturn(Optional.of(prB));

        eloService.recordMatch(1L, 2L, 10L, "RANKED", 10, 8);

        assertEquals(1208, prA.getCurrentRating(), "At 30 matches, K=16 applies -> delta = +8 (1208)");
        assertEquals(30, prA.getMatchesPlayed());

        assertEquals(1184, prB.getCurrentRating(), "At 6 matches, K=32 applies -> delta = -16 (1184)");
        assertEquals(6, prB.getMatchesPlayed());
    }

    @Test
    @DisplayName("Case 2b: Boundary just below threshold (28 matches played becomes 29): K=32 still applies -> delta +16")
    void testKFactorJustBelowThreshold29Matches() {
        // Player A has 28 matches played beforehand -> 28 + 1 = 29 matches (< 30 => K=32)
        PlayerRating prA28 = new PlayerRating(playerA, game, 1200, 28);
        PlayerRating prB = new PlayerRating(playerB, game, 1200, 0);

        when(playerRatingRepository.findByUserIdAndGameId(1L, 10L)).thenReturn(Optional.of(prA28));
        when(playerRatingRepository.findByUserIdAndGameId(2L, 10L)).thenReturn(Optional.of(prB));

        eloService.recordMatch(1L, 2L, 10L, "RANKED", 10, 8);
        assertEquals(1216, prA28.getCurrentRating(), "At 29 matches, K=32 still applies -> delta = +16 (1216)");
        assertEquals(29, prA28.getMatchesPlayed());
    }

    @Test
    @DisplayName("Case 3: Unequal ratings with a DRAW (Player A 1400 vs Player B 1200, both K=32) -> Higher rated loses points, lower rated gains points")
    void testDrawWithUnequalRatings() {
        // Player A: 1400 rating, 0 matches played
        // Player B: 1200 rating, 0 matches played
        // Score: 10 vs 10 (DRAW)
        // expectedA = 1 / (1 + 10^((1200-1400)/400)) = 1 / (1 + 10^-0.5) = 1 / (1 + 0.3162277) ≈ 0.75975
        // actualA = 0.5
        // deltaA = round(32 * (0.5 - 0.75975)) = round(32 * -0.25975) = round(-8.312) = -8
        // newRatingA = 1400 - 8 = 1392
        //
        // expectedB = 1 / (1 + 10^((1400-1200)/400)) = 1 / (1 + 10^0.5) = 1 / (1 + 3.162277) ≈ 0.24025
        // actualB = 0.5
        // deltaB = round(32 * (0.5 - 0.24025)) = round(32 * +0.25975) = round(+8.312) = +8
        // newRatingB = 1200 + 8 = 1208
        PlayerRating prA = new PlayerRating(playerA, game, 1400, 0);
        PlayerRating prB = new PlayerRating(playerB, game, 1200, 0);

        when(playerRatingRepository.findByUserIdAndGameId(1L, 10L)).thenReturn(Optional.of(prA));
        when(playerRatingRepository.findByUserIdAndGameId(2L, 10L)).thenReturn(Optional.of(prB));

        eloService.recordMatch(1L, 2L, 10L, "RANKED", 10, 10);

        assertEquals(1392, prA.getCurrentRating(), "Player A (1400) drew against weaker player -> loses 8 points (1392)");
        assertEquals(1208, prB.getCurrentRating(), "Player B (1200) drew against stronger player -> gains 8 points (1208)");
    }
}
