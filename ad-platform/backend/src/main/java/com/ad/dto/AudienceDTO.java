package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AudienceDTO {

    private Long id;
    private String name;
    private String code;
    private String source;
    private Long sizeEstimate;
    private Integer status;
}
