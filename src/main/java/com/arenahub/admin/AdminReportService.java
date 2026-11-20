package com.arenahub.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportService {

    private final JdbcTemplate jdbcTemplate;

    public AdminReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record StationRevenueItem(Long stationId, String stationLabel, BigDecimal totalRevenue) {}
    public record DailyBookingsReport(List<String> labels, List<Long> data) {}
    public record PeakHourItem(int hourOfDay, String formattedHour, long bookingCount) {}

    @Transactional(readOnly = true)
    public List<StationRevenueItem> getStationRevenues() {
        String sql = """
            SELECT dr.station_id, 
                   COALESCE(s.station_number, CONCAT('Station ', dr.station_id)) AS station_label, 
                   SUM(dr.revenue) AS total_revenue
            FROM daily_revenue dr
            LEFT JOIN stations s ON s.id = dr.station_id
            GROUP BY dr.station_id, s.station_number
            ORDER BY dr.station_id ASC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StationRevenueItem(
                rs.getLong("station_id"),
                rs.getString("station_label"),
                rs.getBigDecimal("total_revenue") != null ? rs.getBigDecimal("total_revenue").setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO
        ));
    }

    @Transactional(readOnly = true)
    public DailyBookingsReport getDailyBookingsLast30Days() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Long> dateBookingMap = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            dateBookingMap.put(startDate.plusDays(i).format(formatter), 0L);
        }

        String sql = """
            SELECT booking_date, SUM(bookings) AS total_bookings
            FROM daily_revenue
            WHERE booking_date >= ?
            GROUP BY booking_date
            ORDER BY booking_date ASC
            """;

        jdbcTemplate.query(sql, ps -> ps.setDate(1, java.sql.Date.valueOf(startDate)), rs -> {
            String dateStr = rs.getString("booking_date");
            long bookings = rs.getLong("total_bookings");
            if (dateStr != null && dateBookingMap.containsKey(dateStr)) {
                dateBookingMap.put(dateStr, bookings);
            }
        });

        List<String> labels = new ArrayList<>(dateBookingMap.keySet());
        List<Long> data = new ArrayList<>(dateBookingMap.values());
        return new DailyBookingsReport(labels, data);
    }

    @Transactional(readOnly = true)
    public List<PeakHourItem> getPeakHours() {
        String sql = """
            SELECT hour_of_day, booking_count
            FROM peak_hours
            ORDER BY hour_of_day ASC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            int hour = rs.getInt("hour_of_day");
            long count = rs.getLong("booking_count");
            String formattedHour = String.format("%02d:00 - %02d:00", hour, (hour + 1) % 24);
            return new PeakHourItem(hour, formattedHour, count);
        });
    }

    @Transactional(readOnly = true)
    public void streamDailyRevenueCsv(OutputStream outputStream) {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        writer.println("station,date,revenue,bookings");

        String sql = """
            SELECT station_id, booking_date, revenue, bookings
            FROM daily_revenue
            ORDER BY booking_date DESC, station_id ASC
            """;

        jdbcTemplate.query(sql, rs -> {
            long stationId = rs.getLong("station_id");
            String dateStr = rs.getString("booking_date");
            BigDecimal revenue = rs.getBigDecimal("revenue");
            long bookings = rs.getLong("bookings");

            String revStr = (revenue != null) ? revenue.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            writer.printf("%d,%s,%s,%d%n", stationId, dateStr, revStr, bookings);
        });

        writer.flush();
    }
}
