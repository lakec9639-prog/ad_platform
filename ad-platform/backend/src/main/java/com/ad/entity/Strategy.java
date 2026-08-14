package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("ad_strategy")
public class Strategy extends BaseEntity {

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

    // RTB bidding config
    private java.math.BigDecimal bidRate;
    private Integer frequencyCap;
    private String timeRange;
    private Integer rtbStatus;
}
