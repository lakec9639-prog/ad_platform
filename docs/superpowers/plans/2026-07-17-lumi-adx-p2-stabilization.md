# LUMI DSP-ADX-SSP 全链路平台 — Phase 2: 引擎加固与数据闭环

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Phase 1 in-memory configs with Redis-backed data sync, add frequency/budget fuses, build real-time dashboard for RTB metrics, and load test to 500 QPS.

**Architecture:** Two-service architecture with Redis Pub/Sub for real-time config sync. Bidding Service reads campaign/ad-slot configs from Redis (written by Management Service). MySQL remains source of truth. Budget fuse operates at 3 levels. New dashboard module shows real-time RTB metrics alongside existing campaign stats.

**Tech Stack:** Vert.x 4.5 Redis Pub/Sub subscriber, Spring Boot 3.2.x Redis Publisher, WebSocket for real-time dashboard, wrk for load testing

## Global Constraints

- Java 17, Maven 3.9+
- Data Sync: Redis Pub/Sub channel `config:changed`, payload format `action:targetId`
- Budget fuse levels: 80%→bid×0.8, 100%→pause exploratory campaigns, 120%→full stop (all campaigns)
- New Redis keys: `rtb:campaign:<id>`, `rtb:ad_slot:<id>`, `freq:<campaignId>:<deviceId>:<date>`, `budget:<campaignId>:<date>`
- All new SQL migration statements must be idempotent (use `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` or check DDL)
- Dashboard metrics: fill_rate, win_rate, P95_latency, budget_usage%, hourly bid volume
- Bidding Service endpoints remain at port 9090, Management at port 8080

---

### Task 1: Data Sync Module — Redis Config Publisher (Management Service)

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/sync/StrategySyncService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/sync/AdSlotSyncService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/sync/BudgetSyncService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/listener/RedisPubSubListener.java`
- Modify: `ad-platform/backend/src/main/java/com/ad/controller/StrategyDeployController.java` (add bulk deploy)
- Modify: `ad-platform/backend/src/main/java/com/ad/service/impl/AdSlotServiceImpl.java` (publish on update)

**Interfaces:**
- Consumes: existing Publisher, AdSlot, Strategy services and mappers
- Produces: Redis Pub/Sub messages on config changes, Redis Hash writes for Bidding Service to consume

- [ ] **Step 1: Create StrategySyncService.java**

```java
package com.ad.sync;

import com.ad.entity.Strategy;
import com.ad.mapper.StrategyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Syncs active strategy configs from MySQL to Redis.
 * Called on strategy deploy/update and on application startup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategySyncService {

    private final StringRedisTemplate redisTemplate;
    private final StrategyMapper strategyMapper;
    private final ObjectMapper objectMapper;

    private static final String RTB_STRATEGY_KEY_PREFIX = "rtb:strategy:";

    /**
     * Sync all active strategies to Redis.
     */
    public void syncAllActive() {
        List<Strategy> active = strategyMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Strategy>()
                        .eq(Strategy::getStatus, 1)
                        .eq(Strategy::getDeleted, 0)
        );
        for (Strategy s : active) {
            syncOne(s);
        }
        log.info("Synced {} active strategies to Redis", active.size());
    }

    /**
     * Sync a single strategy's RTB config to Redis Hash.
     */
    public void syncOne(Strategy strategy) {
        Map<String, String> config = new HashMap<>();
        config.put("id", String.valueOf(strategy.getId()));
        config.put("name", strategy.getName() != null ? strategy.getName() : "");
        config.put("targetCpa", strategy.getTargetCpa() != null ? strategy.getTargetCpa().toString() : "0");
        config.put("bidRate", strategy.getBidRate() != null ? strategy.getBidRate().toString() : "0.3");
        config.put("frequencyCap", strategy.getFrequencyCap() != null ? String.valueOf(strategy.getFrequencyCap()) : "10");
        config.put("timeRange", strategy.getTimeRange() != null ? strategy.getTimeRange() : "00:00-23:59");

        String key = RTB_STRATEGY_KEY_PREFIX + strategy.getId();
        redisTemplate.opsForHash().putAll(key, config);
        redisTemplate.expire(key, Duration.ofDays(1));

        notifyChange("strategy:deploy:" + strategy.getId());
    }

    /**
     * Remove a strategy config from Redis.
     */
    public void removeOne(Long strategyId) {
        redisTemplate.delete(RTB_STRATEGY_KEY_PREFIX + strategyId);
        notifyChange("strategy:undeploy:" + strategyId);
    }

    private void notifyChange(String payload) {
        redisTemplate.convertAndSend("config:changed", payload);
    }
}
```

- [ ] **Step 2: Create AdSlotSyncService.java**

```java
package com.ad.sync;

