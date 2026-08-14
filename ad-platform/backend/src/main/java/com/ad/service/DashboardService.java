package com.ad.service;

import com.ad.dto.DashboardOverviewDTO;
import com.ad.dto.TrendDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DashboardService {

    DashboardOverviewDTO getOverview(LocalDate startDate, LocalDate endDate);

    List<TrendDTO> getTrends(LocalDate startDate, LocalDate endDate, Long campaignId);

    List<Map<String, Object>> getChannelDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getMaterialTop(LocalDate startDate, LocalDate endDate, int limit);
}
