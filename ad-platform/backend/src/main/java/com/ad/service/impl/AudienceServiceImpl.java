package com.ad.service.impl;

import com.ad.dto.AudienceDTO;
import com.ad.entity.Audience;
import com.ad.mapper.AudienceMapper;
import com.ad.service.AudienceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudienceServiceImpl implements AudienceService {

    private final AudienceMapper audienceMapper;

    @Override
    public List<AudienceDTO> listAll() {
        List<Audience> audiences = audienceMapper.selectList(
                new LambdaQueryWrapper<Audience>()
                        .orderByDesc(Audience::getId)
        );
        return audiences.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Long create(AudienceDTO dto) {
        Audience audience = new Audience();
        audience.setName(dto.getName());
        audience.setCode(dto.getCode());
        audience.setSource(dto.getSource());
        audience.setSizeEstimate(dto.getSizeEstimate());
        audience.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        audienceMapper.insert(audience);
        return audience.getId();
    }

    @Override
    public Map<String, Object> getStats(Long id, LocalDate startDate, LocalDate endDate) {
        // Return placeholder stats for audience — real integration would query
        // platform APIs for reachable population, overlap, etc.
        Map<String, Object> stats = new HashMap<>();
        stats.put("audienceId", id);
        stats.put("startDate", startDate != null ? startDate.toString() : null);
        stats.put("endDate", endDate != null ? endDate.toString() : null);
        stats.put("reachableEstimate", "N/A");
        stats.put("overlapRate", "N/A");
        return stats;
    }

    private AudienceDTO toDTO(Audience audience) {
        AudienceDTO dto = new AudienceDTO();
        dto.setId(audience.getId());
        dto.setName(audience.getName());
        dto.setCode(audience.getCode());
        dto.setSource(audience.getSource());
        dto.setSizeEstimate(audience.getSizeEstimate());
        dto.setStatus(audience.getStatus());
        return dto;
    }
}
