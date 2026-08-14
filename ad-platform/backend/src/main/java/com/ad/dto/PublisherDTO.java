package com.ad.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PublisherDTO {
    private Long id;
    private String name;
    private String code;
    private String contact;
    private String apiToken;
    private BigDecimal revenueShare;
    private Integer status;
    private LocalDateTime createdAt;
}
