package com.ad.service;

import com.ad.dto.AudienceDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AudienceService {

    List<AudienceDTO> listAll();

    Long create(AudienceDTO dto);

    Map<String, Object> getStats(Long id, LocalDate startDate, LocalDate endDate);
}
