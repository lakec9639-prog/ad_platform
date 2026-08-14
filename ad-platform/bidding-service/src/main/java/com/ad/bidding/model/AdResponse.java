package com.ad.bidding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponse {
    private int code;
    private String msg;
    private String adType;
    private String htmlSnippet;
    private String impUrl;
    private String clickUrl;
    private String landingUrl;
}
