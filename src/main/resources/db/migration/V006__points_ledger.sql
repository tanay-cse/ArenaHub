CREATE TABLE points_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    booking_id BIGINT NULL,
    reward_id BIGINT NULL,
    points_change INT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_points_ledger_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_points_ledger_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE SET NULL,
    CONSTRAINT fk_points_ledger_reward FOREIGN KEY (reward_id) REFERENCES rewards (id) ON DELETE SET NULL,
    CONSTRAINT chk_points_ledger_points_change CHECK (points_change <> 0),
    CONSTRAINT chk_points_ledger_transaction_type CHECK (transaction_type IN ('EARNED_BOOKING', 'REWARD_REDEMPTION', 'ADJUSTMENT'))
);

CREATE INDEX idx_points_ledger_user_created ON points_ledger (user_id, created_at);

-- Seed a few ADJUSTMENT rows for the demo student
INSERT INTO points_ledger (user_id, points_change, transaction_type, description, created_at) VALUES
((SELECT id FROM users WHERE email = 'student@arenahub.com'), 100, 'ADJUSTMENT', 'Welcome bonus points', DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM users WHERE email = 'student@arenahub.com'), 50, 'ADJUSTMENT', 'Event participation goodwill bonus', DATE_SUB(NOW(), INTERVAL 1 DAY)),
((SELECT id FROM users WHERE email = 'student@arenahub.com'), -25, 'ADJUSTMENT', 'Manual points adjustment', NOW());
