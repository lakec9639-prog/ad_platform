package com.ad.service;

import com.ad.dto.AdSlotCreateDTO;
import com.ad.dto.AdSlotDTO;
import java.util.List;

public interface AdSlotService {
    List<AdSlotDTO> listAll(Long publisherId);
    AdSlotDTO getById(Long id);
    Long create(AdSlotCreateDTO dto);
    void update(Long id, AdSlotCreateDTO dto);
}
