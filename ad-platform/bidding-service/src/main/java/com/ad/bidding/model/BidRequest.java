package com.ad.bidding.model;

import lombok.Data;

@Data
public class BidRequest {
    private String deviceId;
    private String oaid;
    private String ip;
    private String ua;
    private String adSlotCode;
    private int width;
    private int height;
    private String appPackage;
    private long timestamp;

    private String geo;
    private String deviceType;
    private String os;
    private long adSlotId;
    private long publisherId;
}
