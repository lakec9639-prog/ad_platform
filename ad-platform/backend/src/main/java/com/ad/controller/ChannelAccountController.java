package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.ChannelAccountCreateDTO;
import com.ad.dto.ChannelAccountDTO;
import com.ad.service.ChannelAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channel-accounts")
@RequiredArgsConstructor
public class ChannelAccountController {

    private final ChannelAccountService channelAccountService;

    @GetMapping
    public Result<List<ChannelAccountDTO>> list() {
        return Result.ok(channelAccountService.listAll());
    }

    @GetMapping("/{id}")
    public Result<ChannelAccountDTO> getById(@PathVariable Long id) {
        ChannelAccountDTO dto = channelAccountService.getById(id);
        if (dto == null) return Result.fail("Channel account not found");
        return Result.ok(dto);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ChannelAccountCreateDTO dto) {
        Long id = channelAccountService.create(dto);
        return Result.ok(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ChannelAccountCreateDTO dto) {
        channelAccountService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        channelAccountService.delete(id);
        return Result.ok();
    }
}
