package com.ad.bidding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private boolean win;
    private BigDecimal price;
    private String adMaterialUrl;
    private String landingUrl;
    private String trackImpUrl;
    private String trackClickUrl;
    private int nbr;
    private Long strategyId;
    private Long campaignId;
    private List<String> impTrackers;
    private List<String> clickTrackers;
    private int latencyMs;
}
