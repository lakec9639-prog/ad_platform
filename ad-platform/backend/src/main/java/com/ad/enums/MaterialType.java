package com.ad.enums;

import lombok.Getter;

@Getter
public enum MaterialType {

    VIDEO("video", "视频"),
    IMAGE("image", "图片"),
    IMAGE_TEXT("image_text", "图文");

    private final String code;
    private final String label;

    MaterialType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
