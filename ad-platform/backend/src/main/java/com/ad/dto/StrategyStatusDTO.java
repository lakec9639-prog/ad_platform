package com.ad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StrategyStatusDTO {

    @NotNull
    private Integer status;
}
