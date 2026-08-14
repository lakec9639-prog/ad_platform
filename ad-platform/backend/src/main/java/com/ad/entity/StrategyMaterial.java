package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ad_strategy_material")
public class StrategyMaterial {

    private Long id;
    private Long strategyId;
    private Long materialId;
    private Integer sortOrder;
}
