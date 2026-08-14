package com.ad.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChannelAccountDTO {
    private Long id;
    private String name;
    private String channel;
    private String appId;
    private String appSecret;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
