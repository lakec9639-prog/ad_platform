package com.ad.service.impl;

import com.ad.dto.MaterialDTO;
import com.ad.entity.Material;
import com.ad.entity.StrategyMaterial;
import com.ad.mapper.MaterialMapper;
import com.ad.mapper.StatsHourlyMapper;
import com.ad.mapper.StrategyMaterialMapper;
import com.ad.service.MaterialService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialMapper materialMapper;
    private final StatsHourlyMapper statsHourlyMapper;
    private final StrategyMaterialMapper strategyMaterialMapper;

    @Override
    public List<MaterialDTO> listAll() {
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>()
                        .orderByDesc(Material::getId)
        );
        List<MaterialDTO> dtos = new ArrayList<>();
        for (Material m : materials) {
            dtos.add(toDTO(m));
        }
        return dtos;
    }

    @Override
    public Long create(MaterialDTO dto) {
        Material material = new Material();
        material.setName(dto.getName());
        material.setCode(dto.getCode());
        material.setType(dto.getType());
        material.setDuration(dto.getDuration());
        material.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        material.setScore(dto.getScore());
        materialMapper.insert(material);
        return material.getId();
    }

    @Override
    public List<Map<String, Object>> getDecayCurve(Long id, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        // Find which strategies use this material
        List<StrategyMaterial> links = strategyMaterialMapper.selectList(
                new LambdaQueryWrapper<StrategyMaterial>()
                        .eq(StrategyMaterial::getMaterialId, id));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> strategyIds = links.stream()
                .map(StrategyMaterial::getStrategyId)
                .distinct()
                .collect(Collectors.toList());

        // For each strategy, query daily trends and accumulate by date
        Map<LocalDate, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Long sid : strategyIds) {
            List<Map<String, Object>> rawData = statsHourlyMapper.dailyTrends(
                    sid, null, null, startDate, endDate);
            for (Map<String, Object> row : rawData) {
                Object dateObj = row.get("stat_date");
                if (dateObj == null) continue;
                LocalDate date;
                if (dateObj instanceof java.sql.Date) {
                    date = ((java.sql.Date) dateObj).toLocalDate();
                } else {
                    date = (LocalDate) dateObj;
                }
                grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(row);
            }
        }

        // Aggregate by date and compute CTR/CVR/CPA
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Map<String, Object>>> entry : grouped.entrySet()) {
            long totalImpressions = 0;
            long totalClicks = 0;
            long totalConversions = 0;
            BigDecimal totalCost = BigDecimal.ZERO;

            for (Map<String, Object> row : entry.getValue()) {
                totalImpressions += toLong(row.get("impressions"));
                totalClicks += toLong(row.get("clicks"));
                totalConversions += toLong(row.get("conversions"));
                totalCost = totalCost.add(toBigDecimal(row.get("cost")));
            }

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("statDate", entry.getKey().toString());
            dayData.put("impressions", totalImpressions);
            dayData.put("clicks", totalClicks);
            dayData.put("conversions", totalConversions);
            dayData.put("cost", totalCost);

            BigDecimal ctr = totalImpressions > 0
                    ? BigDecimal.valueOf(totalClicks * 100.0 / totalImpressions).setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal cvr = totalClicks > 0
                    ? BigDecimal.valueOf(totalConversions * 100.0 / totalClicks).setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal cpa = totalConversions > 0
                    ? totalCost.divide(BigDecimal.valueOf(totalConversions), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            dayData.put("ctr", ctr);
            dayData.put("cvr", cvr);
            dayData.put("cpa", cpa);
            result.add(dayData);
        }

        // Sort by date ascending
        result.sort(Comparator.comparing(m -> m.get("statDate").toString()));
        return result;
    }

    private MaterialDTO toDTO(Material material) {
        MaterialDTO dto = new MaterialDTO();
        dto.setId(material.getId());
        dto.setName(material.getName());
        dto.setCode(material.getCode());
        dto.setType(material.getType());
        dto.setDuration(material.getDuration());
        dto.setStatus(material.getStatus());
        dto.setScore(material.getScore());
        return dto;
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
