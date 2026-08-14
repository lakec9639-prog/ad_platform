package com.ad.service;

import com.ad.common.PageResult;
import com.ad.dto.CampaignCreateDTO;
import com.ad.dto.CampaignDTO;

import java.util.List;

public interface CampaignService {

    PageResult<CampaignDTO> list(Long strategyId, String channel, String keyword, int page, int size);

    CampaignDTO getById(Long id);

    Long create(CampaignCreateDTO dto);

    void update(Long id, CampaignCreateDTO dto);

    void delete(Long id);

    void updateStatus(Long id, Integer status);

    void batchUpdateStatus(List<Long> ids, Integer status);
}
