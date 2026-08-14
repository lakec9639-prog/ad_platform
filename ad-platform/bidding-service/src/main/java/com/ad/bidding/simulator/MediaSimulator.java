package com.ad.bidding.simulator;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Media traffic simulator. Sends bid requests at a target QPS, then simulates
 * the full user journey: ad impression → click → conversion for each win.
 *
 * Usage: MediaSimulator [totalRequests] [targetQps]
 *   totalRequests: number of bid requests to send (default 500)
 *   targetQps:     sustained rate (default 50)
 */
@Slf4j
public class MediaSimulator {

    private static final String SSP_URL = "http://localhost:9090/ad/request";
    private static final String AUTH_TOKEN = "demo-token-001";

    private static final String[] DEVICE_PREFIXES = {"hv-", "rt-", "cp-", "new-", "unknown-"};
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8)",
            "Mozilla/5.0 (Linux; Android 13; Samsung Galaxy S23)",
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X)"
    };

    private static final String[] SLOT_CODES = {"SLOT_001", "SLOT_001", "SLOT_001"};
    private static final int[] WIDTHS = {320, 728, 300};
    private static final int[] HEIGHTS = {480, 90, 250};

    public static void main(String[] args) {
        int totalRequests = args.length > 0 ? Integer.parseInt(args[0]) : 500;
        int targetQps = args.length > 1 ? Integer.parseInt(args[1]) : 50;

        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx);
        Random random = new Random();

        AtomicInteger sent = new AtomicInteger(0);
        AtomicInteger wins = new AtomicInteger(0);
        AtomicInteger activeSims = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        int intervalMs = Math.max(1, 1000 / targetQps);

        log.info("=== Media Simulator ===");
        log.info("Target: {} requests @ {} QPS ({}ms interval)", totalRequests, targetQps, intervalMs);
        log.info("Device ratios: hv-(S1) rt-(S4→S1) cp-(通投) new-(通投) unknown-(无匹配)");

        long timerId = vertx.setPeriodic(intervalMs, id -> {
            int count = sent.incrementAndGet();
            if (count > totalRequests) {
                vertx.cancelTimer(id);
                return;
            }

            String prefix = DEVICE_PREFIXES[random.nextInt(DEVICE_PREFIXES.length)];
            String deviceId = prefix + "sim-" + count + "-" + System.nanoTime();
            String ua = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
            int slotIdx = random.nextInt(SLOT_CODES.length);

            JsonObject body = new JsonObject()
                    .put("device_id", deviceId)
                    .put("ip", "192.168." + random.nextInt(256) + "." + random.nextInt(256))
                    .put("ua", ua)
                    .put("ad_slot_code", SLOT_CODES[slotIdx])
                    .put("width", WIDTHS[slotIdx])
                    .put("height", HEIGHTS[slotIdx])
                    .put("app_package", "com.demo.media");

            client.postAbs(SSP_URL)
                    .putHeader("Content-Type", "application/json")
                    .putHeader("X-Auth-Token", AUTH_TOKEN)
                    .sendBuffer(Buffer.buffer(body.encode()))
                    .onSuccess(resp -> {
                        if (resp.statusCode() == 200) {
                            JsonObject result = resp.bodyAsJsonObject();
                            if (result.getInteger("code") == 0) {
                                int w = wins.incrementAndGet();
                                // Simulate user journey for this win
                                activeSims.incrementAndGet();
                                String impUrl = result.getString("imp_url", "");
                                String clickUrl = result.getString("click_url", "");
                                String landingUrl = result.getString("landing_url", "");

                                simulateJourney(vertx, client, deviceId, impUrl, clickUrl, landingUrl,
                                        random, activeSims, count % 5 == 0);
                            }
                        }
                    })
                    .onFailure(err -> {});

            if (count % targetQps == 0 || count >= totalRequests) {
                long elapsed = System.currentTimeMillis() - startTime;
                double actualQps = count / (elapsed / 1000.0);
                log.info("[{}s] sent={} wins={} qps={} active_journeys={}",
                        elapsed / 1000, count, wins.get(), String.format("%.0f", actualQps), activeSims.get());
            }
        });

        // Print final summary after all requests are sent + a grace period for journeys
        vertx.setTimer((totalRequests / targetQps + 30L) * 1000L, id -> {
            vertx.cancelTimer(timerId);
            long elapsed = System.currentTimeMillis() - startTime;
            double actualQps = totalRequests / (elapsed / 1000.0);
            double winRate = (double) wins.get() / totalRequests * 100;
            log.info("");
            log.info("========== SIMULATION COMPLETE ==========");
            log.info("Duration:       {}s", elapsed / 1000);
            log.info("Requests sent:  {}", totalRequests);
            log.info("Wins:           {}", wins.get());
            log.info("Win rate:       {}%", String.format("%.1f", winRate));
            log.info("Average QPS:    {}", String.format("%.0f", actualQps));
            log.info("");
            log.info("Open dashboard: http://localhost:9090/dashboard");
            log.info("Stats API:      http://localhost:9090/stats");
            log.info("=========================================");
            vertx.close();
        });
    }

    private static void simulateJourney(Vertx vertx, WebClient client, String deviceId,
                                         String impUrl, String clickUrl, String landingUrl,
                                         Random random, AtomicInteger activeSims,
                                         boolean doConversion) {
        // Step 1: Impression (100ms-500ms after bid win)
        vertx.setTimer(random.nextInt(400) + 100, id1 -> {
            fireTracking(client, impUrl);
            // Step 2: Click (1s-5s after impression)
            vertx.setTimer(random.nextInt(4000) + 1000, id2 -> {
                fireTracking(client, clickUrl);
                // Step 3: Landing page visit (immediate after click)
                if (landingUrl != null && !landingUrl.isEmpty()) {
                    fireTracking(client, landingUrl.replace("/landing/", "/track/landing/"));
                }
                // Step 4: Conversion (3s-15s after click, ~20% of journeys)
                if (doConversion) {
                    vertx.setTimer(random.nextInt(12000) + 3000, id3 -> {
                        fireTracking(client, "http://localhost:9090/track/conv");
                        activeSims.decrementAndGet();
                    });
                } else {
                    activeSims.decrementAndGet();
                }
            });
        });
    }

    private static void fireTracking(WebClient client, String url) {
        if (url == null || url.isEmpty()) return;
        client.getAbs(url).send()
                .onSuccess(resp -> {})
                .onFailure(err -> {});
    }
}