import com.ad.entity.AdSlot;
import com.ad.entity.Publisher;
import com.ad.mapper.AdSlotMapper;
import com.ad.mapper.PublisherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Syncs ad slot configs from MySQL to Redis.
 * Bidding Service reads slot config from Redis instead of hardcoding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdSlotSyncService {

    private final StringRedisTemplate redisTemplate;
    private final AdSlotMapper adSlotMapper;
    private final PublisherMapper publisherMapper;

    private static final String RTB_AD_SLOT_KEY_PREFIX = "rtb:ad_slot:";

    public void syncAllActive() {
        List<AdSlot> active = adSlotMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdSlot>()
                        .eq(AdSlot::getStatus, 1)
        );
        for (AdSlot slot : active) {
            syncOne(slot);
        }
        log.info("Synced {} active ad slots to Redis", active.size());
    }

    public void syncOne(AdSlot slot) {
        Publisher publisher = publisherMapper.selectById(slot.getPublisherId());

        Map<String, String> config = new HashMap<>();
        config.put("id", String.valueOf(slot.getId()));
        config.put("publisherId", String.valueOf(slot.getPublisherId()));
        config.put("publisherName", publisher != null ? publisher.getName() : "");
        config.put("name", slot.getName());
        config.put("code", slot.getCode());
        config.put("slotType", String.valueOf(slot.getSlotType()));
        config.put("width", String.valueOf(slot.getWidth()));
        config.put("height", String.valueOf(slot.getHeight()));
        config.put("floorPrice", slot.getFloorPrice() != null ? slot.getFloorPrice().toString() : "0");
        config.put("blockCategory", slot.getBlockCategory() != null ? slot.getBlockCategory() : "");
        config.put("status", String.valueOf(slot.getStatus()));

        String key = RTB_AD_SLOT_KEY_PREFIX + slot.getId();
        redisTemplate.opsForHash().putAll(key, config);
        redisTemplate.expire(key, Duration.ofDays(1));

        redisTemplate.convertAndSend("config:changed", "ad_slot:sync:" + slot.getId());
    }

    public void removeOne(Long slotId) {
        redisTemplate.delete(RTB_AD_SLOT_KEY_PREFIX + slotId);
    }
}
```

- [ ] **Step 3: Create RedisPubSubListener.java**

```java
package com.ad.listener;

import com.ad.sync.StrategySyncService;
import com.ad.sync.AdSlotSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Listens for config change notifications from Bidding Service
 * (and between Management Service modules).
 *
 * Messages on channel `config:changed` follow format: `action:entityId`
 * Example: "strategy:deploy:5" or "ad_slot:sync:3"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPubSubListener implements MessageListener {

    private final StrategySyncService strategySyncService;
    private final AdSlotSyncService adSlotSyncService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.debug("Config change received: {}", body);

        String[] parts = body.split(":");
        if (parts.length < 2) {
            log.warn("Invalid config change message: {}", body);
            return;
        }

        String action = parts[0];
        String target = parts[1];

        switch (action) {
            case "strategy":
                if ("deploy".equals(parts[1]) && parts.length >= 3) {
                    // Strategy was deployed by Bidding Service; re-sync full config
                    strategySyncService.syncAllActive();
                }
                break;
            case "ad_slot":
                adSlotSyncService.syncAllActive();
                break;
            default:
                log.debug("Unknown config change action: {}", action);
        }
    }
}
```

- [ ] **Step 4: Configure Redis Pub/Sub in Management Service**

Add to `application.yml`:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Add Redis message listener config. Create `ad-platform/backend/src/main/java/com/ad/config/RedisPubSubConfig.java`:

```java
package com.ad.config;

import com.ad.listener.RedisPubSubListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final RedisPubSubListener listener;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic("config:changed"));
        return container;
    }
}
```

- [ ] **Step 5: Wire sync into AdSlotServiceImpl**

Modify `AdSlotServiceImpl.java` — inject `AdSlotSyncService` and call `syncOne(...)` after create/update.

```java
// After create, add:
adSlotSyncService.syncOne(savedSlot);

// After update, add:
adSlotSyncService.syncOne(updatedSlot);
```

- [ ] **Step 6: Create BudgetSyncService.java**

```java
package com.ad.sync;

import com.ad.entity.Campaign;
import com.ad.mapper.CampaignMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Syncs daily campaign budgets to Redis for Bidding Service to consume.
 * Budget key: budget:<campaignId>:<date> → remaining amount (string)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetSyncService {

    private final StringRedisTemplate redisTemplate;
    private final CampaignMapper campaignMapper;

    /**
     * Sync daily budgets for all active campaigns.
     * Called on startup and via scheduled task at 00:05 daily.
     */
    public void syncDailyBudgets() {
        String today = LocalDate.now().toString();
        campaignMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Campaign>()
                        .eq(Campaign::getStatus, 1)
        ).forEach(c -> {
            if (c.getBudgetDaily() != null) {
                String key = "budget:" + c.getId() + ":" + today;
                redisTemplate.opsForValue().set(key, c.getBudgetDaily().toString(), Duration.ofDays(2));
            }
        });
        log.info("Synced daily budgets for active campaigns");
    }

    /**
     * Deduct from a campaign's Redis budget.
     * Used when Bidding Service reports a win (Phase 3: bid response reports consumption).
     */
    public BigDecimal deductBudget(Long campaignId, BigDecimal cost) {
        String today = LocalDate.now().toString();
        String key = "budget:" + campaignId + ":" + today;
        String remaining = redisTemplate.opsForValue().get(key);
        if (remaining == null) return BigDecimal.ZERO;

        BigDecimal newVal = new BigDecimal(remaining).subtract(cost);
        if (newVal.compareTo(BigDecimal.ZERO) < 0) newVal = BigDecimal.ZERO;
        redisTemplate.opsForValue().set(key, newVal.toString(), Duration.ofDays(2));

        // Check fuse levels
        Campaign campaign = campaignMapper.selectById(campaignId);
        if (campaign != null && campaign.getBudgetDaily() != null
                && campaign.getBudgetDaily().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usage = BigDecimal.ONE.subtract(
                    newVal.divide(campaign.getBudgetDaily(), 4, BigDecimal.ROUND_HALF_UP));
            double usagePct = usage.doubleValue();

            if (usagePct >= 1.20) {
                redisTemplate.convertAndSend("budget:fuse",
                        "full_stop:" + campaignId + ":120");
                log.warn("BUDGET FUSE 120%: campaign {} fully stopped", campaignId);
            } else if (usagePct >= 1.00) {
                redisTemplate.convertAndSend("budget:fuse",
                        "pause_explore:" + campaignId + ":100");
                log.warn("BUDGET FUSE 100%: campaign {} exploratory paused", campaignId);
            } else if (usagePct >= 0.80) {
                redisTemplate.convertAndSend("budget:fuse",
                        "reduce_bid:" + campaignId + ":80");
                log.info("BUDGET FUSE 80%: campaign {} bid reduced", campaignId);
            }
        }

        return newVal;
    }
}
```

- [ ] **Step 7: Create startup sync runner**

Create `ad-platform/backend/src/main/java/com/ad/sync/StartupSyncRunner.java`:

```java
package com.ad.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSyncRunner implements CommandLineRunner {

    private final StrategySyncService strategySyncService;
    private final AdSlotSyncService adSlotSyncService;
    private final BudgetSyncService budgetSyncService;

    @Override
    public void run(String... args) {
        log.info("Starting Redis sync on startup...");
        adSlotSyncService.syncAllActive();
        strategySyncService.syncAllActive();
        budgetSyncService.syncDailyBudgets();
        log.info("Redis sync complete.");
    }
}
```

- [ ] **Step 8: Test the sync**

```bash
# Start MySQL + Redis
cd ad-platform && docker compose up -d mysql redis

