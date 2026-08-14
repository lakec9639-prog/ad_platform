package com.ad.bidding.engine;

import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.CampaignConfig;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

@Slf4j
public class StrategyMatcher {

    private final List<CampaignConfig> campaigns;

    public StrategyMatcher() {
        this.campaigns = buildDefaultCampaigns();
    }

    public CampaignConfig match(BidRequest req, Map<String, Long> freqMap) {
        for (CampaignConfig campaign : campaigns) {
            if (matches(req, campaign, freqMap)) {
                log.debug("Matched campaign: {} (strategy {})", campaign.getName(), campaign.getStrategyId());
                return campaign;
            }
        }
        return null;
    }

    private boolean matches(BidRequest req, CampaignConfig campaign, Map<String, Long> freqMap) {
        if (!"ALL".equals(campaign.getChannel()) && !matchesChannel(req, campaign.getChannel())) {
            return false;
        }

        if (campaign.getTimeRange() != null && !isInTimeRange(campaign.getTimeRange())) {
            return false;
        }

        if (campaign.getStrategyId() == 1 && !req.getDeviceId().startsWith("hv-")
                && !req.getDeviceId().startsWith("rt-")) {
            return false;
        }
        if (campaign.getStrategyId() == 4 && !req.getDeviceId().startsWith("rt-")) {
            return false;
        }

        if (campaign.getFrequencyCap() != null && campaign.getFrequencyCap() > 0) {
            Long todayCount = freqMap.getOrDefault(freqKey(req.getDeviceId(), campaign.getId()), 0L);
            if (todayCount >= campaign.getFrequencyCap()) {
                log.debug("Frequency cap hit for device {} on campaign {}", req.getDeviceId(), campaign.getId());
                return false;
            }
        }

        return true;
    }

    private boolean matchesChannel(BidRequest req, String channel) {
        return true;
    }

    private boolean isInTimeRange(String timeRange) {
        try {
            String[] parts = timeRange.split("-");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            LocalTime now = LocalTime.now();
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            return true;
        }
    }

    private String freqKey(String deviceId, Long campaignId) {
        String date = java.time.LocalDate.now().toString();
        return "freq:" + campaignId + ":" + deviceId + ":" + date;
    }

    private List<CampaignConfig> buildDefaultCampaigns() {
        List<CampaignConfig> list = new ArrayList<>();

        list.add(CampaignConfig.builder()
                .id(4L).strategyId(4L).name("弃单重定向强转化")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("200")).bidRate(new BigDecimal("0.6"))
                .frequencyCap(5).timeRange("00:00-23:59")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C006").url("https://cdn.adx.com/materials/c006.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        list.add(CampaignConfig.builder()
                .id(3L).strategyId(3L).name("竞品截流抢夺")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("250")).bidRate(new BigDecimal("0.5"))
                .frequencyCap(8).timeRange("08:00-23:00")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C008").url("https://cdn.adx.com/materials/c008.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        list.add(CampaignConfig.builder()
                .id(1L).strategyId(1L).name("高价值人群精准转化")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("250")).bidRate(new BigDecimal("0.4"))
                .frequencyCap(10).timeRange("09:00-23:00")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C007").url("https://cdn.adx.com/materials/c007.jpg").width(320).height(480).priority(1).build(),
                        CampaignConfig.MaterialOption.builder().code("C002").url("https://cdn.adx.com/materials/c002.jpg").width(320).height(480).priority(2).build()
                ))
                .build());

        list.add(CampaignConfig.builder()
                .id(2L).strategyId(2L).name("新品破圈拉新")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("300")).bidRate(new BigDecimal("0.25"))
                .frequencyCap(5).timeRange("08:00-22:00")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C001").url("https://cdn.adx.com/materials/c001.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        list.add(CampaignConfig.builder()
                .id(5L).strategyId(5L).name("智能通投探索")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("350")).bidRate(new BigDecimal("0.15"))
                .frequencyCap(10).timeRange("00:00-23:59")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C007").url("https://cdn.adx.com/materials/c007.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        return list;
    }
}
