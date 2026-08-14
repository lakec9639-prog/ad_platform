package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ad_strategy_audience")
public class StrategyAudience {

    private Long id;
    private Long strategyId;
    private Long audienceId;
    private String type;
}
