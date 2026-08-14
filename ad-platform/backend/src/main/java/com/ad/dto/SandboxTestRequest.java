package com.ad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SandboxTestRequest {

    @NotNull
    private Long ruleId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
