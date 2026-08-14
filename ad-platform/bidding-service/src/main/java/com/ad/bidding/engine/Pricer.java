package com.ad.bidding.engine;

import com.ad.bidding.model.CampaignConfig;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class Pricer {

    public BigDecimal calculateBid(CampaignConfig campaign) {
        if (campaign.getBidRate() == null || campaign.getTargetCpa() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal cpmBid = campaign.getTargetCpa()
                .multiply(campaign.getBidRate())
                .multiply(new BigDecimal("10"))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Bid calculated: campaign={}, targetCPA={}, rate={}, cpmBid={}",
                campaign.getName(), campaign.getTargetCpa(), campaign.getBidRate(), cpmBid);

        return cpmBid;
    }
}
