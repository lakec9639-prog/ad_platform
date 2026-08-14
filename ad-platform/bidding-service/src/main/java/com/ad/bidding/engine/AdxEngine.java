package com.ad.bidding.engine;

import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.BidResponse;
import com.ad.bidding.stats.MetricsCollector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class AdxEngine {

    private final DspDecisionEngine dspEngine;
    private final MetricsCollector metrics;
    private final AtomicLong bidLogCounter = new AtomicLong(0);

    public AdxEngine(MetricsCollector metrics) {
        this.metrics = metrics;
        this.dspEngine = new DspDecisionEngine();
    }

    public BidResponse process(BidRequest req) {
        long startNanos = System.nanoTime();

        BigDecimal floorPrice = getFloorPrice(req.getAdSlotId());

        BidResponse response = dspEngine.decide(req, floorPrice);

        long latencyNs = System.nanoTime() - startNanos;
        recordBidLog(req, response, floorPrice, latencyNs / 1_000_000);

        if (response.getStrategyId() != null) {
            metrics.recordBid(response.getStrategyId(), response.isWin(), response.getPrice(), latencyNs);
        }

        return response;
    }

    private BigDecimal getFloorPrice(Long adSlotId) {
        return new BigDecimal("0.01");
    }

    private void recordBidLog(BidRequest req, BidResponse resp, BigDecimal floorPrice, long latencyMs) {
        if (resp.isWin()) {
            log.info("BID_WIN slot={} campaign={} price={} floor={} latency={}ms",
                    req.getAdSlotId(), resp.getCampaignId(), resp.getPrice(), floorPrice, latencyMs);
        } else {
            log.info("BID_LOSE slot={} nbr={} latency={}ms",
                    req.getAdSlotId(), resp.getNbr(), latencyMs);
        }
        bidLogCounter.incrementAndGet();
    }

    public long getTotalBidCount() {
        return bidLogCounter.get();
    }
}
