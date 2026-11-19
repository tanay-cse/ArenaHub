package com.arenahub.tournament;

import com.arenahub.audit.AuditService;
import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final UserRepository userRepository;
    private final com.arenahub.catalog.GameRepository gameRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public TournamentService(TournamentRepository tournamentRepository,
                             TournamentPlayerRepository tournamentPlayerRepository,
                             UserRepository userRepository,
                             com.arenahub.catalog.GameRepository gameRepository,
                             AuditService auditService,
                             ObjectMapper objectMapper) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentPlayerRepository = tournamentPlayerRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
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
}
