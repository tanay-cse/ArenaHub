CREATE TABLE games (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL UNIQUE,
    genre VARCHAR(50) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    min_age INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_games_min_age CHECK (min_age >= 0)
);

CREATE TABLE stations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_number VARCHAR(20) NOT NULL UNIQUE,
    station_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    hourly_rate DECIMAL(10,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stations_hourly_rate CHECK (hourly_rate >= 0),
    CONSTRAINT chk_stations_status CHECK (status IN ('AVAILABLE','OCCUPIED','MAINTENANCE','RESERVED'))
);

CREATE TABLE station_games (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    installed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_station_games_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE CASCADE,
    CONSTRAINT fk_station_games_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE RESTRICT,
    CONSTRAINT uk_station_game UNIQUE (station_id, game_id)
);

CREATE INDEX idx_station_games_game_id ON station_games (game_id);

-- Seed 3 games
INSERT INTO games (title, genre, platform, min_age, is_active) VALUES
('Valorant', 'Tactical FPS', 'PC', 16, TRUE),
('EA Sports FC 25', 'Sports', 'CROSS_PLATFORM', 3, TRUE),
('Assetto Corsa Competizione', 'Racing Sim', 'PC', 3, TRUE);

-- Seed 4 stations (types PC_VIP, PC_STANDARD, PS5, SIMULATOR)
INSERT INTO stations (station_number, station_type, status, hourly_rate) VALUES
('ST-VIP-01', 'PC_VIP', 'AVAILABLE', 150.00),
('ST-STD-01', 'PC_STANDARD', 'AVAILABLE', 80.00),
('ST-PS5-01', 'PS5', 'AVAILABLE', 100.00),
('ST-SIM-01', 'SIMULATOR', 'AVAILABLE', 200.00);

-- Seed station_games
INSERT INTO station_games (station_id, game_id) VALUES
((SELECT id FROM stations WHERE station_number = 'ST-VIP-01'), (SELECT id FROM games WHERE title = 'Valorant')),
((SELECT id FROM stations WHERE station_number = 'ST-VIP-01'), (SELECT id FROM games WHERE title = 'EA Sports FC 25')),
((SELECT id FROM stations WHERE station_number = 'ST-STD-01'), (SELECT id FROM games WHERE title = 'Valorant')),
((SELECT id FROM stations WHERE station_number = 'ST-PS5-01'), (SELECT id FROM games WHERE title = 'EA Sports FC 25')),
((SELECT id FROM stations WHERE station_number = 'ST-SIM-01'), (SELECT id FROM games WHERE title = 'Assetto Corsa Competizione'));
