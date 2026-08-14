package com.ad.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdSlotDTO {
    private Long id;
    private Long publisherId;
    private String publisherName;
    private String name;
    private String code;
    private Integer slotType;
    private Integer width;
    private Integer height;
    private BigDecimal floorPrice;
    private String blockCategory;
    private Integer status;
    private LocalDateTime createdAt;
}
