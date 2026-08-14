package com.ad.enums;

import lombok.Getter;

@Getter
public enum Channel {

    DOUYIN("douyin", "抖音"),
    XIAOHONGSHU("xiaohongshu", "小红书"),
    BILIBILI("bilibili", "B站"),
    TENCENT("tencent", "腾讯广告"),
    BAIDU_FEED("baidu_feed", "百度信息流"),
    BAIDU_SEARCH("baidu_search", "百度搜索");

    private final String code;
    private final String label;

    Channel(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Channel fromCode(String code) {
        for (Channel channel : values()) {
            if (channel.code.equals(code)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown channel code: " + code);
    }
}
