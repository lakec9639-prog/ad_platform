package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.AudienceDTO;
import com.ad.service.AudienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audiences")
@RequiredArgsConstructor
public class AudienceController {

    private final AudienceService audienceService;

    @GetMapping
    public Result<List<AudienceDTO>> list() {
        return Result.ok(audienceService.listAll());
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody AudienceDTO dto) {
        Long id = audienceService.create(dto);
        return Result.ok(id);
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getStats(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.ok(audienceService.getStats(id, startDate, endDate));
    }
}
