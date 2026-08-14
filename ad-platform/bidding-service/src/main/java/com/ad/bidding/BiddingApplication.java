package com.ad.bidding;

import com.ad.bidding.verticle.MainVerticle;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BiddingApplication {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> log.info("Bidding Service started, deployment id: {}", id))
                .onFailure(err -> {
                    log.error("Failed to start Bidding Service", err);
                    System.exit(1);
                });
    }
}
