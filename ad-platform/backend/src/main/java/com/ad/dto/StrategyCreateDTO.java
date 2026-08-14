package com.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class StrategyCreateDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String code;

    @NotBlank
    private String objective;

    private String description;

    @NotNull
    private BigDecimal budget;

    private BigDecimal targetCpa;
    private BigDecimal targetCvr;
    private BigDecimal expectedRoas;
    private BigDecimal budgetRatio;
    private Integer sortOrder;
    private List<ChannelAllocation> channelAllocations;
    private List<Long> audienceIds;
    private List<Long> materialIds;

    // RTB bidding config
    private BigDecimal bidRate;
    private Integer frequencyCap;
    private String timeRange;
    private List<Long> publisherIds;
    private List<Long> adSlotIds;

    @Getter
    @Setter
    public static class ChannelAllocation {
        private String channel;
        private BigDecimal budgetRatio;
    }
}
