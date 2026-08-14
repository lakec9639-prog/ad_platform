package com.ad.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum StrategyStatus {

    DRAFT(0, "草稿"),
    ACTIVE(1, "启用"),
    PAUSED(2, "暂停"),
    ENDED(3, "结束");

    private final int code;
    private final String label;

    StrategyStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static StrategyStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown strategy status code: " + code));
    }
}
