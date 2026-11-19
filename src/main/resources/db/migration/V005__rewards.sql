CREATE TABLE rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NULL,
    points_cost INT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_rewards_points_cost CHECK (points_cost > 0),
    CONSTRAINT chk_rewards_stock_quantity CHECK (stock_quantity >= 0)
);

-- Seed 4 rewards
INSERT INTO rewards (name, description, points_cost, stock_quantity, is_active) VALUES
('1 Hour Free Gaming Pass', 'Enjoy 1 hour of complimentary gameplay on any Standard PC or PS5 station.', 100, 50, TRUE),
('Arena Hub Energy Drink', 'Redeem for any energy drink or iced coffee at the Arena cafe counter.', 30, 100, TRUE),
('Pro Gaming Mousepad', 'High-grade speed cloth Arena Hub branded non-slip mousepad.', 250, 20, TRUE),
('VIP Rig Day Pass', 'Full-day unlimited access to VIP high-end stations and racing simulator.', 500, 5, TRUE);
