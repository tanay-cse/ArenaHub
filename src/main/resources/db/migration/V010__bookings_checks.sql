ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_one_hour CHECK (TIMESTAMPDIFF(MINUTE, start_time, end_time) = 60),
    ADD CONSTRAINT chk_bookings_on_the_hour CHECK (MINUTE(start_time) = 0 AND SECOND(start_time) = 0);
