CREATE TABLE badges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    icon VARCHAR(50) NOT NULL
);

CREATE TABLE user_badges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    earned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_badges_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_badges_badge FOREIGN KEY (badge_id) REFERENCES badges (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_badge UNIQUE (user_id, badge_id)
);

CREATE INDEX idx_user_badges_user_id ON user_badges (user_id);
CREATE INDEX idx_user_badges_badge_id ON user_badges (badge_id);

INSERT INTO badges (code, name, description, icon) VALUES
('5_WIN_STREAK', '5-Win Streak', 'Won 5 consecutive matches in a game', 'bi-fire'),
('TOP_10', 'Top 10 Player', 'Placed in the top 10 on a game leaderboard', 'bi-trophy-fill'),
('FIRST_WIN', 'First Victory', 'Achieved first match victory', 'bi-award-fill');
