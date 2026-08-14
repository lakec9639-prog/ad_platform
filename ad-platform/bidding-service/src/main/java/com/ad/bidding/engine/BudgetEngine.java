package com.ad.bidding.engine;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BudgetEngine {

    private final ConcurrentHashMap<Long, BigDecimal> budgets = new ConcurrentHashMap<>();

    public BudgetEngine() {
        budgets.put(1L, new BigDecimal("5000.00"));
        budgets.put(2L, new BigDecimal("3000.00"));
        budgets.put(3L, new BigDecimal("4000.00"));
        budgets.put(4L, new BigDecimal("3000.00"));
        budgets.put(5L, new BigDecimal("5000.00"));
    }

    public boolean hasBudget(Long campaignId) {
        BigDecimal remaining = budgets.get(campaignId);
        return remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal deduct(Long campaignId, BigDecimal cost) {
        BigDecimal newBudget = budgets.computeIfPresent(campaignId, (id, remaining) -> {
            BigDecimal after = remaining.subtract(cost);
            return after.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : after;
        });
        if (newBudget != null && newBudget.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Budget exhausted for campaign {}", campaignId);
        }
        return newBudget;
    }

    public void resetDaily() {
        budgets.put(1L, new BigDecimal("5000.00"));
        budgets.put(2L, new BigDecimal("3000.00"));
        budgets.put(3L, new BigDecimal("4000.00"));
        budgets.put(4L, new BigDecimal("3000.00"));
        budgets.put(5L, new BigDecimal("5000.00"));
    }
}
