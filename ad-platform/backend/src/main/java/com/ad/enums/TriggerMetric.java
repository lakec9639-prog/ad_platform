package com.ad.enums;

import lombok.Getter;

@Getter
public enum TriggerMetric {

    CPA("cpa", "CPA"),
    CTR("ctr", "CTR"),
    CVR("cvr", "CVR"),
    CONSUME("consume", "消耗");

    private final String code;
    private final String label;

    TriggerMetric(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
