CREATE TABLE tournaments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    format VARCHAR(30) NOT NULL DEFAULT 'SINGLE_ELIMINATION',
    status VARCHAR(30) NOT NULL DEFAULT 'ANNOUNCED',
    max_participants INT NOT NULL,
    entry_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    prize_pool DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    start_time DATETIME NOT NULL,
    end_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournaments_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE RESTRICT,
    CONSTRAINT chk_tournaments_format CHECK (format IN ('SINGLE_ELIMINATION','DOUBLE_ELIMINATION','ROUND_ROBIN','SWISS')),
    CONSTRAINT chk_tournaments_status CHECK (status IN ('ANNOUNCED','REGISTRATION_OPEN','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT chk_tournaments_max_participants CHECK (max_participants > 1),
    CONSTRAINT chk_tournaments_entry_fee CHECK (entry_fee >= 0),
    CONSTRAINT chk_tournaments_prize_pool CHECK (prize_pool >= 0)
);

CREATE INDEX idx_tournaments_game_status ON tournaments (game_id, status);

CREATE TABLE tournament_players (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    seed INT NULL,
    final_rank INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournament_players_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE CASCADE,
    CONSTRAINT fk_tournament_players_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_tournament_player UNIQUE (tournament_id, user_id),
    CONSTRAINT chk_tournament_players_seed CHECK (seed > 0),
    CONSTRAINT chk_tournament_players_final_rank CHECK (final_rank > 0)
);

CREATE INDEX idx_tournament_players_user_id ON tournament_players (user_id);

-- Seed 1 tournament with status REGISTRATION_OPEN and a start_time in the future
INSERT INTO tournaments (game_id, name, format, status, max_participants, entry_fee, prize_pool, start_time) VALUES
((SELECT id FROM games WHERE title = 'Valorant'), 'Valorant Spring Invitational', 'SINGLE_ELIMINATION', 'REGISTRATION_OPEN', 16, 25.00, 250.00, DATE_ADD(NOW(), INTERVAL 7 DAY));
