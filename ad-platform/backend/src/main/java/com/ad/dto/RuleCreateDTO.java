package com.ad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleCreateDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String triggerMetric;

    @NotBlank
    private String triggerOperator;

    @NotBlank
    private String triggerThreshold;

    private Integer triggerWindowHours;

    @NotBlank
    private String actionType;

    private String actionParams;

    @NotBlank
    private String scopeType;

    private String scopeValue;
    private Integer priority;
    private Integer cooldownMinutes;
    private Integer status;
}
