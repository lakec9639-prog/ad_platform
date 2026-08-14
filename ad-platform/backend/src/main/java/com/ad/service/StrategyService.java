package com.ad.service;

import com.ad.dto.StrategyCreateDTO;
import com.ad.dto.StrategyDTO;

import java.util.List;

public interface StrategyService {

    List<StrategyDTO> listAll();

    StrategyDTO getById(Long id);

    Long create(StrategyCreateDTO dto);

    void update(Long id, StrategyCreateDTO dto);

    void updateStatus(Long id, Integer status);
}
