package com.ad.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaterialDTO {

    private Long id;
    private String name;
    private String code;
    private String type;
    private Integer duration;
    private Integer status;
    private Integer score;
}
