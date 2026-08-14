package com.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CampaignCreateDTO {

    @NotNull
    private Long strategyId;

    @NotBlank
    private String name;

    @NotBlank
    private String channel;

    private String platformCampaignId;

    @NotNull
    private BigDecimal budgetDaily;

    private String bidType;
    private BigDecimal bidPrice;
    private LocalDateTime launchAt;
    private LocalDateTime stopAt;
}
