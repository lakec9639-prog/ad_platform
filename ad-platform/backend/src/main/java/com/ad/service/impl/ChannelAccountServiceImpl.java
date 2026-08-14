package com.ad.service.impl;

import com.ad.dto.ChannelAccountCreateDTO;
import com.ad.dto.ChannelAccountDTO;
import com.ad.entity.ChannelAccount;
import com.ad.mapper.ChannelAccountMapper;
import com.ad.service.ChannelAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelAccountServiceImpl implements ChannelAccountService {

    private final ChannelAccountMapper channelAccountMapper;

    @Override
    public List<ChannelAccountDTO> listAll() {
        return channelAccountMapper.selectList(
                new LambdaQueryWrapper<ChannelAccount>().orderByDesc(ChannelAccount::getId)
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ChannelAccountDTO getById(Long id) {
        ChannelAccount a = channelAccountMapper.selectById(id);
        return a == null ? null : toDTO(a);
    }

    @Override
    @Transactional
    public Long create(ChannelAccountCreateDTO dto) {
        ChannelAccount a = new ChannelAccount();
        a.setName(dto.getName());
        a.setChannel(dto.getChannel());
        a.setAppId(dto.getAppId());
        a.setAppSecret(dto.getAppSecret());
        a.setStatus(dto.getStatus());
        channelAccountMapper.insert(a);
        return a.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ChannelAccountCreateDTO dto) {
        ChannelAccount a = channelAccountMapper.selectById(id);
        if (a == null) throw new RuntimeException("Channel account not found: " + id);
        a.setName(dto.getName());
        a.setChannel(dto.getChannel());
        a.setAppId(dto.getAppId());
        a.setAppSecret(dto.getAppSecret());
        a.setStatus(dto.getStatus());
        channelAccountMapper.updateById(a);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        channelAccountMapper.deleteById(id);
    }

    private ChannelAccountDTO toDTO(ChannelAccount a) {
        ChannelAccountDTO dto = new ChannelAccountDTO();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setChannel(a.getChannel());
        dto.setAppId(a.getAppId());
        dto.setAppSecret(a.getAppSecret());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }
}
