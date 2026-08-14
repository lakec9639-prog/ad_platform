package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ad_rule_execution_log")
public class RuleExecutionLog {

    private Long id;
    private Long ruleId;
    private Long campaignId;
    private String triggerValue;
    private String actionTaken;
    private String result;
    private String errorMessage;
    private LocalDateTime executedAt;
}
