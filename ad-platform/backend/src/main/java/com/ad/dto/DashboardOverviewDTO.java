package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DashboardOverviewDTO {

    private BigDecimal totalCost;
    private BigDecimal totalNewUsers;
    private BigDecimal totalConversions;
    private BigDecimal totalGmv;
    private BigDecimal totalImpressions;
    private Integer totalClicks;
    private BigDecimal cpa;
    private BigDecimal roas;
    private BigDecimal ctr;
    private BigDecimal cvr;
    private BigDecimal budgetTotal;
    private BigDecimal budgetProgress;
    private BigDecimal budgetRemaining;
}
