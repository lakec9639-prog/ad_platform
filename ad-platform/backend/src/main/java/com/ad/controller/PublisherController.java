package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.PublisherCreateDTO;
import com.ad.dto.PublisherDTO;
import com.ad.service.PublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/publishers")
@RequiredArgsConstructor
public class PublisherController {
    private final PublisherService publisherService;

    @GetMapping
    public Result<List<PublisherDTO>> list() {
        return Result.ok(publisherService.listAll());
    }

    @GetMapping("/{id}")
    public Result<PublisherDTO> getById(@PathVariable Long id) {
        PublisherDTO dto = publisherService.getById(id);
        return dto == null ? Result.fail("Publisher not found") : Result.ok(dto);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PublisherCreateDTO dto) {
        return Result.ok(publisherService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PublisherCreateDTO dto) {
        publisherService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        publisherService.delete(id);
        return Result.ok();
    }
}
