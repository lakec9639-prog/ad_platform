package com.ad.enums;

import lombok.Getter;

@Getter
public enum AudienceStrategyType {

    MAIN("main", "主人群"),
    EXTEND("extend", "扩展人群"),
    EXCLUDE("exclude", "排除人群");

    private final String code;
    private final String label;

    AudienceStrategyType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
