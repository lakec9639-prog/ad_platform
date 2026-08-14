package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Getter
@Setter
@TableName("ad_stats_hourly")
public class StatsHourly {

    private Long id;
    private String channel;
    private Long strategyId;
    private Long campaignId;
    private LocalDate statDate;
    private Integer statHour;
    private Long impressions;
    private Long clicks;
    private Long microConversions;
    private Long conversions;
    private Long newUsers;
    private BigDecimal cost;
    private BigDecimal gmv;
}
