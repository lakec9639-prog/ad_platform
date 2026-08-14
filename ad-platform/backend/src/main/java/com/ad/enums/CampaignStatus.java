package com.ad.enums;

import lombok.Getter;

@Getter
public enum CampaignStatus {

    SETUP(0, "搭建中"),
    RUNNING(1, "投放中"),
    PAUSED(2, "已暂停"),
    STOPPED(3, "已停止");

    private final int code;
    private final String label;

    CampaignStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
