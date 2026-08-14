package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ad_rule")
public class Rule extends BaseEntity {

    private String name;
    private String triggerMetric;
    private String triggerOperator;
    private String triggerThreshold;
    private Integer triggerWindowHours;
    private String actionType;
    private String actionParams;
    private String scopeType;
    private String scopeValue;
    private Integer priority;
    private Integer cooldownMinutes;
    private Boolean isSystem;
    private Integer status;
}
