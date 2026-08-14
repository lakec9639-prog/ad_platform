package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("ad_strategy_channel")
public class StrategyChannel {

    private Long id;
    private Long strategyId;
    private String channel;
    private BigDecimal budgetRatio;
}
