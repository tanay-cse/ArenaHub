CREATE TABLE player_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    current_rating INT NOT NULL DEFAULT 1200,
    matches_played INT NOT NULL DEFAULT 0,
    last_updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_player_ratings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_ratings_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE RESTRICT,
    CONSTRAINT uk_player_ratings_user_game UNIQUE (user_id, game_id),
    CONSTRAINT chk_player_ratings_current_rating CHECK (current_rating >= 0),
    CONSTRAINT chk_player_ratings_matches_played CHECK (matches_played >= 0)
);

CREATE INDEX idx_player_ratings_game_rating ON player_ratings (game_id, current_rating DESC);

CREATE TABLE matches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id BIGINT NOT NULL,
    tournament_id BIGINT NULL,
    next_match_id BIGINT NULL,
    round_number INT NULL,
    bracket_position INT NULL,
    match_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    CONSTRAINT fk_matches_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE RESTRICT,
    CONSTRAINT fk_matches_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE SET NULL,
    CONSTRAINT fk_matches_next_match FOREIGN KEY (next_match_id) REFERENCES matches (id) ON DELETE SET NULL,
    CONSTRAINT chk_matches_match_type CHECK (match_type IN ('CASUAL','RANKED','TOURNAMENT')),
    CONSTRAINT chk_matches_status CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','DISPUTED','CANCELLED'))
);

CREATE INDEX idx_matches_tournament_id ON matches (tournament_id);
CREATE INDEX idx_matches_status_scheduled ON matches (status, scheduled_at);

CREATE TABLE match_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    outcome VARCHAR(20) NULL,
    score INT NULL,
    rating_change INT NOT NULL DEFAULT 0,
    rank_placement INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_results_match FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_match_results_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_match_results_match_user UNIQUE (match_id, user_id),
    CONSTRAINT chk_match_results_outcome CHECK (outcome IN ('WIN','LOSS','DRAW','FORFEIT'))
);

CREATE INDEX idx_match_results_user_id ON match_results (user_id);
