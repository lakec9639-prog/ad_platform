package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TrendDTO {

    private LocalDate statDate;
    private BigDecimal cost;
    private Integer conversions;
    private Integer newUsers;
    private BigDecimal gmv;
    private Long impressions;
    private Integer clicks;
}
