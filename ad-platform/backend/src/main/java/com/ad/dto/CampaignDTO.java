package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CampaignDTO {

    private Long id;
    private Long strategyId;
    private String strategyName;
    private String name;
    private String channel;
    private String platformCampaignId;
    private BigDecimal budgetDaily;
    private BigDecimal bidPrice;
    private String bidType;
    private Integer status;
    private LocalDateTime launchAt;
    private LocalDateTime stopAt;
    private BigDecimal currentCost;
    private BigDecimal currentCpa;
    private BigDecimal currentRoas;
    private Integer currentConversions;
}