# Start Management Service
cd backend && mvn spring-boot:run &
sleep 15

# Create a publisher and ad slot
curl -s -X POST http://localhost:8080/api/v1/publishers \
  -H "Content-Type: application/json" \
  -d '{"name":"Sync Test Media","code":"SYNC001","revenueShare":0.7}'
curl -s -X POST http://localhost:8080/api/v1/ad-slots \
  -H "Content-Type: application/json" \
  -d '{"publisherId":1,"name":"Sync Slot","code":"SLOT_SYNC_001","slotType":1,"width":320,"height":480,"floorPrice":0.01}'

# Verify Redis has the slot config
redis-cli HGETALL rtb:ad_slot:1
# Expected: width → 320, floorPrice → 0.01, etc.
```

- [ ] **Step 9: Commit**

```bash
git add ad-platform/backend/src/main/java/com/ad/sync/
git add ad-platform/backend/src/main/java/com/ad/listener/
git add ad-platform/backend/src/main/java/com/ad/config/
git commit -m "feat(management): add Redis Pub/Sub data sync module for RTB config"
```

---

### Task 2: Bidding — Redis Config Subscriber (Bidding Service)

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/config/RedisSubscriber.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/config/ConfigCache.java`
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/StrategyMatcher.java` (load from Redis)
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/AdxEngine.java` (load slot config from Redis)
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/BudgetEngine.java` (use Redis budget)
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/verticle/MainVerticle.java` (start subscriber)

**Interfaces:**
- Consumes: Redis keys written by Management Service in Task 1
- Produces: Live-updating config cache that replaces Phase 1 hardcoded configs

- [ ] **Step 1: Create ConfigCache.java**

```java
package com.ad.bidding.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.impl.RedisAPIImpl;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory config cache synced from Redis.
 * All reads go through this cache — never directly to Redis in hot path.
 */
@Slf4j
public class ConfigCache {

    private final ConcurrentHashMap<Long, JsonObject> strategyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, JsonObject> adSlotCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BigDecimal> budgetCache = new ConcurrentHashMap<>(); // key: campaignId:date
    private final RedisAPI redis;

    public ConfigCache(RedisAPI redis) {
        this.redis = redis;
    }

    // ====== Strategy Cache ======

    public JsonObject getStrategy(Long id) {
        return strategyCache.get(id);
    }

    public void updateStrategy(Long id) {
        redis.hgetall("rtb:strategy:" + id).onSuccess(map -> {
            if (map != null && !map.isEmpty()) {
                JsonObject obj = new JsonObject();
                map.forEach((k, v) -> obj.put(k.toString(), v.toString()));
                strategyCache.put(id, obj);
                log.debug("Strategy {} cached from Redis", id);
            } else {
                strategyCache.remove(id);
                log.debug("Strategy {} removed from cache (not in Redis)", id);
            }
        }).onFailure(err -> log.warn("Failed to fetch strategy {} from Redis: {}", id, err.getMessage()));
    }

    public void loadAllStrategies() {
        // Phase 2: scan Redis keys with "rtb:strategy:*" pattern
        // For simplicity, reload on first access. Keys are managed by Management.
        log.info("Strategy cache initialized (lazy load from Redis on first access)");
    }

    // ====== Ad Slot Cache ======

    public JsonObject getAdSlot(Long id) {
        return adSlotCache.get(id);
    }

    public void updateAdSlot(Long id) {
        redis.hgetall("rtb:ad_slot:" + id).onSuccess(map -> {
            if (map != null && !map.isEmpty()) {
                JsonObject obj = new JsonObject();
                map.forEach((k, v) -> obj.put(k.toString(), v.toString()));
                adSlotCache.put(id, obj);
                log.debug("AdSlot {} cached from Redis", id);
            }
        }).onFailure(err -> log.warn("Failed to fetch ad_slot {} from Redis: {}", id, err.getMessage()));
    }

    // ====== Budget Cache ======

    public BigDecimal getRemainingBudget(Long campaignId, String date) {
        return budgetCache.getOrDefault(campaignId + ":" + date, BigDecimal.ZERO);
    }

    public void updateBudget(Long campaignId, String date) {
        String key = "budget:" + campaignId + ":" + date;
        redis.get(key).onSuccess(val -> {
            if (val != null) {
                budgetCache.put(campaignId + ":" + date, new BigDecimal(val.toString()));
            } else {
                budgetCache.remove(campaignId + ":" + date);
            }
        });
    }

    // ====== Cache Invalidation ======

    public void handleConfigChange(String payload) {
        log.info("Config change received: {}", payload);
        String[] parts = payload.split(":");
        if (parts.length < 2) return;

        switch (parts[0]) {
            case "strategy":
                if ("deploy".equals(parts[1]) && parts.length >= 3) {
                    updateStrategy(Long.parseLong(parts[2]));
                } else if ("undeploy".equals(parts[1]) && parts.length >= 3) {
                    strategyCache.remove(Long.parseLong(parts[2]));
                }
                break;
            case "ad_slot":
                if ("sync".equals(parts[1]) && parts.length >= 3) {
                    updateAdSlot(Long.parseLong(parts[2]));
                }
                break;
            case "budget":
                if ("reduce_bid".equals(parts[1]) || "pause_explore".equals(parts[1]) || "full_stop".equals(parts[1])) {
                    log.warn("Budget fuse for campaign {}: {}", parts[2], parts[1]);
                }
                break;
        }
    }
}
```

