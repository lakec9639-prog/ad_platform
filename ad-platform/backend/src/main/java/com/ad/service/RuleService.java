package com.ad.service;

import com.ad.common.PageResult;
import com.ad.dto.RuleCreateDTO;
import com.ad.dto.RuleDTO;
import com.ad.dto.SandboxTestResult;

import java.time.LocalDate;
import java.util.List;

public interface RuleService {

    List<RuleDTO> listAll();

    Long create(RuleCreateDTO dto);

    void update(Long id, RuleCreateDTO dto);

    void updateStatus(Long id, Integer status);

    void delete(Long id);

    SandboxTestResult simulate(Long id, LocalDate startDate, LocalDate endDate);

    PageResult<RuleDTO> getLogs(Long ruleId, int page, int size);
}
