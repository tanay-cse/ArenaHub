package com.arenahub.tournament;

import com.arenahub.audit.AuditService;
import com.arenahub.auth.Role;
import com.arenahub.auth.User;
import com.arenahub.catalog.Game;
import com.arenahub.elo.Match;
import com.arenahub.elo.MatchRepository;
import com.arenahub.elo.MatchResult;
import com.arenahub.elo.MatchResultRepository;
import com.arenahub.elo.PlayerRatingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentPlayerRepository tournamentPlayerRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private PlayerRatingRepository playerRatingRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TournamentService tournamentService;

    private Tournament tournament;
    private Game game;
    private AtomicLong matchIdCounter;
    private AtomicLong matchResultIdCounter;
    private List<Match> savedMatches;
    private List<MatchResult> savedMatchResults;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.setId(100L);
        game.setTitle("Counter-Strike 2");

        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Spring Championship");
        tournament.setGame(game);
        tournament.setStatus("REGISTRATION_OPEN");
        tournament.setStartTime(LocalDateTime.now().plusDays(2));
        tournament.setFormat("SINGLE_ELIMINATION");
        tournament.setMaxParticipants(16);
        tournament.setEntryFee(BigDecimal.ZERO);
        tournament.setPrizePool(new BigDecimal("1000.00"));

        matchIdCounter = new AtomicLong(100);
        matchResultIdCounter = new AtomicLong(500);
        savedMatches = new ArrayList<>();
        savedMatchResults = new ArrayList<>();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(matchIdCounter.incrementAndGet());
            }
            if (!savedMatches.contains(m)) {
                savedMatches.add(m);
            }
            return m;
        });

        lenient().when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(inv -> {
            MatchResult mr = inv.getArgument(0);
            if (mr.getId() == null) {
                mr.setId(matchResultIdCounter.incrementAndGet());
            }
            savedMatchResults.add(mr);
            return mr;
        });

        lenient().when(tournamentPlayerRepository.save(any(TournamentPlayer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private List<TournamentPlayer> createRegisteredPlayers(int count) {
        List<TournamentPlayer> players = new ArrayList<>();
        Role role = new Role("STUDENT");
        for (int i = 1; i <= count; i++) {
            User u = new User();
            u.setId((long) i);
            u.setEmail("player" + i + "@arenahub.com");
            u.setFullName("Player " + i);
            u.setRole(role);
            u.setIsActive(true);

            TournamentPlayer tp = new TournamentPlayer();
            tp.setId((long) (1000 + i));
            tp.setTournament(tournament);
            tp.setUser(u);
            tp.setSeed(i);
            tp.setRegisteredAt(LocalDateTime.now().minusMinutes(count - i));

            players.add(tp);
        }
        return players;
    }

    @Test
    @DisplayName("3 Players: Bye-padding (padded to size 4, 1 bye) and next_match_id chaining across 2 rounds")
    void testBracketForThreePlayers() {
        List<TournamentPlayer> players = createRegisteredPlayers(3);
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        tournamentService.generateBracket(1L, "MANUAL", 999L);

        // Padded size: 4. Total rounds: 2 (Round 1: 2 matches, Round 2 / Final: 1 match). Total matches: 3.
        assertEquals(3, savedMatches.size(), "Size 4 bracket should have 3 matches in total (2 in R1, 1 in R2)");

        // Separate matches by round
        Match r2Match = savedMatches.stream().filter(m -> m.getRoundNumber() == 2).findFirst().orElseThrow();
        List<Match> r1Matches = savedMatches.stream().filter(m -> m.getRoundNumber() == 1).toList();

        assertEquals(1, r2Match.getBracketPosition());
        assertNull(r2Match.getNextMatch(), "Final match (Round 2) has no nextMatch (next_match_id is null)");

        assertEquals(2, r1Matches.size(), "Round 1 must have 2 matches");

        // Verify next_match_id chaining: Both Round 1 matches must chain to the Round 2 Final match
        for (Match r1m : r1Matches) {
            assertNotNull(r1m.getNextMatch(), "Round 1 match must have nextMatch assigned");
            assertEquals(r2Match.getId(), r1m.getNextMatch().getId(),
                    "Round 1 match must chain to Round 2 parent match (next_match_id)");
        }

        // Verify bye padding: 4 - 3 = 1 bye
        // Seeds for size 4 are [1, 4, 2, 3].
        // Match 1 (pos 1): Seed 1 vs Seed 4 (BYE). Seed 4 is missing, so Match 1 is COMPLETED automatically.
        // Match 2 (pos 2): Seed 2 vs Seed 3. Both are real players, so Match 2 is SCHEDULED.
        Match byeMatch = r1Matches.stream().filter(m -> m.getBracketPosition() == 1).findFirst().orElseThrow();
        assertEquals("COMPLETED", byeMatch.getStatus(), "Match 1 with bye must be COMPLETED on bracket generation");

        Match scheduledMatch = r1Matches.stream().filter(m -> m.getBracketPosition() == 2).findFirst().orElseThrow();
        assertEquals("SCHEDULED", scheduledMatch.getStatus(), "Match 2 with real pairing must be SCHEDULED");

        // Verify bye winner is automatically advanced to next round's match
        boolean byeWinnerAdvanced = savedMatchResults.stream()
                .anyMatch(mr -> mr.getMatch().getId().equals(r2Match.getId()) && mr.getUser().getId().equals(1L));
        assertTrue(byeWinnerAdvanced, "Player 1 (seed 1 with bye) must be advanced to Round 2 match results");

        assertEquals("IN_PROGRESS", tournament.getStatus());
    }

    @Test
    @DisplayName("5 Players: Bye-padding (padded to size 8, 3 byes) and next_match_id chaining across 3 rounds")
    void testBracketForFivePlayers() {
        List<TournamentPlayer> players = createRegisteredPlayers(5);
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        tournamentService.generateBracket(1L, "MANUAL", 999L);

        // Padded size: 8. Total rounds: 3.
        // Round 1: 4 matches, Round 2: 2 matches, Round 3 (Final): 1 match. Total matches: 7.
        assertEquals(7, savedMatches.size(), "Size 8 bracket should have 7 matches in total (4 in R1, 2 in R2, 1 in R3)");

        Match r3Final = savedMatches.stream().filter(m -> m.getRoundNumber() == 3).findFirst().orElseThrow();
        assertNull(r3Final.getNextMatch(), "Round 3 Final must have null nextMatch");

        List<Match> r2Matches = savedMatches.stream().filter(m -> m.getRoundNumber() == 2).toList();
        assertEquals(2, r2Matches.size(), "Round 2 must have 2 matches");

        // Verify Round 2 next_match_id chaining to Round 3 Final
        for (Match r2m : r2Matches) {
            assertNotNull(r2m.getNextMatch());
            assertEquals(r3Final.getId(), r2m.getNextMatch().getId(), "Round 2 matches chain to Round 3 Final");
        }

        List<Match> r1Matches = savedMatches.stream().filter(m -> m.getRoundNumber() == 1)
                .sorted(Comparator.comparingInt(Match::getBracketPosition))
                .toList();
        assertEquals(4, r1Matches.size(), "Round 1 must have 4 matches");

        Match r2Pos1 = r2Matches.stream().filter(m -> m.getBracketPosition() == 1).findFirst().orElseThrow();
        Match r2Pos2 = r2Matches.stream().filter(m -> m.getBracketPosition() == 2).findFirst().orElseThrow();

        // R1 matches 1 & 2 feed R2 pos 1; R1 matches 3 & 4 feed R2 pos 2
        assertEquals(r2Pos1.getId(), r1Matches.get(0).getNextMatch().getId());
        assertEquals(r2Pos1.getId(), r1Matches.get(1).getNextMatch().getId());
        assertEquals(r2Pos2.getId(), r1Matches.get(2).getNextMatch().getId());
        assertEquals(r2Pos2.getId(), r1Matches.get(3).getNextMatch().getId());

        // Verify bye padding: 8 - 5 = 3 byes!
        // Seeds for size 8: [1, 8, 4, 5, 2, 7, 3, 6]
        // Real: 1, 2, 3, 4, 5. Byes: 6, 7, 8.
        // Match 1 (pos 1): Seed 1 vs Seed 8 (BYE) -> COMPLETED
        // Match 2 (pos 2): Seed 4 vs Seed 5 (REAL) -> SCHEDULED
        // Match 3 (pos 3): Seed 2 vs Seed 7 (BYE) -> COMPLETED
        // Match 4 (pos 4): Seed 3 vs Seed 6 (BYE) -> COMPLETED
        long completedR1 = r1Matches.stream().filter(m -> "COMPLETED".equals(m.getStatus())).count();
        long scheduledR1 = r1Matches.stream().filter(m -> "SCHEDULED".equals(m.getStatus())).count();

        assertEquals(3, completedR1, "Exactly 3 matches in Round 1 should be COMPLETED due to byes");
        assertEquals(1, scheduledR1, "Exactly 1 match in Round 1 should be SCHEDULED (Seed 4 vs Seed 5)");

        assertEquals("SCHEDULED", r1Matches.get(1).getStatus()); // Match pos 2 is the only real match

        // 3 bye winners (seeds 1, 2, 3) advanced to Round 2
        long round2AdvanceCount = savedMatchResults.stream()
                .filter(mr -> mr.getMatch().getRoundNumber() == 2)
                .count();
        assertEquals(3, round2AdvanceCount, "3 bye winners must be auto-advanced into Round 2 match results");
    }

    @Test
    @DisplayName("8 Players: Zero byes (full bracket of 8) and complete next_match_id chaining across 3 rounds")
    void testBracketForEightPlayers() {
        List<TournamentPlayer> players = createRegisteredPlayers(8);
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        tournamentService.generateBracket(1L, "MANUAL", 999L);

        // Padded size: 8. Total matches: 7.
        assertEquals(7, savedMatches.size());

        List<Match> r1Matches = savedMatches.stream().filter(m -> m.getRoundNumber() == 1).toList();
        List<Match> r2Matches = savedMatches.stream().filter(m -> m.getRoundNumber() == 2).toList();
        Match r3Final = savedMatches.stream().filter(m -> m.getRoundNumber() == 3).findFirst().orElseThrow();

        assertEquals(4, r1Matches.size());
        assertEquals(2, r2Matches.size());
        assertNull(r3Final.getNextMatch());

        // Verify zero byes: all 4 Round 1 matches are SCHEDULED with real players
        long scheduledCount = r1Matches.stream().filter(m -> "SCHEDULED".equals(m.getStatus())).count();
        assertEquals(4, scheduledCount, "All 4 Round 1 matches must be SCHEDULED when there are 8 players (0 byes)");

        long completedCount = r1Matches.stream().filter(m -> "COMPLETED".equals(m.getStatus())).count();
        assertEquals(0, completedCount, "No Round 1 matches are COMPLETED because there are 0 byes");

        // Verify complete next_match_id chaining
        Match r2Pos1 = r2Matches.stream().filter(m -> m.getBracketPosition() == 1).findFirst().orElseThrow();
        Match r2Pos2 = r2Matches.stream().filter(m -> m.getBracketPosition() == 2).findFirst().orElseThrow();

        for (Match m : r1Matches) {
            int pos = m.getBracketPosition();
            Match expectedParent = (pos <= 2) ? r2Pos1 : r2Pos2;
            assertEquals(expectedParent.getId(), m.getNextMatch().getId(),
                    "Round 1 match position " + pos + " must chain to correct Round 2 match");
        }

        for (Match m : r2Matches) {
            assertEquals(r3Final.getId(), m.getNextMatch().getId(),
                    "Round 2 match must chain to Round 3 final");
        }
    }
}
