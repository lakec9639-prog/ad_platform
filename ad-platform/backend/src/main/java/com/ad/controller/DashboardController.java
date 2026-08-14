package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.DashboardOverviewDTO;
import com.ad.dto.TrendDTO;
import com.ad.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardOverviewDTO> overview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getOverview(start, end));
    }

    @GetMapping("/trends")
    public Result<List<TrendDTO>> trends(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long campaignId) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getTrends(start, end, campaignId));
    }

    @GetMapping("/channel-dist")
    public Result<List<Map<String, Object>>> channelDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getChannelDistribution(start, end));
    }

    @GetMapping("/material-top")
    public Result<List<Map<String, Object>>> materialTop(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "5") int limit) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getMaterialTop(start, end, limit));
    }
}
