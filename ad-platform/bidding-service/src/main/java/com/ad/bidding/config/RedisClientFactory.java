package com.ad.bidding.config;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;

public class RedisClientFactory {

    public static Redis createClient(Vertx vertx, String host, int port, String password) {
        RedisOptions options = new RedisOptions()
                .setConnectionString("redis://" + host + ":" + port);
        if (password != null && !password.isEmpty()) {
            options.setPassword(password);
        }
        return Redis.createClient(vertx, options);
    }
}
