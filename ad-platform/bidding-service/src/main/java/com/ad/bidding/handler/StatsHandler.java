package com.ad.bidding.handler;

import com.ad.bidding.stats.MetricsCollector;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class StatsHandler implements Handler<RoutingContext> {

    private final MetricsCollector collector;

    public StatsHandler(MetricsCollector collector) {
        this.collector = collector;
    }

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .putHeader("Access-Control-Allow-Origin", "*")
                .end(JsonObject.mapFrom(collector.snapshot()).encode());
    }
}
