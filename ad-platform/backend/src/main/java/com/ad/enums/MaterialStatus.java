package com.ad.enums;

import lombok.Getter;

@Getter
public enum MaterialStatus {

    PENDING(0, "审核中"),
    ACTIVE(1, "生效中"),
    DECAYING(2, "衰退中"),
    STOPPED(3, "已停止");

    private final int code;
    private final String label;

    MaterialStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
