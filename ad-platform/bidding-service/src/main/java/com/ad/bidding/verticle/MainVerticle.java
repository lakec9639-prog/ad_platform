package com.ad.bidding.verticle;

import com.ad.bidding.config.RedisClientFactory;
import com.ad.bidding.handler.DashboardHandler;
import com.ad.bidding.handler.SspHandler;
import com.ad.bidding.handler.StatsHandler;
import com.ad.bidding.handler.TrackingHandler;
import com.ad.bidding.stats.MetricsCollector;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.redis.client.Redis;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainVerticle extends AbstractVerticle {

    private HttpServer server;
    private Redis redisClient;
    private JsonObject config;

    @Override
    public void start(Promise<Void> startPromise) {
        ConfigRetriever.create(vertx)
                .getConfig()
                .onSuccess(cfg -> {
                    this.config = cfg;
                    MetricsCollector metrics = new MetricsCollector();
                    initRedis(cfg).onSuccess(v -> startHttpServer(cfg, metrics, startPromise));
                })
                .onFailure(startPromise::fail);
    }

    private Future<Void> initRedis(JsonObject cfg) {
        Promise<Void> promise = Promise.promise();
        JsonObject redisCfg = cfg.getJsonObject("redis", new JsonObject());
        redisClient = RedisClientFactory.createClient(
                vertx,
                redisCfg.getString("host", "localhost"),
                redisCfg.getInteger("port", 6379),
                redisCfg.getString("password", "")
        );
        redisClient.connect()
                .onSuccess(conn -> {
                    log.info("Redis connected");
                    promise.complete();
                })
                .onFailure(err -> {
                    log.warn("Redis connection failed (non-fatal): {}", err.getMessage());
                    promise.complete();
                });
        return promise.future();
    }

    private void startHttpServer(JsonObject cfg, MetricsCollector metrics, Promise<Void> startPromise) {
        int port = cfg.getJsonObject("server", new JsonObject()).getInteger("port", 9090);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Health check
        router.get("/health").handler(ctx -> {
            ctx.json(new JsonObject().put("status", "UP"));
        });

        // SSP Gateway
        router.post("/ad/request").handler(new SspHandler(metrics));

        // Tracking routes
        TrackingHandler trackingHandler = new TrackingHandler(metrics);
        router.get("/track/imp/:campaignId/:strategyId/:deviceId").handler(trackingHandler);
        router.get("/track/click/:campaignId/:strategyId/:deviceId").handler(trackingHandler);
        router.post("/track/conv").handler(trackingHandler);
        router.get("/track/landing/:campaignId/:strategyId/:deviceId").handler(trackingHandler);

        // Stats API & Dashboard
        router.get("/stats").handler(new StatsHandler(metrics));
        router.get("/dashboard").handler(new DashboardHandler());

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(s -> {
                    server = s;
                    log.info("Bidding Service HTTP server started on port {}", port);
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.close();
        }
        if (redisClient != null) {
            redisClient.close();
        }
    }

    public Redis getRedisClient() {
        return redisClient;
    }

    public JsonObject getAppConfig() {
        return config;
    }
}
