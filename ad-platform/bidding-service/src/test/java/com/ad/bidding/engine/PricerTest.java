package com.ad.bidding.engine;

import com.ad.bidding.model.CampaignConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricerTest {

    private final Pricer pricer = new Pricer();

    @Test
    void shouldCalculateBidFromTargetCpaAndBidRate() {
        CampaignConfig campaign = CampaignConfig.builder()
                .name("test").targetCpa(new BigDecimal("200")).bidRate(new BigDecimal("0.5")).build();

        BigDecimal bid = pricer.calculateBid(campaign);

        // 200 * 0.5 * 10 = 1000.00
        assertEquals(new BigDecimal("1000.00"), bid);
    }

    @Test
    void shouldReturnZeroWhenTargetCpaIsNull() {
        CampaignConfig campaign = CampaignConfig.builder()
                .name("test").targetCpa(null).bidRate(new BigDecimal("0.5")).build();

        assertEquals(BigDecimal.ZERO, pricer.calculateBid(campaign));
    }

    @Test
    void shouldReturnZeroWhenBidRateIsNull() {
        CampaignConfig campaign = CampaignConfig.builder()
                .name("test").targetCpa(new BigDecimal("200")).bidRate(null).build();

        assertEquals(BigDecimal.ZERO, pricer.calculateBid(campaign));
    }

    @Test
    void shouldRoundToTwoDecimalPlaces() {
        CampaignConfig campaign = CampaignConfig.builder()
                .name("test").targetCpa(new BigDecimal("33.33")).bidRate(new BigDecimal("0.37")).build();

        BigDecimal bid = pricer.calculateBid(campaign);

        // 33.33 * 0.37 * 10 = 123.321 → 123.32
        assertEquals(new BigDecimal("123.32"), bid);
    }
}
