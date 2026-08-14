package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ad_audience")
public class Audience extends BaseEntity {

    private String name;
    private String code;
    private String source;
    private Long sizeEstimate;
    private Integer status;
}
