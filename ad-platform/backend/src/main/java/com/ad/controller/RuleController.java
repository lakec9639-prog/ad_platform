package com.ad.controller;

import com.ad.common.PageResult;
import com.ad.common.Result;
import com.ad.dto.RuleCreateDTO;
import com.ad.dto.RuleDTO;
import com.ad.dto.SandboxTestRequest;
import com.ad.dto.SandboxTestResult;
import com.ad.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public Result<List<RuleDTO>> list() {
        return Result.ok(ruleService.listAll());
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody RuleCreateDTO dto) {
        Long id = ruleService.create(dto);
        return Result.ok(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RuleCreateDTO dto) {
        ruleService.update(id, dto);
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody RuleDTO dto) {
        ruleService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/logs")
    public Result<PageResult<RuleDTO>> getLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(ruleService.getLogs(id, page, size));
    }

    @PostMapping("/{id}/test")
    public Result<SandboxTestResult> simulate(
            @PathVariable Long id,
            @Valid @RequestBody SandboxTestRequest request) {
        SandboxTestResult result = ruleService.simulate(id, request.getStartDate(), request.getEndDate());
        return Result.ok(result);
    }
}