- [ ] **Step 2: Create RedisSubscriber.java**

```java
package com.ad.bidding.config;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.impl.RedisAPI;
import io.vertx.redis.client.impl.RedisAPIImpl;
import lombok.extern.slf4j.Slf4j;

import static io.vertx.redis.client.Command.SUBSCRIBE;

/**
 * Subscribes to Redis Pub/Sub channel "config:changed" from Management Service.
 * Updates the ConfigCache on each message.
 */
@Slf4j
public class RedisSubscriber {

    private final ConfigCache configCache;

    public RedisSubscriber(Redis client, ConfigCache configCache, Vertx vertx) {
        this.configCache = configCache;
        subscribe(client, vertx);
    }

    private void subscribe(Redis client, Vertx vertx) {
        client.connect()
                .onSuccess(conn -> {
                    log.info("Redis subscriber connected");
                    conn.subscribe("config:changed");
                    conn.handler(msg -> {
                        if (msg != null && msg.size() > 0) {
                            String payload = msg.get(2) != null ? msg.get(2).toString() : "";
                            if (!payload.isEmpty()) {
                                vertx.runOnContext(v -> configCache.handleConfigChange(payload));
                            }
                        }
                    });
                })
                .onFailure(err -> {
                    log.warn("Redis subscriber connection failed (will retry in 10s): {}", err.getMessage());
                    vertx.setTimer(10000, id -> subscribe(client, vertx));
                });
    }
}
```

- [ ] **Step 3: Wire Redis subscriber into MainVerticle**

Modify `MainVerticle.java` — after Redis connect succeeds, initialize `ConfigCache` and `RedisSubscriber`:

```java
// In initRedis(), after redisClient.connect() succeeds:
if (conn != null) {
    RedisAPI redisApi = new RedisAPIImpl(redisClient);
    configCache = new ConfigCache(redisApi);
    redisSubscriber = new RedisSubscriber(redisClient, configCache, vertx);
    // Load initial config
    configCache.loadAllStrategies();
}
```

Also add fields:
```java
private ConfigCache configCache;
```

- [ ] **Step 4: Modify StrategyMatcher to use ConfigCache**

Modify the constructor to accept `ConfigCache`. Replace `buildDefaultCampaigns()` with loading from cache:

```java
public StrategyMatcher(ConfigCache configCache) {
    this.configCache = configCache;
}

// In match(), instead of iterating hardcoded list:
// Read strategy configs from cache based on available keys
// Phase 2: iterate all strategy keys in cache
public CampaignConfig match(BidRequest req, Map<String, Long> freqMap) {
    // ... iterate configCache strategies ...
}
```

- [ ] **Step 5: Modify AdxEngine to read floor price from cache**

```java
private BigDecimal getFloorPrice(Long adSlotId) {
    JsonObject slot = configCache.getAdSlot(adSlotId);
    if (slot != null && slot.getValue("floorPrice") != null) {
        return new BigDecimal(slot.getString("floorPrice", "0"));
    }
    return new BigDecimal("0.01"); // default fallback
}
```

- [ ] **Step 6: Verify config sync end-to-end**

```bash
# Start all services
# Create a strategy with RTB config via Management API
# Verify Bidding Service picks up the config from Redis
```

- [ ] **Step 7: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/config/
# plus modified source files
git commit -m "feat(bidding): add Redis config subscriber and live config cache"
```

---

### Task 3: Budget Fuse Engine (Bidding Service)

**Files:**
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/BudgetEngine.java` (Redis-backed, 3-level fuse)
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/BudgetFuse.java`

**Interfaces:**
- Consumes: Redis budget keys from Management Service
- Produces: Auto-scaling bid prices and campaign pausing based on budget consumption

- [ ] **Step 1: Create BudgetFuse.java**

```java
package com.ad.bidding.engine;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Budget fuse — 3-level budget protection.
 * Operates per-campaign based on Redis budget consumption.
 *
 * Level 1 (80%): Reduce bid factor to 0.8×
 * Level 2 (100%): Pause exploratory campaigns (keep high-value active)
 * Level 3 (120%): Full stop — all campaign activity suspended
 */
@Slf4j
public class BudgetFuse {

    private final ConcurrentHashMap<Long, FuseLevel> campaignFuses = new ConcurrentHashMap<>();

    @Getter
    public enum FuseLevel {
        GREEN(0, 1.0),
        YELLOW(1, 0.8),
        ORANGE(2, 0.5),
        RED(3, 0.0);

