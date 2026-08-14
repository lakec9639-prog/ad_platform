package com.ad.service.impl;

import com.ad.dto.StrategyCreateDTO;
import com.ad.dto.StrategyDTO;
import com.ad.entity.Strategy;
import com.ad.entity.StrategyAudience;
import com.ad.entity.StrategyChannel;
import com.ad.entity.StrategyMaterial;
import com.ad.enums.StrategyStatus;
import com.ad.mapper.StrategyAudienceMapper;
import com.ad.mapper.StrategyChannelMapper;
import com.ad.mapper.StrategyMapper;
import com.ad.mapper.StrategyMaterialMapper;
import com.ad.mapper.StatsHourlyMapper;
import com.ad.service.StrategyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StrategyServiceImpl implements StrategyService {

    private final StrategyMapper strategyMapper;
    private final StrategyChannelMapper strategyChannelMapper;
    private final StrategyAudienceMapper strategyAudienceMapper;
    private final StrategyMaterialMapper strategyMaterialMapper;
    private final StatsHourlyMapper statsHourlyMapper;

    private static final int DEFAULT_STATS_DAYS = 30;

    @Override
    public List<StrategyDTO> listAll() {
        List<Strategy> strategies = strategyMapper.selectList(
                new LambdaQueryWrapper<Strategy>()
                        .orderByAsc(Strategy::getSortOrder)
        );
        LocalDate startDate = LocalDate.now().minusDays(DEFAULT_STATS_DAYS);
        LocalDate endDate = LocalDate.now();

        return strategies.stream().map(s -> buildDTO(s, startDate, endDate)).collect(Collectors.toList());
    }

    @Override
    public StrategyDTO getById(Long id) {
        Strategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            return null;
        }
        LocalDate startDate = LocalDate.now().minusDays(DEFAULT_STATS_DAYS);
        LocalDate endDate = LocalDate.now();
        return buildDTO(strategy, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StrategyCreateDTO dto) {
        Strategy strategy = new Strategy();
        strategy.setName(dto.getName());
        strategy.setCode(dto.getCode());
        strategy.setStatus(StrategyStatus.DRAFT.getCode());
        strategy.setObjective(dto.getObjective());
        strategy.setDescription(dto.getDescription());
        strategy.setBudget(dto.getBudget());
        strategy.setTargetCpa(dto.getTargetCpa());
        strategy.setTargetCvr(dto.getTargetCvr());
        strategy.setExpectedRoas(dto.getExpectedRoas());
        strategy.setBudgetRatio(dto.getBudgetRatio());
        strategy.setSortOrder(dto.getSortOrder());
        strategyMapper.insert(strategy);

        insertRelations(strategy.getId(), dto);
        return strategy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StrategyCreateDTO dto) {
        Strategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new RuntimeException("Strategy not found: " + id);
        }

        strategy.setName(dto.getName());
        strategy.setCode(dto.getCode());
        strategy.setObjective(dto.getObjective());
        strategy.setDescription(dto.getDescription());
        strategy.setBudget(dto.getBudget());
        strategy.setTargetCpa(dto.getTargetCpa());
        strategy.setTargetCvr(dto.getTargetCvr());
        strategy.setExpectedRoas(dto.getExpectedRoas());
        strategy.setBudgetRatio(dto.getBudgetRatio());
        strategy.setSortOrder(dto.getSortOrder());
        strategyMapper.updateById(strategy);

        // Delete old relations
        strategyChannelMapper.delete(new LambdaQueryWrapper<StrategyChannel>()
                .eq(StrategyChannel::getStrategyId, id));
        strategyAudienceMapper.delete(new LambdaQueryWrapper<StrategyAudience>()
                .eq(StrategyAudience::getStrategyId, id));
        strategyMaterialMapper.delete(new LambdaQueryWrapper<StrategyMaterial>()
                .eq(StrategyMaterial::getStrategyId, id));

        // Re-insert relations
        insertRelations(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Strategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new RuntimeException("Strategy not found: " + id);
        }

        // Validate status transition
        int currentCode = strategy.getStatus();
        StrategyStatus targetStatus = StrategyStatus.fromCode(status);

        if (currentCode == StrategyStatus.ENDED.getCode()) {
            throw new RuntimeException("Cannot change status of an ended strategy");
        }
        if (currentCode == StrategyStatus.DRAFT.getCode()
                && targetStatus.getCode() == StrategyStatus.PAUSED.getCode()) {
            throw new RuntimeException("Cannot pause a draft strategy, please activate first");
        }

        strategy.setStatus(status);
        strategyMapper.updateById(strategy);
    }

    private void insertRelations(Long strategyId, StrategyCreateDTO dto) {
        // Insert channels
        if (dto.getChannelAllocations() != null) {
            for (StrategyCreateDTO.ChannelAllocation ca : dto.getChannelAllocations()) {
                StrategyChannel sc = new StrategyChannel();
                sc.setStrategyId(strategyId);
                sc.setChannel(ca.getChannel());
                sc.setBudgetRatio(ca.getBudgetRatio());
                strategyChannelMapper.insert(sc);
            }
        }

        // Insert audiences
        if (dto.getAudienceIds() != null) {
            for (Long audienceId : dto.getAudienceIds()) {
                StrategyAudience sa = new StrategyAudience();
                sa.setStrategyId(strategyId);
                sa.setAudienceId(audienceId);
                strategyAudienceMapper.insert(sa);
            }
        }

        // Insert materials
        if (dto.getMaterialIds() != null) {
            for (Long materialId : dto.getMaterialIds()) {
                StrategyMaterial sm = new StrategyMaterial();
                sm.setStrategyId(strategyId);
                sm.setMaterialId(materialId);
                strategyMaterialMapper.insert(sm);
            }
        }
    }

    private StrategyDTO buildDTO(Strategy strategy, LocalDate startDate, LocalDate endDate) {
        StrategyDTO dto = new StrategyDTO();
        dto.setId(strategy.getId());
        dto.setName(strategy.getName());
        dto.setCode(strategy.getCode());
        dto.setStatus(strategy.getStatus());
        dto.setObjective(strategy.getObjective());
        dto.setDescription(strategy.getDescription());
        dto.setBudget(strategy.getBudget());
        dto.setTargetCpa(strategy.getTargetCpa());
        dto.setTargetCvr(strategy.getTargetCvr());
        dto.setExpectedRoas(strategy.getExpectedRoas());
        dto.setBudgetRatio(strategy.getBudgetRatio());
        dto.setSortOrder(strategy.getSortOrder());

        // Load relations
        List<StrategyChannel> channels = strategyChannelMapper.selectList(
                new LambdaQueryWrapper<StrategyChannel>()
                        .eq(StrategyChannel::getStrategyId, strategy.getId()));
        if (channels != null && !channels.isEmpty()) {
            dto.setChannelAllocations(channels.stream().map(sc -> {
                StrategyDTO.ChannelAllocation ca = new StrategyDTO.ChannelAllocation();
                ca.setChannel(sc.getChannel());
                ca.setBudgetRatio(sc.getBudgetRatio());
                return ca;
            }).collect(Collectors.toList()));
        } else {
            dto.setChannelAllocations(Collections.emptyList());
        }

        List<StrategyAudience> audiences = strategyAudienceMapper.selectList(
                new LambdaQueryWrapper<StrategyAudience>()
                        .eq(StrategyAudience::getStrategyId, strategy.getId()));
        if (audiences != null && !audiences.isEmpty()) {
            dto.setAudienceIds(audiences.stream()
                    .map(StrategyAudience::getAudienceId)
                    .collect(Collectors.toList()));
        } else {
            dto.setAudienceIds(Collections.emptyList());
        }

        List<StrategyMaterial> materials = strategyMaterialMapper.selectList(
                new LambdaQueryWrapper<StrategyMaterial>()
                        .eq(StrategyMaterial::getStrategyId, strategy.getId()));
        if (materials != null && !materials.isEmpty()) {
            dto.setMaterialIds(materials.stream()
                    .map(StrategyMaterial::getMaterialId)
                    .collect(Collectors.toList()));
        } else {
            dto.setMaterialIds(Collections.emptyList());
        }

        // Query aggregated stats
        Map<String, Object> stats = queryStats(strategy.getId(), null, null, startDate, endDate);
        BigDecimal totalCost = toBigDecimal(stats.get("total_cost"));
        BigDecimal totalConversions = toBigDecimal(stats.get("total_conversions"));

        dto.setCurrentCost(totalCost);
        if (totalConversions.compareTo(BigDecimal.ZERO) > 0) {
            dto.setCurrentCpa(totalCost.divide(totalConversions, 2, RoundingMode.HALF_UP));
        } else {
            dto.setCurrentCpa(BigDecimal.ZERO);
        }

        BigDecimal totalGmv = toBigDecimal(stats.get("total_gmv"));
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            dto.setCurrentRoas(totalGmv.divide(totalCost, 2, RoundingMode.HALF_UP));
        } else {
            dto.setCurrentRoas(BigDecimal.ZERO);
        }

        return dto;
    }

    private Map<String, Object> queryStats(Long strategyId, Long campaignId, String channel,
                                            LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> results = statsHourlyMapper.sumByDateRange(
                strategyId, campaignId, channel, startDate, endDate);
        return results.isEmpty() ? Collections.emptyMap() : results.get(0);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
