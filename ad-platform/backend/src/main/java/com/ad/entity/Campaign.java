package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ad_campaign")
public class Campaign extends BaseEntity {

    private Long strategyId;
    private String name;
    private String channel;
    private String platformCampaignId;
    private BigDecimal budgetDaily;
    private BigDecimal bidPrice;
    private String bidType;
    private Integer status;
    private LocalDateTime launchAt;
    private LocalDateTime stopAt;
}
