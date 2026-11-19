CREATE TABLE waitlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    requested_date DATE NOT NULL,
    requested_hour TINYINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified_at DATETIME NULL,
    CONSTRAINT fk_waitlist_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_waitlist_station FOREIGN KEY (station_id) REFERENCES stations (id),
    CONSTRAINT uq_waitlist_user_slot UNIQUE (user_id, station_id, requested_date, requested_hour),
    CONSTRAINT chk_waitlist_hour CHECK (requested_hour BETWEEN 10 AND 21),
    CONSTRAINT chk_waitlist_status CHECK (status IN ('WAITING', 'NOTIFIED', 'EXPIRED', 'FULFILLED', 'CANCELLED'))
);

CREATE INDEX idx_waitlist_slot ON waitlist (station_id, requested_date, requested_hour, status);