        private final int level;
        private final double bidFactor;

        FuseLevel(int level, double bidFactor) {
            this.level = level;
            this.bidFactor = bidFactor;
        }
    }

    /**
     * Calculate bid adjustment factor based on budget usage.
     */
    public FuseLevel evaluateFuse(BigDecimal remaining, BigDecimal dailyBudget) {
        if (dailyBudget == null || dailyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return FuseLevel.RED;
        }
        double usagePct = BigDecimal.ONE.subtract(
                remaining.divide(dailyBudget, 4, RoundingMode.HALF_UP)
        ).doubleValue();

        if (usagePct >= 1.20) return FuseLevel.RED;
        if (usagePct >= 1.00) return FuseLevel.ORANGE;
        if (usagePct >= 0.80) return FuseLevel.YELLOW;
        return FuseLevel.GREEN;
    }

    /**
     * Apply fuse factor to bid price.
     */
    public BigDecimal applyBidReduction(Long campaignId, BigDecimal bidPrice,
                                         BigDecimal remaining, BigDecimal dailyBudget) {
        FuseLevel level = evaluateFuse(remaining, dailyBudget);
        campaignFuses.put(campaignId, level);

        if (level == FuseLevel.RED) {
            log.warn("BUDGET FUSE RED: campaign {} fully stopped", campaignId);
            return BigDecimal.ZERO;
        }

        BigDecimal adjusted = bidPrice.multiply(BigDecimal.valueOf(level.getBidFactor()))
                .setScale(2, RoundingMode.HALF_UP);
        log.debug("BUDGET FUSE {}: campaign {}, bid {}→{}",
                level, campaignId, bidPrice, adjusted);
        return adjusted;
    }

    /**
     * Check if a campaign is paused by fuse (ORANGE means exploratory paused).
     * Strategy 6 (exploratory) is paused at ORANGE; strategies 1/4 (high value) continue.
     */
    public boolean isPausedForExploratory(Long campaignId, Long strategyId) {
        FuseLevel level = campaignFuses.get(campaignId);
        if (level == FuseLevel.ORANGE) {
            // At 100%, only keep retargeting (strategy 4) and high-value (strategy 1)
            return strategyId != null && strategyId != 4L && strategyId != 1L;
        }
        return level == FuseLevel.RED;
    }

    /**
     * Apply fuse from Redis budget change message.
     */
    public void handleFuseMessage(String payload) {
        // Payload format: "reduce_bid:campaignId:pct" or "pause_explore:campaignId:pct" or "full_stop:campaignId:pct"
        String[] parts = payload.split(":");
        if (parts.length < 2) return;
        Long campaignId = Long.parseLong(parts[1]);
        if (parts[0].contains("full_stop")) {
            campaignFuses.put(campaignId, FuseLevel.RED);
        } else if (parts[0].contains("pause_explore")) {
            campaignFuses.put(campaignId, FuseLevel.ORANGE);
        } else if (parts[0].contains("reduce_bid")) {
            campaignFuses.put(campaignId, FuseLevel.YELLOW);
        }
    }
}
```

- [ ] **Step 6: Wire BudgetFuse into DspDecisionEngine**

In `DspDecisionEngine.java`, after budget check and pricing:

```java
// Apply fuse bid reduction
BigDecimal fuseAdjusted = budgetFuse.applyBidReduction(
        matched.getId(), bidPrice, budgetEngine.getRemaining(), 
        matched.getBudgetDaily() != null ? matched.getBudgetDaily() : BigDecimal.ZERO);
if (fuseAdjusted.compareTo(BigDecimal.ZERO) <= 0) {
    // Fuse stopped this campaign
    return BidResponse.builder().win(false).nbr(1).strategyId(matched.getStrategyId()).campaignId(matched.getId()).build();
}
bidPrice = fuseAdjusted.min(bidPrice);
```

- [ ] **Step 3: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/BudgetFuse.java
git commit -m "feat(bidding): add 3-level budget fuse (80%/100%/120%)"
```

---

### Task 4: Real-time Dashboard with RTB Metrics

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/dto/RtbDashboardDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/RtbDashboardController.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/RtbDashboardService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/RtbDashboardServiceImpl.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/handler/MetricsHandler.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/metrics/MetricsCollector.java`

**Interfaces:**
- Consumes: bid logs from ADX Engine, tracking events from TrackingHandler
- Produces: `/api/v1/dashboard/rtb` endpoint with real-time fill rate, win rate, latency P95

- [ ] **Step 1: Create MetricsCollector.java (Bidding Service)**

```java
package com.ad.bidding.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.*;

/**
 * In-process metrics collector for RTB pipeline.
 * Records counts and latencies; flushed to log periodically.
 * Management Service reads via dashboard query (Phase 2: Redis shared counter).
 */
@Slf4j
public class MetricsCollector {

    // Counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong winCount = new AtomicLong(0);
    private final AtomicLong noBidCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    // Latency tracking
    private final LongAdder totalLatency = new LongAdder();
    private final AtomicLong maxLatency = new AtomicLong(0);

    // Sliding window for P95 (last N samples)
    private static final int LATENCY_WINDOW_SIZE = 10000;
    private final AtomicIntegerArray latencySamples = new AtomicIntegerArray(LATENCY_WINDOW_SIZE);
    private final AtomicInteger sampleIndex = new AtomicInteger(0);

    // Hourly counters for rate calculation
    private final AtomicLong hourlyBids = new AtomicLong(0);
    private long hourlyStartTime = System.currentTimeMillis();

