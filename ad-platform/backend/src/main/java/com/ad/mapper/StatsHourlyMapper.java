package com.ad.mapper;

import com.ad.entity.StatsHourly;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsHourlyMapper extends BaseMapper<StatsHourly> {

    List<Map<String, Object>> sumByDateRange(
            @Param("strategyId") Long strategyId,
            @Param("campaignId") Long campaignId,
            @Param("channel") String channel,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> dailyTrends(
            @Param("strategyId") Long strategyId,
            @Param("campaignId") Long campaignId,
            @Param("channel") String channel,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> channelDistribution(
            @Param("strategyId") Long strategyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> materialTop(
            @Param("limit") Integer limit,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> strategyPerformance(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
