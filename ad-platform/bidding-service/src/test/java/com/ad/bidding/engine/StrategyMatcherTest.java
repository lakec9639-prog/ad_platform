package com.ad.bidding.engine;

import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.CampaignConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class StrategyMatcherTest {

    private StrategyMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new StrategyMatcher();
    }

    @Test
    void shouldMatchHighValueDeviceToS1() {
        BidRequest req = new BidRequest();
        req.setDeviceId("hv-sim-001");

        CampaignConfig result = matcher.match(req, new HashMap<>());

        assertNotNull(result);
        assertEquals(1L, result.getStrategyId()); // S1 high value
    }

    @Test
    void shouldMatchRetargetDeviceToS4() {
        BidRequest req = new BidRequest();
        req.setDeviceId("rt-sim-001");

        CampaignConfig result = matcher.match(req, new HashMap<>());

        assertNotNull(result);
        assertEquals(4L, result.getStrategyId()); // S4 retarget (priority over S1)
    }

    @Test
    void shouldReturnNullForUnknownDevice() {
        BidRequest req = new BidRequest();
        req.setDeviceId("unknown-sim-001");

        CampaignConfig result = matcher.match(req, new HashMap<>());

        assertNull(result); // S6 = no match
    }

    @Test
    void shouldBlockByFrequencyCap() {
        BidRequest req = new BidRequest();
        req.setDeviceId("hv-sim-freq-001");

        HashMap<String, Long> freqMap = new HashMap<>();
        // S1 has frequencyCap=10, so 15 exposures → blocked
        freqMap.put("freq:1:hv-sim-freq-001:" + java.time.LocalDate.now(), 15L);

        CampaignConfig result = matcher.match(req, freqMap);

        // Should skip S1 (frequency capped) and match S2 or S5 as fallback
        assertNull(result);
    }

    @Test
    void shouldMatchWithSufficientFrequencyBudget() {
        BidRequest req = new BidRequest();
        req.setDeviceId("hv-sim-ok-001");

        HashMap<String, Long> freqMap = new HashMap<>();
        // S1 has frequencyCap=10, 5 is within limit
        freqMap.put("freq:1:hv-sim-ok-001:" + java.time.LocalDate.now(), 5L);

        CampaignConfig result = matcher.match(req, freqMap);

        assertNotNull(result);
        assertEquals(1L, result.getStrategyId());
    }
}