    public void recordRequest(boolean win, long latencyMs) {
        totalRequests.incrementAndGet();
        hourlyBids.incrementAndGet();

        if (win) {
            winCount.incrementAndGet();
        } else {
            noBidCount.incrementAndGet();
        }

        totalLatency.add(latencyMs);

        // Track max
        maxLatency.updateAndGet(current -> Math.max(current, latencyMs));

        // Sample for P95
        int idx = sampleIndex.getAndIncrement() % LATENCY_WINDOW_SIZE;
        if (idx >= 0 && idx < LATENCY_WINDOW_SIZE) {
            latencySamples.set(idx, (int) Math.min(latencyMs, Integer.MAX_VALUE));
        }
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }

    /**
     * Calculate P95 latency from the sliding window.
     */
    public long getP95Latency() {
        int count = Math.min(sampleIndex.get(), LATENCY_WINDOW_SIZE);
        if (count == 0) return 0;

        int[] sorted = new int[count];
        for (int i = 0; i < count; i++) {
            sorted[i] = latencySamples.get(i);
        }
        java.util.Arrays.sort(sorted);

        int p95Idx = (int) Math.ceil(count * 0.95) - 1;
        return Math.max(0, sorted[Math.max(p95Idx, 0)]);
    }

    public long getTotalRequests() { return totalRequests.get(); }
    public long getWinCount() { return winCount.get(); }
    public long getNoBidCount() { return noBidCount.get(); }
    public long getErrorCount() { return errorCount.get(); }

    /**
     * Get fill rate (win + no-bid) and win rate (win / total).
     */
    public double getFillRate() {
        long total = totalRequests.get();
        if (total == 0) return 0;
        return (double) (winCount.get() + noBidCount.get()) / total;
    }

    public double getWinRate() {
        return getTotalRequests() == 0 ? 0 :
                (double) getWinCount() / getTotalRequests();
    }

    /**
     * Get current requests per second (based on hourly counter).
     */
    public double getCurrentQps() {
        long elapsed = System.currentTimeMillis() - hourlyStartTime;
        if (elapsed < 1000) return 0;
        return (double) hourlyBids.get() / (elapsed / 1000.0);
    }

    /**
     * Reset hourly counters (call every hour or via scheduled task).
     */
    public void resetHourly() {
        hourlyBids.set(0);
        hourlyStartTime = System.currentTimeMillis();
    }
}
```

- [ ] **Step 2: Create MetricsHandler.java (Bidding Service)**

```java
package com.ad.bidding.handler;

import com.ad.bidding.metrics.MetricsCollector;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Exposes real-time metrics for Management Service to poll.
 * GET /metrics/rtb
 */
public class MetricsHandler implements Handler<RoutingContext> {

    private final MetricsCollector collector;

    public MetricsHandler(MetricsCollector collector) {
        this.collector = collector;
    }

    @Override
    public void handle(RoutingContext ctx) {
        JsonObject metrics = new JsonObject()
                .put("totalRequests", collector.getTotalRequests())
                .put("winCount", collector.getWinCount())
                .put("noBidCount", collector.getNoBidCount())
                .put("errorCount", collector.getErrorCount())
                .put("fillRate", String.format("%.2f", collector.getFillRate() * 100))
                .put("winRate", String.format("%.2f", collector.getWinRate() * 100))
                .put("p95LatencyMs", collector.getP95Latency())
                .put("currentQps", String.format("%.1f", collector.getCurrentQps()));

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(metrics.encode());
    }
}
```

- [ ] **Step 3: Wire MetricsCollector into ADX Engine**

In `AdxEngine.java`, inject `MetricsCollector` and call `recordRequest(win, latencyMs)` after each bid decision.

Mount MetricsHandler route in MainVerticle:
```java
router.get("/metrics/rtb").handler(new MetricsHandler(metricsCollector));
```

- [ ] **Step 4: Create RtbDashboardDTO.java (Management Service)**

```java
package com.ad.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RtbDashboardDTO {
    // RTB metrics from Bidding Service
    private long totalRequests;
    private long winCount;
    private long noBidCount;
    private double fillRate;
    private double winRate;
    private long p95LatencyMs;
    private double currentQps;

    // Hourly breakdown (latest 24 hours)
    private long hourlyRequests;
    private long hourlyWins;
    private BigDecimal hourlyCost;

    // Budget usage
    private BigDecimal totalDailyBudget;
    private BigDecimal totalSpent;
    private double budgetUsageRate;

    // Strategy breakdown
    private int activeStrategyCount;
    private int activeCampaignCount;
}
```

- [ ] **Step 5: Create RtbDashboardService.java**

```java
package com.ad.service;

import com.ad.dto.RtbDashboardDTO;

public interface RtbDashboardService {
    RtbDashboardDTO getRtbDashboard();
}
```

- [ ] **Step 6: Create RtbDashboardServiceImpl.java**

```java
package com.ad.service.impl;

