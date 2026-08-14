package com.ad.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchStatusDTO {

    @NotEmpty
    private List<Long> ids;

    @NotNull
    private Integer status;
}
