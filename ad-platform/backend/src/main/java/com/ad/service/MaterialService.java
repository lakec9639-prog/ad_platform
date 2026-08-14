package com.ad.service;

import com.ad.dto.MaterialDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MaterialService {

    List<MaterialDTO> listAll();

    Long create(MaterialDTO dto);

    List<Map<String, Object>> getDecayCurve(Long id, LocalDate startDate, LocalDate endDate);
}
