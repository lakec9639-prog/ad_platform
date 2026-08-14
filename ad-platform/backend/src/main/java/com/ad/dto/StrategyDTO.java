package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class StrategyDTO {

    private Long id;
    private String name;
    private String code;
    private Integer status;
    private String objective;
    private String description;
    private BigDecimal budget;
    private BigDecimal targetCpa;
    private BigDecimal targetCvr;
    private BigDecimal expectedRoas;
    private BigDecimal budgetRatio;
    private Integer sortOrder;
    private List<ChannelAllocation> channelAllocations;
    private List<Long> audienceIds;
    private List<Long> materialIds;
    private BigDecimal currentCost;
    private BigDecimal currentCpa;
    private BigDecimal currentRoas;

    @Getter
    @Setter
    public static class ChannelAllocation {
        private String channel;
        private BigDecimal budgetRatio;
    }
}
