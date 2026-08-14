package com.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PublisherCreateDTO {
    @NotBlank private String name;
    @NotBlank private String code;
    private String contact;
    @NotNull private BigDecimal revenueShare;
}
