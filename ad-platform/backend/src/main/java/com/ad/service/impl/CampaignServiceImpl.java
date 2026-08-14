package com.ad.service.impl;

import com.ad.common.PageResult;
import com.ad.dto.CampaignCreateDTO;
import com.ad.dto.CampaignDTO;
import com.ad.entity.Campaign;
import com.ad.entity.Strategy;
import com.ad.mapper.CampaignMapper;
import com.ad.mapper.StatsHourlyMapper;
import com.ad.mapper.StrategyMapper;
import com.ad.service.CampaignService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignMapper campaignMapper;
    private final StrategyMapper strategyMapper;
    private final StatsHourlyMapper statsHourlyMapper;

    private static final int DEFAULT_STATS_DAYS = 30;

    @Override
    public PageResult<CampaignDTO> list(Long strategyId, String channel, String keyword, int page, int size) {
        LambdaQueryWrapper<Campaign> wrapper = new LambdaQueryWrapper<Campaign>()
                .eq(strategyId != null, Campaign::getStrategyId, strategyId)
                .eq(channel != null && !channel.isEmpty(), Campaign::getChannel, channel)
                .like(keyword != null && !keyword.isEmpty(), Campaign::getName, keyword)
                .orderByDesc(Campaign::getId);

        IPage<Campaign> pageResult = campaignMapper.selectPage(new Page<>(page, size), wrapper);

        LocalDate startDate = LocalDate.now().minusDays(DEFAULT_STATS_DAYS);
        LocalDate endDate = LocalDate.now();

        List<CampaignDTO> dtoList = pageResult.getRecords().stream()
                .map(c -> buildDTO(c, startDate, endDate))
                .collect(Collectors.toList());

        return PageResult.of(
                new Page<CampaignDTO>()
                        .setRecords(dtoList)
                        .setTotal(pageResult.getTotal())
                        .setCurrent(pageResult.getCurrent())
                        .setSize(pageResult.getSize())
        );
    }

    @Override
    public CampaignDTO getById(Long id) {
        Campaign campaign = campaignMapper.selectById(id);
        if (campaign == null) {
            return null;
        }
        LocalDate startDate = LocalDate.now().minusDays(DEFAULT_STATS_DAYS);
        LocalDate endDate = LocalDate.now();
        return buildDTO(campaign, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CampaignCreateDTO dto) {
        Campaign campaign = new Campaign();
        campaign.setStrategyId(dto.getStrategyId());
        campaign.setName(dto.getName());
        campaign.setChannel(dto.getChannel());
        campaign.setPlatformCampaignId(dto.getPlatformCampaignId());
        campaign.setBudgetDaily(dto.getBudgetDaily());
        campaign.setBidType(dto.getBidType());
        campaign.setBidPrice(dto.getBidPrice());
        campaign.setLaunchAt(dto.getLaunchAt());
        campaign.setStopAt(dto.getStopAt());
        campaign.setStatus(0); // DRAFT
        campaignMapper.insert(campaign);
        return campaign.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CampaignCreateDTO dto) {
        Campaign campaign = campaignMapper.selectById(id);
        if (campaign == null) {
            throw new RuntimeException("Campaign not found: " + id);
        }
        campaign.setStrategyId(dto.getStrategyId());
        campaign.setName(dto.getName());
        campaign.setChannel(dto.getChannel());
        campaign.setPlatformCampaignId(dto.getPlatformCampaignId());
        campaign.setBudgetDaily(dto.getBudgetDaily());
        campaign.setBidType(dto.getBidType());
        campaign.setBidPrice(dto.getBidPrice());
        campaign.setLaunchAt(dto.getLaunchAt());
        campaign.setStopAt(dto.getStopAt());
        campaignMapper.updateById(campaign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        campaignMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Campaign campaign = campaignMapper.selectById(id);
        if (campaign == null) {
            throw new RuntimeException("Campaign not found: " + id);
        }
        campaign.setStatus(status);
        campaignMapper.updateById(campaign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        campaignMapper.updateBatchStatus(ids, status);
    }

    private CampaignDTO buildDTO(Campaign campaign, LocalDate startDate, LocalDate endDate) {
        CampaignDTO dto = new CampaignDTO();
        dto.setId(campaign.getId());
        dto.setStrategyId(campaign.getStrategyId());
        dto.setName(campaign.getName());
        dto.setChannel(campaign.getChannel());
        dto.setPlatformCampaignId(campaign.getPlatformCampaignId());
        dto.setBudgetDaily(campaign.getBudgetDaily());
        dto.setBidPrice(campaign.getBidPrice());
        dto.setBidType(campaign.getBidType());
        dto.setStatus(campaign.getStatus());
        dto.setLaunchAt(campaign.getLaunchAt());
        dto.setStopAt(campaign.getStopAt());

        // Look up strategy name
        Strategy strategy = strategyMapper.selectById(campaign.getStrategyId());
        dto.setStrategyName(strategy != null ? strategy.getName() : null);

        // Query aggregated stats for this campaign
        List<Map<String, Object>> statsList = statsHourlyMapper.sumByDateRange(
                null, campaign.getId(), null, startDate, endDate);
        if (!statsList.isEmpty()) {
            Map<String, Object> stats = statsList.get(0);
            BigDecimal totalCost = toBigDecimal(stats.get("total_cost"));
            BigDecimal totalConversions = toBigDecimal(stats.get("total_conversions"));
            BigDecimal totalGmv = toBigDecimal(stats.get("total_gmv"));
            Long totalConversionsLong = toLong(stats.get("total_conversions"));

            dto.setCurrentCost(totalCost);
            dto.setCurrentConversions(totalConversionsLong != null ? totalConversionsLong.intValue() : 0);

            if (totalConversions.compareTo(BigDecimal.ZERO) > 0) {
                dto.setCurrentCpa(totalCost.divide(totalConversions, 2, RoundingMode.HALF_UP));
            } else {
                dto.setCurrentCpa(BigDecimal.ZERO);
            }

            if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
                dto.setCurrentRoas(totalGmv.divide(totalCost, 2, RoundingMode.HALF_UP));
            } else {
                dto.setCurrentRoas(BigDecimal.ZERO);
            }
        }

        return dto;
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

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
