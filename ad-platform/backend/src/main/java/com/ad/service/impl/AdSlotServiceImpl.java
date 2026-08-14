package com.ad.service.impl;

import com.ad.dto.AdSlotCreateDTO;
import com.ad.dto.AdSlotDTO;
import com.ad.entity.AdSlot;
import com.ad.entity.Publisher;
import com.ad.mapper.AdSlotMapper;
import com.ad.mapper.PublisherMapper;
import com.ad.service.AdSlotService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdSlotServiceImpl implements AdSlotService {
    private final AdSlotMapper adSlotMapper;
    private final PublisherMapper publisherMapper;

    @Override
    public List<AdSlotDTO> listAll(Long publisherId) {
        return adSlotMapper.selectList(
                new LambdaQueryWrapper<AdSlot>()
                        .eq(publisherId != null, AdSlot::getPublisherId, publisherId)
                        .orderByDesc(AdSlot::getId)
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AdSlotDTO getById(Long id) {
        AdSlot slot = adSlotMapper.selectById(id);
        return slot == null ? null : toDTO(slot);
    }

    @Override
    @Transactional
    public Long create(AdSlotCreateDTO dto) {
        AdSlot slot = new AdSlot();
        slot.setPublisherId(dto.getPublisherId());
        slot.setName(dto.getName());
        slot.setCode(dto.getCode());
        slot.setSlotType(dto.getSlotType());
        slot.setWidth(dto.getWidth());
        slot.setHeight(dto.getHeight());
        slot.setFloorPrice(dto.getFloorPrice());
        slot.setBlockCategory(dto.getBlockCategory());
        slot.setStatus(1);
        adSlotMapper.insert(slot);
        return slot.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AdSlotCreateDTO dto) {
        AdSlot slot = adSlotMapper.selectById(id);
        if (slot == null) throw new RuntimeException("AdSlot not found: " + id);
        slot.setPublisherId(dto.getPublisherId());
        slot.setName(dto.getName());
        slot.setCode(dto.getCode());
        slot.setSlotType(dto.getSlotType());
        slot.setWidth(dto.getWidth());
        slot.setHeight(dto.getHeight());
        slot.setFloorPrice(dto.getFloorPrice());
        slot.setBlockCategory(dto.getBlockCategory());
        adSlotMapper.updateById(slot);
    }

    private AdSlotDTO toDTO(AdSlot slot) {
        AdSlotDTO dto = new AdSlotDTO();
        dto.setId(slot.getId());
        dto.setPublisherId(slot.getPublisherId());
        dto.setName(slot.getName());
        dto.setCode(slot.getCode());
        dto.setSlotType(slot.getSlotType());
        dto.setWidth(slot.getWidth());
        dto.setHeight(slot.getHeight());
        dto.setFloorPrice(slot.getFloorPrice());
        dto.setBlockCategory(slot.getBlockCategory());
        dto.setStatus(slot.getStatus());
        dto.setCreatedAt(slot.getCreatedAt());

        Publisher p = publisherMapper.selectById(slot.getPublisherId());
        dto.setPublisherName(p != null ? p.getName() : null);
        return dto;
    }
}
