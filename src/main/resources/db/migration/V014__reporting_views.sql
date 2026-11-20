CREATE VIEW daily_revenue AS 
SELECT 
    DATE(start_time) AS booking_date, 
    station_id, 
    SUM(total_price) AS revenue, 
    COUNT(*) AS bookings 
FROM bookings 
WHERE status <> 'CANCELLED' 
GROUP BY DATE(start_time), station_id;

CREATE VIEW peak_hours AS 
SELECT 
    HOUR(start_time) AS hour_of_day, 
    COUNT(*) AS booking_count 
FROM bookings 
WHERE status <> 'CANCELLED' 
GROUP BY HOUR(start_time);
