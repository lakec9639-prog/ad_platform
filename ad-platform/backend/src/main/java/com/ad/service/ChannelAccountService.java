package com.ad.service;

import com.ad.dto.ChannelAccountCreateDTO;
import com.ad.dto.ChannelAccountDTO;
import java.util.List;

public interface ChannelAccountService {
    List<ChannelAccountDTO> listAll();
    ChannelAccountDTO getById(Long id);
    Long create(ChannelAccountCreateDTO dto);
    void update(Long id, ChannelAccountCreateDTO dto);
    void delete(Long id);
}
