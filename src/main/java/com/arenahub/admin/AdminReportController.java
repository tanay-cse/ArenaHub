package com.arenahub.admin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public String viewReports(Model model) {
        List<AdminReportService.StationRevenueItem> stationRevenues = adminReportService.getStationRevenues();
        List<String> stationRevenueLabels = stationRevenues.stream().map(AdminReportService.StationRevenueItem::stationLabel).toList();
        List<BigDecimal> stationRevenueData = stationRevenues.stream().map(AdminReportService.StationRevenueItem::totalRevenue).toList();

        AdminReportService.DailyBookingsReport dailyBookings = adminReportService.getDailyBookingsLast30Days();
        List<AdminReportService.PeakHourItem> peakHours = adminReportService.getPeakHours();

        model.addAttribute("stationRevenueLabels", stationRevenueLabels);
        model.addAttribute("stationRevenueData", stationRevenueData);
        model.addAttribute("dailyBookingsLabels", dailyBookings.labels());
        model.addAttribute("dailyBookingsData", dailyBookings.data());
        model.addAttribute("peakHours", peakHours);

        return "admin/reports";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<StreamingResponseBody> exportDailyRevenueCsv() {
        StreamingResponseBody stream = out -> adminReportService.streamDailyRevenueCsv(out);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily_revenue.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(stream);
    }
}
