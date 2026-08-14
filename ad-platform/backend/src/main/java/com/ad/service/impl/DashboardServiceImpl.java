package com.ad.service.impl;

import com.ad.dto.DashboardOverviewDTO;
import com.ad.dto.TrendDTO;
import com.ad.mapper.StatsHourlyMapper;
import com.ad.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StatsHourlyMapper statsHourlyMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "dash:";
    private static final long CACHE_TTL_MINUTES = 5;
    private static final BigDecimal BUDGET_TOTAL = new BigDecimal("800000");

    @Override
    public DashboardOverviewDTO getOverview(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Try cache first (gracefully handle Redis down)
        String cacheKey = CACHE_KEY_PREFIX + "overview:" + startDate + ":" + endDate;
        try {
            DashboardOverviewDTO cached = (DashboardOverviewDTO) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping cache: {}", e.getMessage());
        }

        // Query from MySQL
        List<Map<String, Object>> statsList = statsHourlyMapper.sumByDateRange(
                null, null, null, startDate, endDate);

        DashboardOverviewDTO dto = new DashboardOverviewDTO();

        if (!statsList.isEmpty()) {
            Map<String, Object> stats = statsList.get(0);
            BigDecimal totalCost = toBigDecimal(stats.get("total_cost"));
            BigDecimal totalNewUsers = toBigDecimal(stats.get("total_new_users"));
            BigDecimal totalConversions = toBigDecimal(stats.get("total_conversions"));
            BigDecimal totalGmv = toBigDecimal(stats.get("total_gmv"));
            BigDecimal totalImpressions = toBigDecimal(stats.get("total_impressions"));
            BigDecimal totalClicksBd = toBigDecimal(stats.get("total_clicks"));

            dto.setTotalCost(totalCost);
            dto.setTotalNewUsers(totalNewUsers);
            dto.setTotalConversions(totalConversions);
            dto.setTotalGmv(totalGmv);
            dto.setTotalImpressions(totalImpressions);
            dto.setTotalClicks(totalClicksBd.intValue());

            // CPA = cost / conversions
            if (totalConversions.compareTo(BigDecimal.ZERO) > 0) {
                dto.setCpa(totalCost.divide(totalConversions, 2, RoundingMode.HALF_UP));
            } else {
                dto.setCpa(BigDecimal.ZERO);
            }

            // ROAS = gmv / cost
            if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
                dto.setRoas(totalGmv.divide(totalCost, 2, RoundingMode.HALF_UP));
            } else {
                dto.setRoas(BigDecimal.ZERO);
            }

            // CTR = clicks / impressions
            if (totalImpressions.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ctr = totalClicksBd.multiply(BigDecimal.valueOf(100))
                        .divide(totalImpressions, 4, RoundingMode.HALF_UP);
                dto.setCtr(ctr);
            } else {
                dto.setCtr(BigDecimal.ZERO);
            }

            // CVR = conversions / clicks
            if (totalClicksBd.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cvr = totalConversions.multiply(BigDecimal.valueOf(100))
                        .divide(totalClicksBd, 4, RoundingMode.HALF_UP);
                dto.setCvr(cvr);
            } else {
                dto.setCvr(BigDecimal.ZERO);
            }

            // Budget progress
            dto.setBudgetTotal(BUDGET_TOTAL);
            if (BUDGET_TOTAL.compareTo(BigDecimal.ZERO) > 0) {
                dto.setBudgetProgress(totalCost.divide(BUDGET_TOTAL, 4, RoundingMode.HALF_UP));
                dto.setBudgetRemaining(BUDGET_TOTAL.subtract(totalCost));
            } else {
                dto.setBudgetProgress(BigDecimal.ZERO);
                dto.setBudgetRemaining(BUDGET_TOTAL);
            }
        } else {
            // Empty defaults
            dto.setTotalCost(BigDecimal.ZERO);
            dto.setTotalNewUsers(BigDecimal.ZERO);
            dto.setTotalConversions(BigDecimal.ZERO);
            dto.setTotalGmv(BigDecimal.ZERO);
            dto.setTotalImpressions(BigDecimal.ZERO);
            dto.setTotalClicks(0);
            dto.setCpa(BigDecimal.ZERO);
            dto.setRoas(BigDecimal.ZERO);
            dto.setCtr(BigDecimal.ZERO);
            dto.setCvr(BigDecimal.ZERO);
            dto.setBudgetTotal(BUDGET_TOTAL);
            dto.setBudgetProgress(BigDecimal.ZERO);
            dto.setBudgetRemaining(BUDGET_TOTAL);
        }

        // Cache result
        try {
            redisTemplate.opsForValue().set(cacheKey, dto, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache set failed: {}", e.getMessage());
        }

        return dto;
    }

    @Override
    public List<TrendDTO> getTrends(LocalDate startDate, LocalDate endDate, Long campaignId) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        String cacheKey = CACHE_KEY_PREFIX + "trends:" + startDate + ":" + endDate + ":cid=" + campaignId;
        try {
            @SuppressWarnings("unchecked")
            List<TrendDTO> cached = (List<TrendDTO>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping cache: {}", e.getMessage());
        }

        List<Map<String, Object>> rawData = statsHourlyMapper.dailyTrends(
                null, campaignId, null, startDate, endDate);

        // Group by stat_date
        Map<LocalDate, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rawData) {
            Object dateObj = row.get("stat_date");
            if (dateObj == null) continue;
            LocalDate date = extractLocalDate(dateObj);
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(row);
        }

        List<TrendDTO> trends = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Map<String, Object>>> entry : grouped.entrySet()) {
            long totalImpressions = 0;
            long totalClicks = 0;
            long totalConversions = 0;
            long totalNewUsers = 0;
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalGmv = BigDecimal.ZERO;

            for (Map<String, Object> row : entry.getValue()) {
                totalImpressions += toLong(row.get("impressions"));
                totalClicks += toLong(row.get("clicks"));
                totalConversions += toLong(row.get("conversions"));
                totalNewUsers += toLong(row.get("new_users"));
                totalCost = totalCost.add(toBigDecimal(row.get("cost")));
                totalGmv = totalGmv.add(toBigDecimal(row.get("gmv")));
            }

            TrendDTO trend = new TrendDTO();
            trend.setStatDate(entry.getKey());
            trend.setImpressions(totalImpressions);
            trend.setClicks((int) totalClicks);
            trend.setConversions((int) totalConversions);
            trend.setNewUsers((int) totalNewUsers);
            trend.setCost(totalCost);
            trend.setGmv(totalGmv);
            trends.add(trend);
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, trends, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache set failed: {}", e.getMessage());
        }
        return trends;
    }

    @Override
    public List<Map<String, Object>> getChannelDistribution(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        String cacheKey = CACHE_KEY_PREFIX + "channel:" + startDate + ":" + endDate;
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping cache: {}", e.getMessage());
        }

        List<Map<String, Object>> distribution = statsHourlyMapper.channelDistribution(
                null, startDate, endDate);

        try {
            redisTemplate.opsForValue().set(cacheKey, distribution, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache set failed: {}", e.getMessage());
        }
        return distribution;
    }

    @Override
    public List<Map<String, Object>> getMaterialTop(LocalDate startDate, LocalDate endDate, int limit) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        String cacheKey = CACHE_KEY_PREFIX + "material_top:" + startDate + ":" + endDate + ":limit=" + limit;
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping cache: {}", e.getMessage());
        }

        List<Map<String, Object>> top = statsHourlyMapper.materialTop(limit, startDate, endDate);

        try {
            redisTemplate.opsForValue().set(cacheKey, top, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache set failed: {}", e.getMessage());
        }
        return top;
    }

    private LocalDate extractLocalDate(Object obj) {
        if (obj instanceof LocalDate) return (LocalDate) obj;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        return LocalDate.parse(obj.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
