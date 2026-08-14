package com.ad.bidding.handler;

import com.ad.bidding.engine.AdxEngine;
import com.ad.bidding.model.AdResponse;
import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.BidResponse;
import com.ad.bidding.stats.MetricsCollector;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
public class SspHandler implements Handler<RoutingContext> {

    private static final long VALID_AD_SLOT_ID = 1L;
    private static final long VALID_PUBLISHER_ID = 1L;
    private static final String VALID_TOKEN = "demo-token-001";
    private static final BigDecimal FLOOR_PRICE = new BigDecimal("0.01");
    private final AdxEngine adxEngine;
    private final MetricsCollector metrics;

    public SspHandler(MetricsCollector metrics) {
        this.metrics = metrics;
        this.adxEngine = new AdxEngine(metrics);
    }

    @Override
    public void handle(RoutingContext ctx) {
        long startNano = System.nanoTime();
        metrics.recordBidRequest();

        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            respondNoAd(ctx, 1, "invalid request body");
            return;
        }

        BidRequest req = parseRequest(body);
        if (req == null) {
            respondNoAd(ctx, 1, "missing required fields");
            return;
        }

        String token = ctx.request().getHeader("X-Auth-Token");
        if (!VALID_TOKEN.equals(token)) {
            respondNoAd(ctx, 2, "invalid auth token");
            return;
        }

        if (req.getAdSlotId() != VALID_AD_SLOT_ID) {
            respondNoAd(ctx, 3, "unknown ad slot");
            return;
        }

        enrichRequest(req);
        BidResponse bidResponse = callAdx(req);

        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
        log.info("SSP request processed: slot={}, win={}, elapsed={}ms",
                req.getAdSlotCode(), bidResponse.isWin(), elapsedMs);

        if (!bidResponse.isWin()) {
            respondNoAd(ctx, 0, "no bid");
            return;
        }

        String htmlSnippet = String.format(
                "<a href=\"%s\" target=\"_blank\">" +
                "<img src=\"%s\" style=\"width:%dpx;height:%dpx\"/>" +
                "<img src=\"%s\" style=\"display:none\" />" +
                "</a>",
                bidResponse.getLandingUrl(),
                bidResponse.getAdMaterialUrl(),
                req.getWidth(), req.getHeight(),
                bidResponse.getTrackImpUrl()
        );

        AdResponse adResp = AdResponse.builder()
                .code(0)
                .msg("ok")
                .adType("html")
                .htmlSnippet(htmlSnippet)
                .impUrl(bidResponse.getTrackImpUrl())
                .clickUrl(bidResponse.getTrackClickUrl())
                .landingUrl(bidResponse.getLandingUrl())
                .build();

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(JsonObject.mapFrom(adResp).encode());
    }

    private BidRequest parseRequest(JsonObject body) {
        try {
            BidRequest req = new BidRequest();
            req.setDeviceId(body.getString("device_id"));
            req.setOaid(body.getString("oaid"));
            req.setIp(body.getString("ip"));
            req.setUa(body.getString("ua"));
            req.setAdSlotCode(body.getString("ad_slot_code"));
            req.setWidth(body.getInteger("width", 0));
            req.setHeight(body.getInteger("height", 0));
            req.setAppPackage(body.getString("app_package"));
            req.setTimestamp(Instant.now().toEpochMilli());
            req.setAdSlotId(VALID_AD_SLOT_ID);
            req.setPublisherId(VALID_PUBLISHER_ID);

            if (req.getDeviceId() == null && req.getOaid() == null) return null;
            if (req.getAdSlotCode() == null) return null;
            return req;
        } catch (Exception e) {
            log.warn("Failed to parse bid request: {}", e.getMessage());
            return null;
        }
    }

    private void enrichRequest(BidRequest req) {
        String ip = req.getIp();
        if (ip != null) {
            if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                req.setGeo("internal");
            } else {
                req.setGeo("unknown");
            }
        }

        String ua = req.getUa();
        if (ua != null) {
            String uaLower = ua.toLowerCase();
            if (uaLower.contains("iphone") || uaLower.contains("ipad")) {
                req.setOs("iOS");
                req.setDeviceType("mobile");
            } else if (uaLower.contains("android")) {
                req.setOs("Android");
                req.setDeviceType("mobile");
            } else {
                req.setOs("unknown");
                req.setDeviceType("desktop");
            }
        }
    }

    private BidResponse callAdx(BidRequest req) {
        return adxEngine.process(req);
    }

    private void respondNoAd(RoutingContext ctx, int code, String msg) {
        AdResponse resp = AdResponse.builder()
                .code(code)
                .msg(msg)
                .build();
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(JsonObject.mapFrom(resp).encode());
    }
}
