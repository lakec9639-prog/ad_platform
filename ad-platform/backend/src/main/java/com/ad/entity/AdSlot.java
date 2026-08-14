package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_ad_slot")
public class AdSlot extends BaseEntity {
    private Long publisherId;
    private String name;
    private String code;
    private Integer slotType;
    private Integer width;
    private Integer height;
    private BigDecimal floorPrice;
    private String blockCategory;
    private Integer status;
}
