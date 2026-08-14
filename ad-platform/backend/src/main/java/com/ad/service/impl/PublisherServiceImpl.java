package com.ad.service.impl;

import com.ad.dto.PublisherCreateDTO;
import com.ad.dto.PublisherDTO;
import com.ad.entity.Publisher;
import com.ad.mapper.PublisherMapper;
import com.ad.service.PublisherService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {
    private final PublisherMapper publisherMapper;

    @Override
    public List<PublisherDTO> listAll() {
        return publisherMapper.selectList(
                new LambdaQueryWrapper<Publisher>().orderByDesc(Publisher::getId)
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public PublisherDTO getById(Long id) {
        Publisher p = publisherMapper.selectById(id);
        return p == null ? null : toDTO(p);
    }

    @Override
    @Transactional
    public Long create(PublisherCreateDTO dto) {
        Publisher p = new Publisher();
        p.setName(dto.getName());
        p.setCode(dto.getCode());
        p.setContact(dto.getContact());
        p.setApiToken(UUID.randomUUID().toString().replace("-", ""));
        p.setRevenueShare(dto.getRevenueShare());
        p.setStatus(1);
        publisherMapper.insert(p);
        return p.getId();
    }

    @Override
    @Transactional
    public void update(Long id, PublisherCreateDTO dto) {
        Publisher p = publisherMapper.selectById(id);
        if (p == null) throw new RuntimeException("Publisher not found: " + id);
        p.setName(dto.getName());
        p.setCode(dto.getCode());
        p.setContact(dto.getContact());
        p.setRevenueShare(dto.getRevenueShare());
        publisherMapper.updateById(p);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        publisherMapper.deleteById(id);
    }

    private PublisherDTO toDTO(Publisher p) {
        PublisherDTO dto = new PublisherDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCode(p.getCode());
        dto.setContact(p.getContact());
        dto.setApiToken(p.getApiToken());
        dto.setRevenueShare(p.getRevenueShare());
        dto.setStatus(p.getStatus());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
