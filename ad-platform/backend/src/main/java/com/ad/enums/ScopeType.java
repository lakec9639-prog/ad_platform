package com.ad.enums;

import lombok.Getter;

@Getter
public enum ScopeType {

    STRATEGY("strategy", "策略"),
    CHANNEL("channel", "渠道"),
    CAMPAIGN("campaign", "计划");

    private final String code;
    private final String label;

    ScopeType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
