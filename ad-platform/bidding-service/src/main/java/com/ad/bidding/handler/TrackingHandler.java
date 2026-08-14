package com.ad.bidding.handler;

import com.ad.bidding.stats.MetricsCollector;
import com.ad.bidding.tracker.EventLogger;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TrackingHandler implements Handler<RoutingContext> {

    private static final Buffer PIXEL_GIF = createPixelGif();
    private final EventLogger eventLogger = new EventLogger();
    private final MetricsCollector metrics;

    public TrackingHandler(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String path = ctx.normalizedPath();
        String type;

        if (path.contains("/imp/")) {
            type = "impression";
        } else if (path.contains("/click/")) {
            type = "click";
        } else if (path.contains("/conv")) {
            type = "conversion";
        } else if (path.contains("/landing/")) {
            type = "landing";
        } else {
            ctx.response().setStatusCode(404).end();
            return;
        }

        String campaignId = ctx.pathParam("campaignId");
        String strategyId = ctx.pathParam("strategyId");
        String deviceId = ctx.pathParam("deviceId");

        long sid = strategyId != null ? Long.parseLong(strategyId) : 0;
        long cid = campaignId != null ? Long.parseLong(campaignId) : 0;

        eventLogger.logEvent(type, cid, sid, deviceId);

        switch (type) {
            case "impression" -> metrics.recordImpression(sid);
            case "click" -> metrics.recordClick(sid);
            case "conversion" -> metrics.recordConversion(sid);
        }

        if ("landing".equals(type)) {
            ctx.response()
                    .setStatusCode(302)
                    .putHeader("Location", "https://lumi.example.com/product?utm_source=adx&track=1")
                    .end();
        } else {
            ctx.response()
                    .putHeader("Content-Type", "image/gif")
                    .putHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .putHeader("Pragma", "no-cache")
                    .putHeader("Expires", "0")
                    .end(PIXEL_GIF);
        }
    }

    private static Buffer createPixelGif() {
        byte[] gif = {
                (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61,
                (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                (byte) 0x80, (byte) 0x00, (byte) 0x00,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x44, (byte) 0x01, (byte) 0x00, (byte) 0x3B
        };
        return Buffer.buffer(gif);
    }
}
