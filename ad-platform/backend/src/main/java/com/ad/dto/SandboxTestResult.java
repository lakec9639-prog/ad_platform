package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SandboxTestResult {

    private Integer triggerCount;
    private Integer affectedCampaignCount;
    private BigDecimal estimatedBudgetSaved;
    private List<SandboxTrigger> triggers;

    @Getter
    @Setter
    public static class SandboxTrigger {
        private LocalDate date;
        private Long campaignId;
        private BigDecimal triggerValue;
        private String actionDescription;
    }
}
