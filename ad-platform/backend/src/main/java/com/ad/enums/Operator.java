package com.ad.enums;

import lombok.Getter;

@Getter
public enum Operator {

    GT("gt", "大于"),
    LT("lt", "小于"),
    GTE("gte", "大于等于"),
    LTE("lte", "小于等于");

    private final String code;
    private final String label;

    Operator(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
