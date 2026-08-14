package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_publisher")
public class Publisher extends BaseEntity {
    private String name;
    private String code;
    private String contact;
    private String apiToken;
    private BigDecimal revenueShare;
    private Integer status;
}
