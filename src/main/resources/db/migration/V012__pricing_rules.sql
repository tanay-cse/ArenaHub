CREATE TABLE pricing_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    station_type VARCHAR(30) NULL,
    start_hour TINYINT NOT NULL,
    end_hour TINYINT NOT NULL,
    rate_type VARCHAR(20) NOT NULL,
    multiplier DECIMAL(4,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pricing_hours CHECK (start_hour < end_hour),
    CONSTRAINT chk_pricing_multiplier CHECK (multiplier > 0),
    CONSTRAINT chk_pricing_rate_type CHECK (rate_type IN ('PEAK', 'OFF_PEAK', 'MEMBER'))
);

INSERT INTO pricing_rules (rule_name, station_type, start_hour, end_hour, rate_type, multiplier, is_active)
VALUES
('Evening Peak', NULL, 18, 22, 'PEAK', 1.50, TRUE),
('Morning Off-Peak', NULL, 10, 14, 'OFF_PEAK', 0.80, TRUE),
('Member Discount', NULL, 0, 24, 'MEMBER', 0.90, TRUE);
