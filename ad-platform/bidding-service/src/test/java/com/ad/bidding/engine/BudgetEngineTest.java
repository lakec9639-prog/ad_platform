package com.ad.bidding.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BudgetEngineTest {

    private BudgetEngine budgetEngine;

    @BeforeEach
    void setUp() {
        budgetEngine = new BudgetEngine();
    }

    @Test
    void shouldHaveBudgetForKnownCampaigns() {
        assertTrue(budgetEngine.hasBudget(1L));
        assertTrue(budgetEngine.hasBudget(2L));
        assertTrue(budgetEngine.hasBudget(3L));
        assertTrue(budgetEngine.hasBudget(4L));
        assertTrue(budgetEngine.hasBudget(5L));
    }

    @Test
    void shouldNotHaveBudgetForUnknownCampaign() {
        assertFalse(budgetEngine.hasBudget(99L));
    }

    @Test
    void shouldDeductBudget() {
        BigDecimal remaining = budgetEngine.deduct(1L, new BigDecimal("500.00"));
        assertEquals(new BigDecimal("4500.00"), remaining);
        assertTrue(budgetEngine.hasBudget(1L));
    }

    @Test
    void shouldFloorBudgetAtZero() {
        budgetEngine.deduct(1L, new BigDecimal("6000.00"));
        BigDecimal remaining = budgetEngine.deduct(1L, new BigDecimal("0.01"));
        assertFalse(budgetEngine.hasBudget(1L));
        assertEquals(BigDecimal.ZERO, remaining);
    }

    @Test
    void shouldReturnNullForUnknownCampaignDeduction() {
        assertNull(budgetEngine.deduct(99L, new BigDecimal("10.00")));
    }

    @Test
    void shouldResetAllBudgets() {
        budgetEngine.deduct(1L, new BigDecimal("5000.00"));
        assertFalse(budgetEngine.hasBudget(1L));

        budgetEngine.resetDaily();
        assertTrue(budgetEngine.hasBudget(1L));
    }
}
