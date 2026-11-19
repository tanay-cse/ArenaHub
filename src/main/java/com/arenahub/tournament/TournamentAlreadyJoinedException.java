package com.arenahub.tournament;

public class TournamentAlreadyJoinedException extends RuntimeException {
    public TournamentAlreadyJoinedException(String message) {
        super(message);
    }
}
