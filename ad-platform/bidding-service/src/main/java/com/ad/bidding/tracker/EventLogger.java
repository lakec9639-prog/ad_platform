package com.ad.bidding.tracker;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class EventLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void logEvent(String type, Long campaignId, Long strategyId, String deviceId) {
        String now = LocalDateTime.now().format(FORMATTER);
        String line = String.format("%s\t%s\t%d\t%d\t%s",
                now, type, campaignId, strategyId, deviceId != null ? deviceId : "unknown");
        log.info("TRACK|{}", line);
    }
}
