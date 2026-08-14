package com.ad.bidding.stats;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {

    private final ConcurrentHashMap<Long, StrategyMetrics> strategyMetrics = new ConcurrentHashMap<>();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger totalBids = new AtomicInteger(0);
    private final AtomicInteger totalWins = new AtomicInteger(0);
    private final AtomicLong totalLatencyNs = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    public void recordBidRequest() {
        totalRequests.incrementAndGet();
    }

    public void recordBid(Long strategyId, boolean win, BigDecimal price, long latencyNs) {
        totalBids.incrementAndGet();
        totalLatencyNs.addAndGet(latencyNs);
        if (win) {
            totalWins.incrementAndGet();
        }
        strategyMetrics.computeIfAbsent(strategyId, k -> new StrategyMetrics(strategyId))
                .recordBid(win, price, latencyNs);
    }

    public void recordImpression(Long strategyId) {
        strategyMetrics.computeIfAbsent(strategyId, k -> new StrategyMetrics(strategyId))
                .recordImpression();
    }

    public void recordClick(Long strategyId) {
        strategyMetrics.computeIfAbsent(strategyId, k -> new StrategyMetrics(strategyId))
                .recordClick();
    }

    public void recordConversion(Long strategyId) {
        strategyMetrics.computeIfAbsent(strategyId, k -> new StrategyMetrics(strategyId))
                .recordConversion();
    }

    public StatsSnapshot snapshot() {
        StatsSnapshot snap = new StatsSnapshot();
        snap.elapsedSec = (System.currentTimeMillis() - startTime) / 1000;
        snap.totalRequests = totalRequests.get();
        snap.totalBids = totalBids.get();
        snap.totalWins = totalWins.get();
        snap.totalLatencyMs = totalLatencyNs.get() / 1_000_000;
        snap.winRate = snap.totalBids > 0 ? (double) snap.totalWins / snap.totalBids * 100 : 0;
        snap.avgLatencyMs = snap.totalBids > 0 ? (double) snap.totalLatencyMs / snap.totalBids : 0;
        snap.qps = snap.elapsedSec > 0 ? (double) snap.totalRequests / snap.elapsedSec : 0;

        for (Map.Entry<Long, StrategyMetrics> entry : strategyMetrics.entrySet()) {
            snap.strategies.put(entry.getKey(), entry.getValue().snapshot());
        }
        return snap;
    }

    public static class StatsSnapshot {
        public long elapsedSec;
        public int totalRequests;
        public int totalBids;
        public int totalWins;
        public long totalLatencyMs;
        public double winRate;
        public double avgLatencyMs;
        public double qps;
        public ConcurrentHashMap<Long, StrategyMetricsSnapshot> strategies = new ConcurrentHashMap<>();
    }

    public static class StrategyMetricsSnapshot {
        public long strategyId;
        public int bids;
        public int wins;
        public int impressions;
        public int clicks;
        public int conversions;
        public double totalSpend;
        public double avgLatencyMs;
        public double winRate;
        public double ctr;
        public double cvr;

        StrategyMetricsSnapshot(long strategyId) {
            this.strategyId = strategyId;
        }
    }

    private static class StrategyMetrics {
        private final long strategyId;
        private final AtomicInteger bids = new AtomicInteger(0);
        private final AtomicInteger wins = new AtomicInteger(0);
        private final AtomicInteger impressions = new AtomicInteger(0);
        private final AtomicInteger clicks = new AtomicInteger(0);
        private final AtomicInteger conversions = new AtomicInteger(0);
        private final AtomicLong totalLatencyNs = new AtomicLong(0);
        private final AtomicLong totalSpendMicro = new AtomicLong(0);

        StrategyMetrics(long strategyId) {
            this.strategyId = strategyId;
        }

        void recordBid(boolean win, BigDecimal price, long latencyNs) {
            bids.incrementAndGet();
            totalLatencyNs.addAndGet(latencyNs);
            if (win) {
                wins.incrementAndGet();
                totalSpendMicro.addAndGet(price.multiply(new BigDecimal("1000000")).longValue());
            }
        }

        void recordImpression() { impressions.incrementAndGet(); }
        void recordClick() { clicks.incrementAndGet(); }
        void recordConversion() { conversions.incrementAndGet(); }

        StrategyMetricsSnapshot snapshot() {
            StrategyMetricsSnapshot s = new StrategyMetricsSnapshot(strategyId);
            s.bids = bids.get();
            s.wins = wins.get();
            s.impressions = impressions.get();
            s.clicks = clicks.get();
            s.conversions = conversions.get();
            s.totalSpend = totalSpendMicro.get() / 1_000_000.0;
            s.winRate = s.bids > 0 ? (double) s.wins / s.bids * 100 : 0;
            s.avgLatencyMs = s.bids > 0 ? totalLatencyNs.get() / 1_000_000.0 / s.bids : 0;
            s.ctr = s.impressions > 0 ? (double) s.clicks / s.impressions * 100 : 0;
            s.cvr = s.clicks > 0 ? (double) s.conversions / s.clicks * 100 : 0;
            return s;
        }
    }
}
