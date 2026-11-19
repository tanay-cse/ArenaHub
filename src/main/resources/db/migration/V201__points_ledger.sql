CREATE TABLE points_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    points INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    ref_type VARCHAR(50) NULL,
    ref_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_points_ledger_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_points_ledger_user_id ON points_ledger (user_id);

-- Seed a few rows for the demo student (user_id = 3 / student@arenahub.com)
INSERT INTO points_ledger (user_id, points, reason, ref_type, ref_id, created_at) VALUES
((SELECT id FROM users WHERE email = 'student@arenahub.com'), 100, 'Welcome bonus points', 'REGISTRATION', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
((SELECT id FROM users WHERE email = 'student@arenahub.com'), 50, 'Station booking loyalty reward', 'BOOKING', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
((SELECT id FROM users WHERE email = 'student@arenahub.com'), -30, 'Redeemed snack item', 'REDEMPTION', NULL, NOW());
