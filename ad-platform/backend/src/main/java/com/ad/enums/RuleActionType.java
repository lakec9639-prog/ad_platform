package com.ad.enums;

import lombok.Getter;

@Getter
public enum RuleActionType {

    PAUSE_CAMPAIGN("pause_campaign", "暂停计划"),
    ACTIVATE("activate", "启用计划"),
    RAISE_BID("raise_bid", "提价"),
    LOWER_BID("lower_bid", "降价"),
    SWAP_MATERIAL("swap_material", "替换素材"),
    ADJUST_BUDGET("adjust_budget", "调整预算"),
    SEND_ALERT("send_alert", "发送告警");

    private final String code;
    private final String label;

    RuleActionType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
