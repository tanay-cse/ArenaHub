CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_price DECIMAL(10,2) NOT NULL,
    active_slot_start DATETIME GENERATED ALWAYS AS (IF(status = 'CANCELLED', NULL, start_time)) STORED,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_station FOREIGN KEY (station_id) REFERENCES stations (id),
    CONSTRAINT uq_bookings_active_slot UNIQUE (station_id, active_slot_start),
    CONSTRAINT chk_bookings_time CHECK (end_time > start_time),
    CONSTRAINT chk_bookings_price CHECK (total_price >= 0.00),
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_bookings_station_time ON bookings (station_id, start_time, end_time);
CREATE INDEX idx_bookings_user_id ON bookings (user_id, created_at);
CREATE INDEX idx_bookings_status ON bookings (status);
