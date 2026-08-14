package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.StrategyCreateDTO;
import com.ad.dto.StrategyDTO;
import com.ad.dto.StrategyStatusDTO;
import com.ad.service.StrategyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;

    @GetMapping
    public Result<List<StrategyDTO>> list() {
        return Result.ok(strategyService.listAll());
    }

    @GetMapping("/{id}")
    public Result<StrategyDTO> getById(@PathVariable Long id) {
        StrategyDTO dto = strategyService.getById(id);
        if (dto == null) {
            return Result.fail("Strategy not found");
        }
        return Result.ok(dto);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody StrategyCreateDTO dto) {
        Long id = strategyService.create(dto);
        return Result.ok(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StrategyCreateDTO dto) {
        strategyService.update(id, dto);
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StrategyStatusDTO dto) {
        strategyService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
