package com.ad.enums;

import lombok.Getter;

@Getter
public enum AudienceSource {

    DMP("dmp", "DMP人群"),
    LOOKALIKE("lookalike", "Lookalike相似人群"),
    RETARGET("retarget", "Retarget重定向");

    private final String code;
    private final String label;

    AudienceSource(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
