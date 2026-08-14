package com.ad.bidding.engine;

import com.ad.bidding.model.*;

import java.math.BigDecimal;
import java.util.*;

public class DspDecisionEngine {

    private final StrategyMatcher strategyMatcher;
    private final Pricer pricer;
    private final BudgetEngine budgetEngine;

    public DspDecisionEngine() {
        this.strategyMatcher = new StrategyMatcher();
        this.pricer = new Pricer();
        this.budgetEngine = new BudgetEngine();
    }

    public BidResponse decide(BidRequest req, BigDecimal floorPrice) {
        long startNanos = System.nanoTime();

        Map<String, Long> freqMap = new HashMap<>();
        CampaignConfig matched = strategyMatcher.match(req, freqMap);

        if (matched == null) {
            return BidResponse.builder()
                    .win(false)
                    .price(BigDecimal.ZERO)
                    .nbr(2)
                    .build();
        }

        if (!budgetEngine.hasBudget(matched.getId())) {
            return BidResponse.builder()
                    .win(false)
                    .price(BigDecimal.ZERO)
                    .nbr(1)
                    .strategyId(matched.getStrategyId())
                    .campaignId(matched.getId())
                    .build();
        }

        BigDecimal bidPrice = pricer.calculateBid(matched);

        if (bidPrice.compareTo(floorPrice) < 0) {
            return BidResponse.builder()
                    .win(false)
                    .price(bidPrice)
                    .nbr(3)
                    .strategyId(matched.getStrategyId())
                    .campaignId(matched.getId())
                    .build();
        }

        budgetEngine.deduct(matched.getId(), bidPrice);

        CampaignConfig.MaterialOption material = selectMaterial(matched, req.getWidth(), req.getHeight());

        String baseTrackUrl = "http://localhost:9090/track";
        String deviceId = req.getDeviceId() != null ? req.getDeviceId() : req.getOaid();

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        return BidResponse.builder()
                .win(true)
                .price(bidPrice)
                .adMaterialUrl(material.getUrl())
                .landingUrl("https://lumi.example.com/product?utm_source=adx&cid=" + matched.getId())
                .trackImpUrl(baseTrackUrl + "/imp/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId)
                .trackClickUrl(baseTrackUrl + "/click/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId)
                .nbr(0)
                .strategyId(matched.getStrategyId())
                .campaignId(matched.getId())
                .impTrackers(Arrays.asList(baseTrackUrl + "/imp/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId))
                .clickTrackers(Arrays.asList(baseTrackUrl + "/click/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId))
                .latencyMs((int) elapsedMs)
                .build();
    }

    private CampaignConfig.MaterialOption selectMaterial(CampaignConfig campaign, int width, int height) {
        if (campaign.getMaterials() == null || campaign.getMaterials().isEmpty()) {
            return CampaignConfig.MaterialOption.builder()
                    .code("default").url("https://cdn.adx.com/materials/default.jpg")
                    .width(width).height(height).priority(0).build();
        }
        return campaign.getMaterials().get(0);
    }

    public void recordEvent(String deviceId, Long campaignId, String eventType) {
    }
}
