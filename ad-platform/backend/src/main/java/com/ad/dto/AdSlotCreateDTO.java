package com.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdSlotCreateDTO {
    @NotNull private Long publisherId;
    @NotBlank private String name;
    @NotBlank private String code;
    @NotNull private Integer slotType;
    @NotNull private Integer width;
    @NotNull private Integer height;
    private BigDecimal floorPrice;
    private String blockCategory;
}
