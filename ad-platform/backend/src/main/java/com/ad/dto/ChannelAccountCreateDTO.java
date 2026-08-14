package com.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelAccountCreateDTO {
    @NotBlank
    private String name;
    @NotBlank
    private String channel;
    private String appId;
    private String appSecret;
    @NotNull
    private Integer status;
}
