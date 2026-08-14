package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.AdSlotCreateDTO;
import com.ad.dto.AdSlotDTO;
import com.ad.service.AdSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ad-slots")
@RequiredArgsConstructor
public class AdSlotController {
    private final AdSlotService adSlotService;

    @GetMapping
    public Result<List<AdSlotDTO>> list(@RequestParam(required = false) Long publisherId) {
        return Result.ok(adSlotService.listAll(publisherId));
    }

    @GetMapping("/{id}")
    public Result<AdSlotDTO> getById(@PathVariable Long id) {
        AdSlotDTO dto = adSlotService.getById(id);
        return dto == null ? Result.fail("AdSlot not found") : Result.ok(dto);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdSlotCreateDTO dto) {
        return Result.ok(adSlotService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdSlotCreateDTO dto) {
        adSlotService.update(id, dto);
        return Result.ok();
    }
}
