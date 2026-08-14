package com.ad.controller;

import com.ad.common.PageResult;
import com.ad.common.Result;
import com.ad.dto.BatchStatusDTO;
import com.ad.dto.CampaignCreateDTO;
import com.ad.dto.CampaignDTO;
import com.ad.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    public Result<PageResult<CampaignDTO>> list(
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(campaignService.list(strategyId, channel, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<CampaignDTO> getById(@PathVariable Long id) {
        CampaignDTO dto = campaignService.getById(id);
        if (dto == null) {
            return Result.fail("Campaign not found");
        }
        return Result.ok(dto);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CampaignCreateDTO dto) {
        Long id = campaignService.create(dto);
        return Result.ok(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CampaignCreateDTO dto) {
        campaignService.update(id, dto);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return Result.ok(null);
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        campaignService.updateStatus(id, body.getOrDefault("status", 0));
        return Result.ok(null);
    }

    @PatchMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@Valid @RequestBody BatchStatusDTO dto) {
        campaignService.batchUpdateStatus(dto.getIds(), dto.getStatus());
        return Result.ok(null);
    }
}