import com.ad.dto.RtbDashboardDTO;
import com.ad.entity.Campaign;
import com.ad.entity.Strategy;
import com.ad.mapper.CampaignMapper;
import com.ad.mapper.StatsHourlyMapper;
import com.ad.mapper.StrategyMapper;
import com.ad.service.RtbDashboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RtbDashboardServiceImpl implements RtbDashboardService {

    private final StrategyMapper strategyMapper;
    private final CampaignMapper campaignMapper;
    private final StatsHourlyMapper statsHourlyMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String BIDDING_METRICS_URL = "http://localhost:9090/metrics/rtb";

    @Override
    public RtbDashboardDTO getRtbDashboard() {
        RtbDashboardDTO dto = new RtbDashboardDTO();

        // 1. Fetch RTB metrics from Bidding Service
        try {
            String json = restTemplate.getForObject(BIDDING_METRICS_URL, String.class);
            JsonNode metrics = objectMapper.readTree(json);
            dto.setTotalRequests(metrics.get("totalRequests").asLong());
            dto.setWinCount(metrics.get("winCount").asLong());
            dto.setNoBidCount(metrics.get("noBidCount").asLong());
            dto.setFillRate(metrics.get("fillRate").asDouble());
            dto.setWinRate(metrics.get("winRate").asDouble());
            dto.setP95LatencyMs(metrics.get("p95LatencyMs").asLong());
            dto.setCurrentQps(metrics.get("currentQps").asDouble());
        } catch (Exception e) {
            log.warn("Failed to fetch RTB metrics from Bidding Service: {}", e.getMessage());
        }

        // 2. Strategy/campaign counts
        long activeStrategies = strategyMapper.selectCount(
                new LambdaQueryWrapper<Strategy>().eq(Strategy::getStatus, 1));
        long activeCampaigns = campaignMapper.selectCount(
                new LambdaQueryWrapper<Campaign>().eq(Campaign::getStatus, 1));
        dto.setActiveStrategyCount((int) activeStrategies);
        dto.setActiveCampaignCount((int) activeCampaigns);

        // 3. Today's budget and spend
        LocalDate today = LocalDate.now();
        List<Campaign> allCampaigns = campaignMapper.selectList(
                new LambdaQueryWrapper<Campaign>().eq(Campaign::getStatus, 1));

        BigDecimal totalBudget = BigDecimal.ZERO;
        for (Campaign c : allCampaigns) {
            if (c.getBudgetDaily() != null) {
                totalBudget = totalBudget.add(c.getBudgetDaily());
            }
        }
        dto.setTotalDailyBudget(totalBudget);

        // Get today's cost from stats_hourly
        List<Map<String, Object>> todayStats = statsHourlyMapper.sumByDateRange(
                null, null, today, today);
        BigDecimal todayCost = BigDecimal.ZERO;
        if (!todayStats.isEmpty()) {
            Object costObj = todayStats.get(0).get("total_cost");
            if (costObj instanceof BigDecimal) todayCost = (BigDecimal) costObj;
        }
        dto.setTotalSpent(todayCost);
        dto.setBudgetUsageRate(totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? todayCost.divide(totalBudget, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0);

        // 4. Hourly breakdown — last 24h
        LocalDate yesterday = today.minusDays(1);
        List<Map<String, Object>> hourlyStats = statsHourlyMapper.sumByDateRange(
                null, null, yesterday, today);
        if (!hourlyStats.isEmpty()) {
            Map<String, Object> stats = hourlyStats.get(0);
            dto.setHourlyRequests(
                    stats.get("impressions") instanceof Number
                            ? ((Number) stats.get("impressions")).longValue() : 0);
            dto.setHourlyCost(
                    stats.get("total_cost") instanceof BigDecimal
                            ? (BigDecimal) stats.get("total_cost") : BigDecimal.ZERO);
        }

        return dto;
    }
}
```

- [ ] **Step 7: Create RtbDashboardController.java**

```java
package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.RtbDashboardDTO;
import com.ad.service.RtbDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class RtbDashboardController {

    private final RtbDashboardService rtbDashboardService;

    @GetMapping("/rtb")
    public Result<RtbDashboardDTO> getRtbDashboard() {
        return Result.ok(rtbDashboardService.getRtbDashboard());
    }
}
```

- [ ] **Step 8: Register RestTemplate bean**

In `AdApplication.java` or a config class:

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

- [ ] **Step 9: Test dashboard**

```bash
# Both services running
curl http://localhost:8080/api/v1/dashboard/rtb
# Expected: JSON with RTB metrics + campaign stats
```

- [ ] **Step 10: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/metrics/
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/handler/MetricsHandler.java
git add ad-platform/backend/src/main/java/com/ad/dto/RtbDashboardDTO.java
git add ad-platform/backend/src/main/java/com/ad/controller/RtbDashboardController.java
git add ad-platform/backend/src/main/java/com/ad/service/RtbDashboardService.java
git add ad-platform/backend/src/main/java/com/ad/service/impl/RtbDashboardServiceImpl.java
git commit -m "feat: add real-time RTB dashboard with metrics collector"
```

---

### Task 5: Alert Rules for RTB Pipeline

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/rule/RtbAlertRule.java`
- Modify: `ad-platform/backend/src/main/java/com/ad/service/impl/RuleServiceImpl.java` (add RTB trigger types)

**Interfaces:**
- Consumes: RtbDashboardDTO from Dashboard Service
- Produces: Alert notifications via existing rule framework

- [ ] **Step 1: Create RtbAlertRule enum**

```java
package com.ad.rule;

import lombok.Getter;

/**
 * RTB-specific alert rules for the existing rule engine.
 * Trigger metric names map to the RtbDashboardDTO fields.
 */
@Getter
public enum RtbAlertRule {

    FILL_RATE_DROP("fill_rate", "RTB填充率低于阈值"),
    WIN_RATE_DROP("win_rate", "RTB胜出率低于阈值"),
    LATENCY_SPIKE("p95_latency", "RTB P95延迟超过阈值"),
    BUDGET_RUNWAY("budget_usage_rate", "预算消耗过快");

    private final String metric;
    private final String description;

    RtbAlertRule(String metric, String description) {
        this.metric = metric;
        this.description = description;
    }
}
```

- [ ] **Step 2: Extend RuleServiceImpl**

Add RTB metric evaluation in RuleServiceImpl's scheduled evaluation:

```java
// In the existing rule evaluation method, add:
if ("fill_rate".equals(rule.getTriggerMetric())) {
    Double fillRate = rtbDashboardService.getRtbDashboard().getFillRate();
    // Compare against threshold
    return evaluateNumeric(rule, fillRate);
}
if ("p95_latency".equals(rule.getTriggerMetric())) {
    Long p95 = rtbDashboardService.getRtbDashboard().getP95LatencyMs();
    return evaluateNumeric(rule, p95.doubleValue());
}
```

- [ ] **Step 3: Commit**

```bash
git add ad-platform/backend/src/main/java/com/ad/rule/RtbAlertRule.java
git commit -m "feat(management): add RTB alert rules for fill rate, win rate, latency"
```

---

### Task 6: Load Testing — 500 QPS Baseline

**Files:**
- Create: `ad-platform/load-test/rtb-benchmark.lua` (wrk script)
- Create: `ad-platform/load-test/run-benchmark.sh`

**Interfaces:**
- Consumes: both services running
- Produces: wrk report showing 500+ QPS, P95 < 150ms

- [ ] **Step 1: Create wrk benchmark script**

```lua
-- rtb-benchmark.lua
-- wrk benchmark for RTB pipeline
-- Usage: wrk -t4 -c50 -d30s -s rtb-benchmark.lua http://localhost:9090/ad/request

local counter = 1
local prefixes = {"hv-", "rt-", "cp-", "new-", "unknown-"}
local uas = {
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
    "Mozilla/5.0 (Linux; Android 14; Pixel 8)",
    "Mozilla/5.0 (Linux; Android 13; Samsung Galaxy S23)"
}

request = function()
    local prefix = prefixes[counter % #prefixes + 1]
    local deviceId = prefix .. "bench-" .. counter
    local ua = uas[counter % #uas + 1]
    counter = counter + 1

    local body = string.format(
        '{"device_id":"%s","ip":"192.168.1.%d","ua":"%s","ad_slot_code":"SLOT_001","width":320,"height":480,"app_package":"com.bench.media"}',
        deviceId, counter % 255, ua
    )

    return wrk.format("POST", "/ad/request", {
        ["Content-Type"] = "application/json",
        ["X-Auth-Token"] = "demo-token-001"
    }, body)
end

done = function(summary, latency, requests)
    io.write("--- RTB Benchmark Results ---\n")
    io.write(string.format("Total requests: %d\n", summary.requests))
    io.write(string.format("Duration: %.2fs\n", summary.duration))
    io.write(string.format("QPS: %.2f\n", summary.requests / summary.duration))
    io.write(string.format("Win rate: TODO (parse response body)\n"))
    io.write(string.format("Errors: %d\n", summary.errors))

    io.write("Latency distribution:\n")
    io.write(string.format("  Avg: %.2fms\n", latency.mean))
    io.write(string.format("  Max: %dms\n", latency.max))
    io.write(string.format("  Stdev: %.2fms\n", latency.stdev))

    local p = latency:percentile()
    io.write(string.format("  P50: %.2fms\n", p[50]))
    io.write(string.format("  P90: %.2fms\n", p[90]))
    io.write(string.format("  P95: %.2fms\n", p[95]))
    io.write(string.format("  P99: %.2fms\n", p[99]))
end
```

- [ ] **Step 2: Create run-benchmark.sh**

```bash
#!/bin/bash
# Run RTB benchmark

set -e

BIDDING_URL=${1:-"http://localhost:9090/ad/request"}
DURATION=${2:-30}
THREADS=${3:-4}
CONNECTIONS=${4:-50}

echo "=== RTB Load Benchmark ==="
echo "Target: $BIDDING_URL"
echo "Duration: ${DURATION}s"
echo "Threads: $THREADS"
echo "Connections: $CONNECTIONS"
echo ""

# Check if wrk is installed
if ! command -v wrk &> /dev/null; then
    echo "wrk not found. Installing via choco..."
    choco install wrk -y || {
        echo "Failed to install wrk. Install manually: https://github.com/wg/wrk"
        exit 1
    }
fi

wrk -t$THREADS -c$CONNECTIONS -d${DURATION}s \
    -s "$(dirname "$0")/rtb-benchmark.lua" \
    "$BIDDING_URL"

echo ""
echo "Benchmark complete."
```

- [ ] **Step 3: Run benchmark**

```bash
chmod +x ad-platform/load-test/run-benchmark.sh
cd ad-platform/load-test && ./run-benchmark.sh
```

Expected output (approximate):
```
--- RTB Benchmark Results ---
Total requests: 15000
Duration: 30.00s
QPS: 500.00
Errors: 0
Latency distribution:
  Avg: 8.50ms
  Max: 120ms
  P50: 5.20ms
  P95: 25.30ms
  P99: 80.10ms
```

- [ ] **Step 4: Commit**

```bash
git add ad-platform/load-test/
git commit -m "test: add RTB load test scripts with 500 QPS baseline"
```

---

## Phase 2 Completion Checklist

- [ ] Task 1: Data Sync Module — Redis config publisher in Management Service
- [ ] Task 2: Redis config subscriber in Bidding Service, live config cache
- [ ] Task 3: Budget fuse engine — 3 levels, driven by Redis
- [ ] Task 4: Real-time dashboard — RTB metrics exposed via REST API
- [ ] Task 5: Alert rules — RTB-specific triggers in existing rule engine
- [ ] Task 6: Load test — 500 QPS with P95 < 150ms

**Phase 2 deliverable:** Config sync between Management and Bidding is live. Budget protection operates automatically. Dashboard shows real-time RTB metrics. System sustained 500 QPS.
