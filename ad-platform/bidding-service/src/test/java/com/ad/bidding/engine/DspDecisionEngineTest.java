package com.ad.bidding.engine;

import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.BidResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DspDecisionEngineTest {

    private DspDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DspDecisionEngine();
    }

    @Test
    void shouldReturnWinForHighValueDevice() {
        BidRequest req = new BidRequest();
        req.setDeviceId("hv-sim-win-001");
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("0.01"));

        assertTrue(resp.isWin());
        assertEquals(0, resp.getNbr());
        assertNotNull(resp.getCampaignId());
        assertNotNull(resp.getAdMaterialUrl());
        assertNotNull(resp.getTrackImpUrl());
        assertNotNull(resp.getTrackClickUrl());
        assertTrue(resp.getLatencyMs() >= 0);
    }

    @Test
    void shouldReturnNoBidForUnknownDevice() {
        BidRequest req = new BidRequest();
        req.setDeviceId("unknown-sim-nobid-001");
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("0.01"));

        assertFalse(resp.isWin());
        assertEquals(2, resp.getNbr()); // nbr=2 = no match
    }

    @Test
    void shouldReturnFloorPriceFailure() {
        BidRequest req = new BidRequest();
        req.setDeviceId("hv-sim-floor-001");
        req.setWidth(320);
        req.setHeight(480);

        // Floor price higher than any bid
        BidResponse resp = engine.decide(req, new BigDecimal("99999.00"));

        assertFalse(resp.isWin());
        assertEquals(3, resp.getNbr()); // nbr=3 = below floor
    }

    @Test
    void shouldHandleNewDevicePrefix() {
        BidRequest req = new BidRequest();
        req.setDeviceId("new-sim-001");
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("0.01"));

        assertEquals(0, resp.getNbr());
    }

    @Test
    void shouldHandleCpDevice() {
        BidRequest req = new BidRequest();
        req.setDeviceId("cp-sim-001");
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("0.01"));

        assertEquals(0, resp.getNbr());
    }
}
