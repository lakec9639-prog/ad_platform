package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ad_material")
public class Material extends BaseEntity {

    private String name;
    private String code;
    private String type;
    private Integer duration;
    private Integer status;
    private Integer score;
}
