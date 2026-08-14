package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_channel_account")
public class ChannelAccount extends BaseEntity {
    private String name;
    private String channel;
    private String appId;
    private String appSecret;
    private Integer status;
}
