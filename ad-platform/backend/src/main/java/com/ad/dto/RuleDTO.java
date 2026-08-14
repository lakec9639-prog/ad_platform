package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleDTO {

    private Long id;
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
