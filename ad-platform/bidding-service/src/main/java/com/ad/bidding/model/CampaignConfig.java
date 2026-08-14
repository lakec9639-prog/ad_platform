package com.ad.bidding.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CampaignConfig {
    private Long id;
    private Long strategyId;
    private String name;
    private String channel;
    private BigDecimal bidPrice;
    private String bidType;
    private BigDecimal budgetDaily;
    private BigDecimal targetCpa;
    private BigDecimal bidRate;
    private List<String> audienceCodes;
    private List<MaterialOption> materials;
    private Integer frequencyCap;
    private String timeRange;

    @Data
    @Builder
    public static class MaterialOption {
        private String code;
        private String url;
        private int width;
        private int height;
        private int priority;
    }
}
