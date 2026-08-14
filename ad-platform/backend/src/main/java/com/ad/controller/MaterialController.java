package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.MaterialDTO;
import com.ad.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public Result<List<MaterialDTO>> list() {
        return Result.ok(materialService.listAll());
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody MaterialDTO dto) {
        Long id = materialService.create(dto);
        return Result.ok(id);
    }

    @GetMapping("/{id}/decay")
    public Result<List<Map<String, Object>>> getDecayCurve(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.ok(materialService.getDecayCurve(id, startDate, endDate));
    }
}
