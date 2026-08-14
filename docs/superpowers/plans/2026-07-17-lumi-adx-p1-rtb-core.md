# LUMI DSP-ADX-SSP 全链路平台 — Phase 1: 核心 RTB 链路打通

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the real-time bidding core: SSP → ADX → DSP → Tracking, end-to-end working with mock media traffic at 200 QPS.

**Architecture:** Two-service architecture. Bidding Service (Vert.x 4.x) handles all RTB traffic. Management Service (Spring Boot 3.x, existing code) adds Publisher/AdSlot/Strategy-extension for the RTB pipeline. They share MySQL + Redis but Phase 1 uses in-memory config for Bidding.

**Tech Stack:** Vert.x 4.5 + Spring Boot 3.2.x + MySQL 8.0 + Redis 7.x + Maven multi-module

## Global Constraints

- Java 17, Maven 3.9+
- API prefix: `/api/v1`, Response format: `{ code: 0, data: {...}, message: "ok" }`
- Dates: ISO 8601 (`yyyy-MM-dd`), datetime `yyyy-MM-dd HH:mm:ss`
- Soft delete on all tables, version field for optimistic locking (existing convention)
- Naming: Java camelCase, SQL snake_case
- Bidding Service port: 9090, Management Service port: 8080
- Two services in the same Git repo, separate Maven modules
- Bidding Service endpoints are NOT prefixed with `/api/v1`
- All new files under `ad-platform/` directory

---

### Task 1: Multi-Module Restructure + DB Migration

**Files:**
- Create: `ad-platform/pom.xml` (aggregator parent)
- Modify: `ad-platform/backend/pom.xml` (add parent ref + module name)
- Create: `ad-platform/bidding-service/pom.xml`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/BiddingApplication.java`
- Create: `ad-platform/backend/src/main/resources/db/migration-v2.sql`

**Interfaces:**
- Consumes: existing `ad-platform/backend/pom.xml` structure, existing `init-schema.sql`
- Produces: Maven multi-module build, new database tables `ad_publisher`, `ad_ad_slot`, `ad_bid_log`, `ad_tracking_log`, parent + child POMs

- [ ] **Step 1: Create aggregator parent POM at ad-platform/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ad</groupId>
    <artifactId>ad-platform-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <name>ad-platform-parent</name>

    <modules>
        <module>backend</module>
        <module>bidding-service</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
</project>
```

- [ ] **Step 2: Modify backend/pom.xml to be a child module**

Add `<parent>` block at top (after `<modelVersion>`), replace existing `<groupId>`/`<version>`:

```xml
<parent>
    <groupId>com.ad</groupId>
    <artifactId>ad-platform-parent</artifactId>
    <version>1.0.0</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>backend</artifactId>
<name>ad-platform-backend</name>
<!-- keep existing version 1.0.0 or remove it (inherited from parent) -->
```

Keep all existing dependencies unchanged. Add one new dependency for Redis pub/sub support (already in spring-boot-starter-data-redis).

- [ ] **Step 3: Create bidding-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ad</groupId>
        <artifactId>ad-platform-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>bidding-service</artifactId>
    <version>1.0.0</version>
    <name>bidding-service</name>

    <properties>
        <vertx.version>4.5.10</vertx.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.vertx</groupId>
                <artifactId>vertx-stack-depchain</artifactId>
                <version>${vertx.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-core</artifactId>
        </dependency>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-web</artifactId>
        </dependency>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-redis-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-mysql-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-config</artifactId>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.17.1</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>2.17.1</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.32</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.5.6</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.ad.bidding.BiddingApplication</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create BiddingApplication.java entry point**

```java
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
```

- [ ] **Step 5: Create DB migration SQL**

```sql
-- ============================================================
-- Migration V2: DSP-ADX-SSP tables for Phase 1
-- ============================================================

USE ad_platform;

-- 1. 媒体方表
CREATE TABLE IF NOT EXISTS ad_publisher (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL                COMMENT '媒体名称',
    code            VARCHAR(20)     NOT NULL UNIQUE         COMMENT '媒体编码',
    contact         VARCHAR(50)     DEFAULT NULL            COMMENT '联系人',
    api_token       VARCHAR(64)     NOT NULL                COMMENT '接入Token',
    revenue_share   DECIMAL(5,2)    DEFAULT 0.70            COMMENT '媒体分成比例',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '0-禁用 1-激活',
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     DEFAULT NULL,
    updated_by      VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='媒体方';

-- 2. 广告位表
CREATE TABLE IF NOT EXISTS ad_ad_slot (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    publisher_id    BIGINT          NOT NULL                COMMENT '所属媒体',
    name            VARCHAR(100)    NOT NULL                COMMENT '广告位名称',
    code            VARCHAR(20)     NOT NULL UNIQUE         COMMENT '广告位编码',
    slot_type       TINYINT         NOT NULL DEFAULT 1      COMMENT '1-Banner 2-插屏 3-激励视频 4-原生',
    width           INT             NOT NULL                COMMENT '广告位宽度',
    height          INT             NOT NULL                COMMENT '广告位高度',
    floor_price     DECIMAL(10,2)   DEFAULT 0.00            COMMENT '底价(CPM)',
    block_category  VARCHAR(500)    DEFAULT NULL            COMMENT '屏蔽品类ID列表(JSON)',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '0-禁用 1-激活',
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     DEFAULT NULL,
    updated_by      VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_publisher (publisher_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告位';

-- 3. 竞价日志表
CREATE TABLE IF NOT EXISTS ad_bid_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    ad_slot_id      BIGINT          NOT NULL                COMMENT '广告位ID',
    campaign_id     BIGINT          DEFAULT NULL            COMMENT '命中的计划ID',
    strategy_id     BIGINT          DEFAULT NULL            COMMENT '命中的策略ID',
    device_id       VARCHAR(64)     DEFAULT NULL            COMMENT '设备ID(脱敏)',
    bid_price       DECIMAL(10,2)   DEFAULT 0.00            COMMENT '出价(CPM)',
    floor_price     DECIMAL(10,2)   DEFAULT 0.00            COMMENT '底价',
    win             TINYINT         DEFAULT 0               COMMENT '是否胜出',
    nbr             TINYINT         DEFAULT NULL            COMMENT '不出价原因码',
    bid_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '竞价时间',
    latency_ms      INT             DEFAULT 0               COMMENT '竞价耗时(ms)',
    PRIMARY KEY (id),
    KEY idx_bid_at (bid_at),
    KEY idx_slot (ad_slot_id),
    KEY idx_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞价日志';

-- 4. 监播日志表
CREATE TABLE IF NOT EXISTS ad_tracking_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    campaign_id     BIGINT          DEFAULT NULL            COMMENT '计划ID',
    strategy_id     BIGINT          DEFAULT NULL            COMMENT '策略ID',
    device_id       VARCHAR(64)     DEFAULT NULL            COMMENT '设备ID(脱敏)',
    track_type      TINYINT         NOT NULL                COMMENT '1-曝光 2-点击 3-转化',
    track_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    PRIMARY KEY (id),
    KEY idx_track_at (track_at),
    KEY idx_type (track_type),
    KEY idx_campaign (campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监播日志';
```

- [ ] **Step 6: Verify multi-module build**

Run: `cd ad-platform && mvn clean compile -q`
Expected: Both `backend` and `bidding-service` compile successfully.

- [ ] **Step 7: Commit**

```bash
git add ad-platform/pom.xml ad-platform/backend/pom.xml ad-platform/bidding-service/
git add ad-platform/backend/src/main/resources/db/migration-v2.sql
git commit -m "feat: add bidding-service module and DB migration for ADX tables"
```

---

### Task 2: Bidding Service Skeleton — MainVerticle + Redis + Config

**Files:**
- Create: `ad-platform/bidding-service/src/main/resources/config.json`
- Create: `ad-platform/bidding-service/src/main/resources/logback.xml`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/verticle/MainVerticle.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/config/RedisClientFactory.java`

**Interfaces:**
- Consumes: Task 1 POM structure
- Produces: Running Vert.x instance on port 9090 with HTTP server + Redis connection

- [ ] **Step 1: Create config.json**

```json
{
  "server": {
    "port": 9090
  },
  "redis": {
    "host": "localhost",
    "port": 6379,
    "password": ""
  },
  "mysql": {
    "host": "localhost",
    "port": 3306,
    "database": "ad_platform",
    "user": "root",
    "password": "root"
  },
  "tracking": {
    "baseUrl": "http://localhost:9090/track"
  },
  "bidding": {
    "timeoutMs": 50,
    "defaultFloorPrice": 0.01
  }
}
```

- [ ] **Step 2: Create logback.xml**

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/bidding.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/bidding.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

- [ ] **Step 3: Create RedisClientFactory.java**

```java
package com.ad.bidding.config;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisClient;
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
```

- [ ] **Step 4: Create MainVerticle.java**

```java
package com.ad.bidding.verticle;

import com.ad.bidding.config.RedisClientFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
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
                    initRedis(cfg)
                            .onSuccess(v -> startHttpServer(cfg, startPromise))
                            .onFailure(startPromise::fail);
                })
                .onFailure(startPromise::fail);
    }

    private Promise<Void> initRedis(JsonObject cfg) {
        Promise<Void> promise = Promise.promise();
        JsonObject redisCfg = cfg.getJsonObject("redis");
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
                    // Non-fatal in Phase 1 — bidding works with in-memory config
                    promise.complete();
                });
        return promise;
    }

    private void startHttpServer(JsonObject cfg, Promise<Void> startPromise) {
        int port = cfg.getJsonObject("server").getInteger("port", 9090);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Health check
        router.get("/health").handler(ctx -> {
            ctx.json(new JsonObject().put("status", "UP"));
        });

        // Routes will be mounted by sub-verticles in later tasks:
        // SSP: POST /ad/request
        // Tracking: GET /track/imp, /track/click, /track/conv, /track/landing

        server = vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(s -> {
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
```

- [ ] **Step 5: Verify the service starts**

Run: `cd ad-platform/bidding-service && mvn compile exec:java -Dexec.mainClass="com.ad.bidding.BiddingApplication"`
Expected: "Bidding Service HTTP server started on port 9090"
Curl: `curl http://localhost:9090/health` → `{"status":"UP"}`

- [ ] **Step 6: Commit**

```bash
git add ad-platform/bidding-service/src/main/resources/
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/
git commit -m "feat(bidding): add MainVerticle with Redis + HTTP server skeleton"
```

---

### Task 3: Bidding — SSP Gateway

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/model/BidRequest.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/model/BidResponse.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/model/AdResponse.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/handler/SspHandler.java`
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/verticle/MainVerticle.java` (mount SspHandler routes)

**Interfaces:**
- Consumes: MainVerticle's Router instance
- Produces: `POST /ad/request` endpoint that validates, enriches, and forwards to ADX

- [ ] **Step 1: Create BidRequest.java**

```java
package com.ad.bidding.model;

import lombok.Data;

@Data
public class BidRequest {
    private String deviceId;       // 设备ID (OAID/IDFA)
    private String oaid;           // Android OAID
    private String ip;
    private String ua;             // User-Agent
    private String adSlotCode;     // 广告位编码
    private int width;
    private int height;
    private String appPackage;     // 媒体包名
    private long timestamp;        // 请求时间戳

    // Enriched by SSP Gateway
    private String geo;            // IP解析地域
    private String deviceType;     // UA解析设备类型
    private String os;             // 操作系统
    private long adSlotId;         // 解析后的广告位ID
    private long publisherId;      // 解析后的媒体ID
}
```

- [ ] **Step 2: Create BidResponse.java**

```java
package com.ad.bidding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private boolean win;
    private BigDecimal price;         // CPM出价
    private String adMaterialUrl;     // 素材URL
    private String landingUrl;        // 落地页URL
    private String trackImpUrl;       // 曝光监播URL
    private String trackClickUrl;     // 点击监播URL
    private int nbr;                  // 不出价原因码 (2=不出价)
    private Long strategyId;          // 命中的策略ID
    private Long campaignId;          // 命中的计划ID
    private List<String> impTrackers; // 三方曝光监播
    private List<String> clickTrackers; // 三方点击监播
}
```

- [ ] **Step 3: Create AdResponse.java** (response sent back to media)

```java
package com.ad.bidding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponse {
    private int code;           // 0=有广告 1=无广告
    private String msg;
    private String adType;      // html / img / video
    private String htmlSnippet; // 广告HTML片段
    private String impUrl;      // 曝光监播URL
    private String clickUrl;    // 点击监播URL
    private String landingUrl;  // 落地页URL
}
```

- [ ] **Step 4: Create SspHandler.java**

```java
package com.ad.bidding.handler;

import com.ad.bidding.model.AdResponse;
import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.BidResponse;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * SSP Gateway — 媒体广告请求入口
 *
 * Implements the full pipeline for each request:
 * 1. Validate request (ad slot exists, media active)
 * 2. Enrich user info (geo, device type from IP/UA)
 * 3. Forward to ADX Engine
 * 4. Format and return response
 */
@Slf4j
public class SspHandler implements Handler<RoutingContext> {

    // In Phase 1, ad slot config is hardcoded for demo.
    // Phase 2 will read from Redis cache synced from MySQL.
    private static final long VALID_AD_SLOT_ID = 1L;
    private static final long VALID_PUBLISHER_ID = 1L;
    private static final String VALID_TOKEN = "demo-token-001";
    private static final BigDecimal FLOOR_PRICE = new BigDecimal("0.01");

    @Override
    public void handle(RoutingContext ctx) {
        long startNanos = System.nanoTime();

        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            respondNoAd(ctx, 1, "invalid request body");
            return;
        }

        // 1. Parse and validate request
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

        if (!VALID_AD_SLOT_ID.equals(req.getAdSlotId())) {
            respondNoAd(ctx, 3, "unknown ad slot");
            return;
        }

        // 2. Enrich request
        enrichRequest(req);

        // 3. Forward to ADX (simulated for Phase 1 — DSP Decision Engine will be Task 4)
        BidResponse bidResponse = callAdx(req);

        // 4. Format response
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("SSP request processed: slot={}, win={}, elapsed={}ms",
                req.getAdSlotCode(), bidResponse.isWin(), elapsedMs);

        if (!bidResponse.isWin()) {
            respondNoAd(ctx, 0, "no bid");
            return;
        }

        // Build HTML snippet with tracking pixels
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
        // Phase 1: simple enrichment from IP/UA
        // Phase 2: read from Redis user profile cache
        String ip = req.getIp();
        if (ip != null) {
            if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                req.setGeo("internal");
            } else {
                req.setGeo("unknown"); // Phase 2: IP-to-geo DB
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
        // Phase 1: stub — always win with a fixed price
        // Phase 2: replaced with real ADX Engine + DSP Decision Engine
        return BidResponse.builder()
                .win(true)
                .price(new BigDecimal("1.50"))
                .adMaterialUrl("https://cdn.adx.com/materials/demo-creative.jpg")
                .landingUrl("https://lumi.example.com/product?utm_source=adx")
                .trackImpUrl(String.format("http://localhost:9090/track/imp/1/1/%s", req.getDeviceId()))
                .trackClickUrl(String.format("http://localhost:9090/track/click/1/1/%s", req.getDeviceId()))
                .strategyId(1L)
                .campaignId(1L)
                .impTrackers(Arrays.asList("https://imp.example.com/pixel"))
                .clickTrackers(Arrays.asList("https://clk.example.com/pixel"))
                .nbr(0)
                .build();
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
```

- [ ] **Step 5: Mount SSP route in MainVerticle**

In `MainVerticle.java`, inside `startHttpServer()`, after `router.route().handler(BodyHandler.create());` and after health check, add:

```java
// SSP Gateway
router.post("/ad/request").handler(new SspHandler());
```

- [ ] **Step 6: Test the endpoint**

Run the bidding service, then:

```bash
curl -X POST http://localhost:9090/ad/request \
  -H "Content-Type: application/json" \
  -H "X-Auth-Token: demo-token-001" \
  -d '{"device_id":"test-device-001","oaid":"test-oaid-001","ip":"192.168.1.1","ua":"Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)","ad_slot_code":"SLOT_001","width":320,"height":480,"app_package":"com.example.media"}'
```

Expected response: `{"code":0,"msg":"ok","adType":"html","htmlSnippet":"...", ...}`

- [ ] **Step 7: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/model/
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/handler/
# plus the modified MainVerticle.java
git commit -m "feat(bidding): add SSP Gateway with request validation and enrichment"
```

---

### Task 4: Management — Publisher + AdSlot CRUD

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/entity/Publisher.java`
- Create: `ad-platform/backend/src/main/java/com/ad/entity/AdSlot.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/PublisherDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/PublisherCreateDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/AdSlotDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/AdSlotCreateDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/mapper/PublisherMapper.java`
- Create: `ad-platform/backend/src/main/java/com/ad/mapper/AdSlotMapper.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/PublisherService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/PublisherServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/AdSlotService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/AdSlotServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/PublisherController.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/AdSlotController.java`

**Interfaces:**
- Consumes: existing BaseEntity, Result, PageResult patterns
- Produces: REST API at `/api/v1/publishers` and `/api/v1/ad-slots`

- [ ] **Step 1: Create Publisher entity**

```java
package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_publisher")
public class Publisher extends BaseEntity {
    private String name;
    private String code;
    private String contact;
    private String apiToken;
    private BigDecimal revenueShare;
    private Integer status;
}
```

- [ ] **Step 2: Create AdSlot entity**

```java
package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_ad_slot")
public class AdSlot extends BaseEntity {
    private Long publisherId;
    private String name;
    private String code;
    private Integer slotType;
    private Integer width;
    private Integer height;
    private BigDecimal floorPrice;
    private String blockCategory;
    private Integer status;
}
```

- [ ] **Step 3: Create DTOs**

```java
// PublisherDTO.java
package com.ad.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class PublisherDTO {
    private Long id;
    private String name;
    private String code;
    private String contact;
    private String apiToken;
    private BigDecimal revenueShare;
    private Integer status;
    private LocalDateTime createdAt;
}

// PublisherCreateDTO.java
package com.ad.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class PublisherCreateDTO {
    @NotBlank private String name;
    @NotBlank private String code;
    private String contact;
    @NotNull private BigDecimal revenueShare;
}

// AdSlotDTO.java
package com.ad.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class AdSlotDTO {
    private Long id;
    private Long publisherId;
    private String publisherName;
    private String name;
    private String code;
    private Integer slotType;
    private Integer width;
    private Integer height;
    private BigDecimal floorPrice;
    private String blockCategory;
    private Integer status;
    private LocalDateTime createdAt;
}

// AdSlotCreateDTO.java
package com.ad.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class AdSlotCreateDTO {
    @NotNull private Long publisherId;
    @NotBlank private String name;
    @NotBlank private String code;
    @NotNull private Integer slotType;
    @NotNull private Integer width;
    @NotNull private Integer height;
    private BigDecimal floorPrice;
    private String blockCategory;
}
```

- [ ] **Step 4: Create Mappers**

```java
// PublisherMapper.java
package com.ad.mapper;
import com.ad.entity.Publisher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
public interface PublisherMapper extends BaseMapper<Publisher> {}

// AdSlotMapper.java
package com.ad.mapper;
import com.ad.entity.AdSlot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
public interface AdSlotMapper extends BaseMapper<AdSlot> {}
```

- [ ] **Step 5: Create Services**

```java
// PublisherService.java
package com.ad.service;
import com.ad.dto.PublisherCreateDTO;
import com.ad.dto.PublisherDTO;
import java.util.List;
public interface PublisherService {
    List<PublisherDTO> listAll();
    PublisherDTO getById(Long id);
    Long create(PublisherCreateDTO dto);
    void update(Long id, PublisherCreateDTO dto);
    void delete(Long id);
}

// PublisherServiceImpl.java
package com.ad.service.impl;
import com.ad.dto.PublisherCreateDTO;
import com.ad.dto.PublisherDTO;
import com.ad.entity.Publisher;
import com.ad.mapper.PublisherMapper;
import com.ad.service.PublisherService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {
    private final PublisherMapper publisherMapper;

    @Override
    public List<PublisherDTO> listAll() {
        return publisherMapper.selectList(
                new LambdaQueryWrapper<Publisher>().orderByDesc(Publisher::getId)
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public PublisherDTO getById(Long id) {
        Publisher p = publisherMapper.selectById(id);
        return p == null ? null : toDTO(p);
    }

    @Override
    @Transactional
    public Long create(PublisherCreateDTO dto) {
        Publisher p = new Publisher();
        p.setName(dto.getName());
        p.setCode(dto.getCode());
        p.setContact(dto.getContact());
        p.setApiToken(UUID.randomUUID().toString().replace("-", ""));
        p.setRevenueShare(dto.getRevenueShare());
        p.setStatus(1);
        publisherMapper.insert(p);
        return p.getId();
    }

    @Override
    @Transactional
    public void update(Long id, PublisherCreateDTO dto) {
        Publisher p = publisherMapper.selectById(id);
        if (p == null) throw new RuntimeException("Publisher not found: " + id);
        p.setName(dto.getName());
        p.setCode(dto.getCode());
        p.setContact(dto.getContact());
        p.setRevenueShare(dto.getRevenueShare());
        publisherMapper.updateById(p);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        publisherMapper.deleteById(id);
    }

    private PublisherDTO toDTO(Publisher p) {
        PublisherDTO dto = new PublisherDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCode(p.getCode());
        dto.setContact(p.getContact());
        dto.setApiToken(p.getApiToken());
        dto.setRevenueShare(p.getRevenueShare());
        dto.setStatus(p.getStatus());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}

// AdSlotService.java
package com.ad.service;
import com.ad.dto.AdSlotCreateDTO;
import com.ad.dto.AdSlotDTO;
import java.util.List;
public interface AdSlotService {
    List<AdSlotDTO> listAll(Long publisherId);
    AdSlotDTO getById(Long id);
    Long create(AdSlotCreateDTO dto);
    void update(Long id, AdSlotCreateDTO dto);
}

// AdSlotServiceImpl.java
package com.ad.service.impl;
import com.ad.dto.AdSlotCreateDTO;
import com.ad.dto.AdSlotDTO;
import com.ad.entity.AdSlot;
import com.ad.entity.Publisher;
import com.ad.mapper.AdSlotMapper;
import com.ad.mapper.PublisherMapper;
import com.ad.service.AdSlotService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdSlotServiceImpl implements AdSlotService {
    private final AdSlotMapper adSlotMapper;
    private final PublisherMapper publisherMapper;

    @Override
    public List<AdSlotDTO> listAll(Long publisherId) {
        return adSlotMapper.selectList(
                new LambdaQueryWrapper<AdSlot>()
                        .eq(publisherId != null, AdSlot::getPublisherId, publisherId)
                        .orderByDesc(AdSlot::getId)
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AdSlotDTO getById(Long id) {
        AdSlot slot = adSlotMapper.selectById(id);
        return slot == null ? null : toDTO(slot);
    }

    @Override
    @Transactional
    public Long create(AdSlotCreateDTO dto) {
        AdSlot slot = new AdSlot();
        slot.setPublisherId(dto.getPublisherId());
        slot.setName(dto.getName());
        slot.setCode(dto.getCode());
        slot.setSlotType(dto.getSlotType());
        slot.setWidth(dto.getWidth());
        slot.setHeight(dto.getHeight());
        slot.setFloorPrice(dto.getFloorPrice());
        slot.setBlockCategory(dto.getBlockCategory());
        slot.setStatus(1);
        adSlotMapper.insert(slot);
        return slot.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AdSlotCreateDTO dto) {
        AdSlot slot = adSlotMapper.selectById(id);
        if (slot == null) throw new RuntimeException("AdSlot not found: " + id);
        slot.setPublisherId(dto.getPublisherId());
        slot.setName(dto.getName());
        slot.setCode(dto.getCode());
        slot.setSlotType(dto.getSlotType());
        slot.setWidth(dto.getWidth());
        slot.setHeight(dto.getHeight());
        slot.setFloorPrice(dto.getFloorPrice());
        slot.setBlockCategory(dto.getBlockCategory());
        adSlotMapper.updateById(slot);
    }

    private AdSlotDTO toDTO(AdSlot slot) {
        AdSlotDTO dto = new AdSlotDTO();
        dto.setId(slot.getId());
        dto.setPublisherId(slot.getPublisherId());
        dto.setName(slot.getName());
        dto.setCode(slot.getCode());
        dto.setSlotType(slot.getSlotType());
        dto.setWidth(slot.getWidth());
        dto.setHeight(slot.getHeight());
        dto.setFloorPrice(slot.getFloorPrice());
        dto.setBlockCategory(slot.getBlockCategory());
        dto.setStatus(slot.getStatus());
        dto.setCreatedAt(slot.getCreatedAt());

        // Load publisher name
        Publisher p = publisherMapper.selectById(slot.getPublisherId());
        dto.setPublisherName(p != null ? p.getName() : null);
        return dto;
    }
}
```

- [ ] **Step 6: Create Controllers**

```java
// PublisherController.java
package com.ad.controller;
import com.ad.common.Result;
import com.ad.dto.PublisherCreateDTO;
import com.ad.dto.PublisherDTO;
import com.ad.service.PublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/publishers")
@RequiredArgsConstructor
public class PublisherController {
    private final PublisherService publisherService;

    @GetMapping
    public Result<List<PublisherDTO>> list() {
        return Result.ok(publisherService.listAll());
    }

    @GetMapping("/{id}")
    public Result<PublisherDTO> getById(@PathVariable Long id) {
        PublisherDTO dto = publisherService.getById(id);
        return dto == null ? Result.fail("Publisher not found") : Result.ok(dto);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PublisherCreateDTO dto) {
        return Result.ok(publisherService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PublisherCreateDTO dto) {
        publisherService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        publisherService.delete(id);
        return Result.ok();
    }
}

// AdSlotController.java
package com.ad.controller;
import com.ad.common.Result;
import com.ad.dto.AdSlotCreateDTO;
import com.ad.dto.AdSlotDTO;
import com.ad.service.AdSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ad-slots")
@RequiredArgsConstructor
public class AdSlotController {
    private final AdSlotService adSlotService;

    @GetMapping
    public Result<List<AdSlotDTO>> list(@RequestParam(required = false) Long publisherId) {
        return Result.ok(adSlotService.listAll(publisherId));
    }

    @GetMapping("/{id}")
    public Result<AdSlotDTO> getById(@PathVariable Long id) {
        AdSlotDTO dto = adSlotService.getById(id);
        return dto == null ? Result.fail("AdSlot not found") : Result.ok(dto);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdSlotCreateDTO dto) {
        return Result.ok(adSlotService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdSlotCreateDTO dto) {
        adSlotService.update(id, dto);
        return Result.ok();
    }
}
```

- [ ] **Step 7: Add PublisherMapper and AdSlotMapper to MyBatis-Plus scan**

Verify `ad-platform/backend/src/main/resources/application.yml` has mapper scan configured (it should already from existing setup). If not, ensure `mybatis-plus.mapper-locations` covers the new mappers.

- [ ] **Step 8: Test the endpoints**

```bash
# Start the Management Service
cd ad-platform/backend && mvn spring-boot:run

# In another terminal
curl -X POST http://localhost:8080/api/v1/publishers \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Media","code":"DEMO001","revenueShare":0.7}'
# Expected: {"code":0,"data":1,"message":"ok"}
# Note the api_token in response (auto-generated UUID)

curl http://localhost:8080/api/v1/publishers
# Expected: list with 1 publisher
```

- [ ] **Step 9: Commit**

```bash
git add ad-platform/backend/src/main/java/com/ad/entity/Publisher.java
git add ad-platform/backend/src/main/java/com/ad/entity/AdSlot.java
git add ad-platform/backend/src/main/java/com/ad/dto/PublisherDTO.java
git add ad-platform/backend/src/main/java/com/ad/dto/PublisherCreateDTO.java
git add ad-platform/backend/src/main/java/com/ad/dto/AdSlotDTO.java
git add ad-platform/backend/src/main/java/com/ad/dto/AdSlotCreateDTO.java
git add ad-platform/backend/src/main/java/com/ad/mapper/PublisherMapper.java
git add ad-platform/backend/src/main/java/com/ad/mapper/AdSlotMapper.java
git add ad-platform/backend/src/main/java/com/ad/service/PublisherService.java
git add ad-platform/backend/src/main/java/com/ad/service/impl/PublisherServiceImpl.java
git add ad-platform/backend/src/main/java/com/ad/service/AdSlotService.java
git add ad-platform/backend/src/main/java/com/ad/service/impl/AdSlotServiceImpl.java
git add ad-platform/backend/src/main/java/com/ad/controller/PublisherController.java
git add ad-platform/backend/src/main/java/com/ad/controller/AdSlotController.java
git commit -m "feat(management): add publisher and ad slot CRUD"
```

---

### Task 5: Bidding — ADX Engine + DSP Decision Engine

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/AdxEngine.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/DspDecisionEngine.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/StrategyMatcher.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/Pricer.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/model/CampaignConfig.java`
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/handler/SspHandler.java` (replace stub `callAdx`)

**Interfaces:**
- Consumes: BidRequest from SSP Handler
- Produces: BidResponse with real strategy matching, audience check, and pricing

- [ ] **Step 1: Create CampaignConfig.java**

```java
package com.ad.bidding.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CampaignConfig {
    private Long id;
    private Long strategyId;
    private String name;
    private String channel;
    private BigDecimal bidPrice;
    private String bidType;         // OCPM / CPC / CPM
    private BigDecimal budgetDaily;
    private BigDecimal targetCpa;
    private BigDecimal bidRate;     // 出价系数 (e.g. 0.4 = CPA × 0.4)
    private List<String> audienceCodes;  // 关联的人群编码
    private List<MaterialOption> materials;
    private Integer frequencyCap;   // 单用户日曝光上限
    private String timeRange;       // 时段 "09:00-23:00"

    @Data
    @Builder
    public static class MaterialOption {
        private String code;
        private String url;
        private int width;
        private int height;
        private int priority;
    }
}
```

- [ ] **Step 2: Create StrategyMatcher.java**

```java
package com.ad.bidding.engine;

import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.CampaignConfig;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

/**
 * 策略匹配器 — 按优先级遍历策略，首条命中终止。
 *
 * Phase 1: hardcoded campaign configs matching the 6 RTB strategies.
 * Phase 2: config loaded from Redis, synced from Management DB.
 */
@Slf4j
public class StrategyMatcher {

    private final List<CampaignConfig> campaigns;

    public StrategyMatcher() {
        this.campaigns = buildDefaultCampaigns();
    }

    /**
     * Match a bid request against all available campaigns.
     * Returns the highest-priority matching campaign, or null if none match.
     */
    public CampaignConfig match(BidRequest req, Map<String, Long> freqMap) {
        for (CampaignConfig campaign : campaigns) {
            if (matches(req, campaign, freqMap)) {
                log.debug("Matched campaign: {} (strategy {})", campaign.getName(), campaign.getStrategyId());
                return campaign;
            }
        }
        return null;
    }

    private boolean matches(BidRequest req, CampaignConfig campaign, Map<String, Long> freqMap) {
        // 1. Channel check
        if (!"ALL".equals(campaign.getChannel()) && !matchesChannel(req, campaign.getChannel())) {
            return false;
        }

        // 2. Time range check
        if (campaign.getTimeRange() != null && !isInTimeRange(campaign.getTimeRange())) {
            return false;
        }

        // 3. Audience check (Phase 1: simplified — check against Redis)
        // In Phase 1, we simulate audience match:
        // Strategy 1 (high-value): device IDs starting with "hv-" match
        // Strategy 4 (retargeting): device IDs starting with "rt-" match
        if (campaign.getStrategyId() == 1 && !req.getDeviceId().startsWith("hv-")
                && !req.getDeviceId().startsWith("rt-")) {
            return false;
        }
        if (campaign.getStrategyId() == 4 && !req.getDeviceId().startsWith("rt-")) {
            return false;
        }

        // 4. Frequency cap check
        if (campaign.getFrequencyCap() != null && campaign.getFrequencyCap() > 0) {
            Long todayCount = freqMap.getOrDefault(freqKey(req.getDeviceId(), campaign.getId()), 0L);
            if (todayCount >= campaign.getFrequencyCap()) {
                log.debug("Frequency cap hit for device {} on campaign {}", req.getDeviceId(), campaign.getId());
                return false;
            }
        }

        return true;
    }

    private boolean matchesChannel(BidRequest req, String channel) {
        // Phase 1: simplified — check device type against channel preference
        // Strategy 2 (B站 new user): prefers mobile
        // Strategy 3 (竞品截流): prefers all
        // Strategy 5 (通投): all
        return true;
    }

    private boolean isInTimeRange(String timeRange) {
        try {
            String[] parts = timeRange.split("-");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            LocalTime now = LocalTime.now();
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            return true;
        }
    }

    private String freqKey(String deviceId, Long campaignId) {
        String date = java.time.LocalDate.now().toString();
        return "freq:" + campaignId + ":" + deviceId + ":" + date;
    }

    /**
     * Build default 6 RTB campaigns as defined in the spec.
     * Priority order (matched in this order):
     *   Strategy 4 (弃单重定向) → Strategy 3 (竞品截流) → Strategy 1 (高价值人群)
     *   → Strategy 2 (新品破圈) → Strategy 5 (智能通投) → Strategy 6 (兜底)
     */
    private List<CampaignConfig> buildDefaultCampaigns() {
        List<CampaignConfig> list = new ArrayList<>();

        // Strategy 4: 弃单重定向强转化 (priority 1)
        list.add(CampaignConfig.builder()
                .id(4L).strategyId(4L).name("弃单重定向强转化")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("200")).bidRate(new BigDecimal("0.6"))
                .frequencyCap(5).timeRange("00:00-23:59")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C006").url("https://cdn.adx.com/materials/c006.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        // Strategy 3: 竞品截流抢夺 (priority 2)
        list.add(CampaignConfig.builder()
                .id(3L).strategyId(3L).name("竞品截流抢夺")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("250")).bidRate(new BigDecimal("0.5"))
                .frequencyCap(8).timeRange("08:00-23:00")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C008").url("https://cdn.adx.com/materials/c008.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        // Strategy 1: 高价值人群精准转化 (priority 3)
        list.add(CampaignConfig.builder()
                .id(1L).strategyId(1L).name("高价值人群精准转化")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("250")).bidRate(new BigDecimal("0.4"))
                .frequencyCap(10).timeRange("09:00-23:00")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C007").url("https://cdn.adx.com/materials/c007.jpg").width(320).height(480).priority(1).build(),
                        CampaignConfig.MaterialOption.builder().code("C002").url("https://cdn.adx.com/materials/c002.jpg").width(320).height(480).priority(2).build()
                ))
                .build());

        // Strategy 2: 新品破圈拉新 (priority 4)
        list.add(CampaignConfig.builder()
                .id(2L).strategyId(2L).name("新品破圈拉新")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("300")).bidRate(new BigDecimal("0.25"))
                .frequencyCap(5).timeRange("08:00-22:00")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C001").url("https://cdn.adx.com/materials/c001.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        // Strategy 5: 智能通投探索 (priority 5)
        list.add(CampaignConfig.builder()
                .id(5L).strategyId(5L).name("智能通投探索")
                .channel("ALL").bidType("OCPM")
                .targetCpa(new BigDecimal("350")).bidRate(new BigDecimal("0.15"))
                .frequencyCap(10).timeRange("00:00-23:59")
                .materials(Arrays.asList(
                        CampaignConfig.MaterialOption.builder().code("C007").url("https://cdn.adx.com/materials/c007.jpg").width(320).height(480).priority(1).build()
                ))
                .build());

        // Strategy 6: 兜底不出价 (no campaign — returns null from matcher)
        return list;
    }
}
```

- [ ] **Step 3: Create Pricer.java**

```java
package com.ad.bidding.engine;

import com.ad.bidding.model.CampaignConfig;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 出价计算器 — 基于策略配置的目标CPA × 出价系数
 */
@Slf4j
public class Pricer {

    /**
     * Calculate bid price (CPM equivalent).
     *
     * Formula: bidPrice = targetCPA × bidRate × 1000
     * (Converting from CPA target to CPM bid — simplified for Phase 1)
     *
     * @param campaign matched campaign config
     * @return calculated CPM bid price
     */
    public BigDecimal calculateBid(CampaignConfig campaign) {
        if (campaign.getBidRate() == null || campaign.getTargetCpa() == null) {
            return BigDecimal.ZERO;
        }

        // CPM bid = targetCPA × bidRate × 1000 / 1000 (adjust by eCPM factor)
        // Simplified: CPM_bid = targetCPA × bidRate × CTR_estimate × 1000
        // For Phase 1: use flat conversion factor of 0.01 (assumes ~1% CTR)
        BigDecimal cpmBid = campaign.getTargetCpa()
                .multiply(campaign.getBidRate())
                .multiply(new BigDecimal("10"))    // simplified eCPM factor
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Bid calculated: campaign={}, targetCPA={}, rate={}, cpmBid={}",
                campaign.getName(), campaign.getTargetCpa(), campaign.getBidRate(), cpmBid);

        return cpmBid;
    }
}
```

- [ ] **Step 4: Create DspDecisionEngine.java**

```java
package com.ad.bidding.engine;

import com.ad.bidding.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * DSP Decision Engine — orchestrates the full decision pipeline:
 * 1. Strategy matching
 * 2. Audience check (via StrategyMatcher)
 * 3. Frequency cap check (via StrategyMatcher)
 * 4. Budget check
 * 5. Pricing
 * 6. Material matching
 * 7. Response assembly
 */
public class DspDecisionEngine {

    private final StrategyMatcher strategyMatcher;
    private final Pricer pricer;
    private final BudgetEngine budgetEngine;

    public DspDecisionEngine() {
        this.strategyMatcher = new StrategyMatcher();
        this.pricer = new Pricer();
        this.budgetEngine = new BudgetEngine();
    }

    /**
     * Main entry point — decide whether to bid on this request.
     * Called by ADX Engine.
     */
    public BidResponse decide(BidRequest req, BigDecimal floorPrice) {
        long startNanos = System.nanoTime();

        // 1. Strategy matching (includes audience + frequency cap)
        Map<String, Long> freqMap = new HashMap<>(); // Phase 1: empty, Phase 2: from Redis
        CampaignConfig matched = strategyMatcher.match(req, freqMap);

        if (matched == null) {
            return BidResponse.builder()
                    .win(false)
                    .price(BigDecimal.ZERO)
                    .nbr(2)  // nbr=2: no matching campaign
                    .build();
        }

        // 2. Budget check
        if (!budgetEngine.hasBudget(matched.getId())) {
            return BidResponse.builder()
                    .win(false)
                    .price(BigDecimal.ZERO)
                    .nbr(1)  // nbr=1: budget exhausted
                    .strategyId(matched.getStrategyId())
                    .campaignId(matched.getId())
                    .build();
        }

        // 3. Calculate bid price
        BigDecimal bidPrice = pricer.calculateBid(matched);

        // 4. Check floor price
        if (bidPrice.compareTo(floorPrice) < 0) {
            return BidResponse.builder()
                    .win(false)
                    .price(bidPrice)
                    .nbr(3)  // nbr=3: below floor
                    .strategyId(matched.getStrategyId())
                    .campaignId(matched.getId())
                    .build();
        }

        // 5. Deduct budget
        budgetEngine.deduct(matched.getId(), bidPrice);

        // 6. Select material
        CampaignConfig.MaterialOption material = selectMaterial(matched, req.getWidth(), req.getHeight());

        // 7. Build tracking URLs
        String baseTrackUrl = "http://localhost:9090/track";
        String deviceId = req.getDeviceId() != null ? req.getDeviceId() : req.getOaid();

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        return BidResponse.builder()
                .win(true)
                .price(bidPrice)
                .adMaterialUrl(material.getUrl())
                .landingUrl("https://lumi.example.com/product?utm_source=adx&cid=" + matched.getId())
                .trackImpUrl(baseTrackUrl + "/imp/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId)
                .trackClickUrl(baseTrackUrl + "/click/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId)
                .nbr(0)
                .strategyId(matched.getStrategyId())
                .campaignId(matched.getId())
                .impTrackers(Arrays.asList(baseTrackUrl + "/imp/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId))
                .clickTrackers(Arrays.asList(baseTrackUrl + "/click/" + matched.getId() + "/" + matched.getStrategyId() + "/" + deviceId))
                .latencyMs((int) elapsedMs)
                .build();
    }

    private CampaignConfig.MaterialOption selectMaterial(CampaignConfig campaign, int width, int height) {
        // Phase 1: return first material
        // Phase 2: match by size, then by priority
        if (campaign.getMaterials() == null || campaign.getMaterials().isEmpty()) {
            return CampaignConfig.MaterialOption.builder()
                    .code("default").url("https://cdn.adx.com/materials/default.jpg")
                    .width(width).height(height).priority(0).build();
        }
        return campaign.getMaterials().get(0);
    }

    // Called from SSP handler to record exposures/clicks for frequency counting
    public void recordEvent(String deviceId, Long campaignId, String eventType) {
        // Phase 2: write to Redis frequency counter
    }
}
```

- [ ] **Step 5: Create BudgetEngine.java**

```java
package com.ad.bidding.engine;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预算引擎 — 原子预算扣减 + 熔断检查
 *
 * Phase 1: in-memory ConcurrentHashMap
 * Phase 2: Redis DECRBY atomic ops
 */
@Slf4j
public class BudgetEngine {

    // campaignId -> remaining daily budget
    private final ConcurrentHashMap<Long, BigDecimal> budgets = new ConcurrentHashMap<>();

    public BudgetEngine() {
        // Initialize default budgets for demo
        budgets.put(1L, new BigDecimal("5000.00"));   // Strategy 1
        budgets.put(2L, new BigDecimal("3000.00"));   // Strategy 2
        budgets.put(3L, new BigDecimal("4000.00"));   // Strategy 3
        budgets.put(4L, new BigDecimal("3000.00"));   // Strategy 4
        budgets.put(5L, new BigDecimal("5000.00"));   // Strategy 5
    }

    /**
     * Check if campaign has remaining budget.
     */
    public boolean hasBudget(Long campaignId) {
        BigDecimal remaining = budgets.get(campaignId);
        return remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Deduct bid cost from campaign budget.
     * In Phase 2, this becomes Redis DECRBY.
     * @return remaining budget after deduction
     */
    public BigDecimal deduct(Long campaignId, BigDecimal cost) {
        BigDecimal newBudget = budgets.computeIfPresent(campaignId, (id, remaining) -> {
            BigDecimal after = remaining.subtract(cost);
            return after.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : after;
        });
        if (newBudget != null && newBudget.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Budget exhausted for campaign {}", campaignId);
        }
        return newBudget;
    }

    /**
     * Reset daily budgets (call from Management API or scheduled task).
     */
    public void resetDaily() {
        budgets.put(1L, new BigDecimal("5000.00"));
        budgets.put(2L, new BigDecimal("3000.00"));
        budgets.put(3L, new BigDecimal("4000.00"));
        budgets.put(4L, new BigDecimal("3000.00"));
        budgets.put(5L, new BigDecimal("5000.00"));
    }
}
```

- [ ] **Step 6: Create AdxEngine.java**

```java
package com.ad.bidding.engine;

import com.ad.bidding.BiddingApplication;
import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.BidResponse;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ADX Engine — 广告交易引擎
 *
 * Responsibilities:
 * 1. Look up ad slot config (floor price, blocked categories)
 * 2. Call DSP Decision Engine
 * 3. Verify bid ≥ floor price
 * 4. Record bid log
 */
@Slf4j
public class AdxEngine {

    private final DspDecisionEngine dspEngine;
    private final AtomicLong bidLogCounter = new AtomicLong(0);

    public AdxEngine() {
        this.dspEngine = new DspDecisionEngine();
    }

    /**
     * Process one bid request through the ADX.
     * Called from SSP Gateway.
     */
    public BidResponse process(BidRequest req) {
        long startNanos = System.nanoTime();

        // 1. Get ad slot config
        // Phase 1: hardcoded; Phase 2: from Redis cache
        BigDecimal floorPrice = getFloorPrice(req.getAdSlotId());

        // 2. Check blocked categories (Phase 1: skip)
        // 3. Call DSP Decision Engine
        BidResponse response = dspEngine.decide(req, floorPrice);

        // 4. Log the bid
        recordBidLog(req, response, floorPrice, (System.nanoTime() - startNanos) / 1_000_000);

        return response;
    }

    private BigDecimal getFloorPrice(Long adSlotId) {
        // Phase 1: default floor price
        // Phase 2: read from Redis Hash hget("ad_slot:1", "floor_price")
        return new BigDecimal("0.01");
    }

    private void recordBidLog(BidRequest req, BidResponse resp, BigDecimal floorPrice, long latencyMs) {
        // Phase 1: log only
        // Phase 2: batch write to MySQL ad_bid_log table
        if (resp.isWin()) {
            log.info("BID_WIN slot={} campaign={} price={} floor={} latency={}ms",
                    req.getAdSlotId(), resp.getCampaignId(), resp.getPrice(), floorPrice, latencyMs);
        } else {
            log.info("BID_LOSE slot={} nbr={} latency={}ms",
                    req.getAdSlotId(), resp.getNbr(), latencyMs);
        }
        bidLogCounter.incrementAndGet();
    }

    public long getTotalBidCount() {
        return bidLogCounter.get();
    }
}
```

- [ ] **Step 7: Wire ADX Engine into SspHandler**

Replace the `callAdx()` stub method in `SspHandler.java`:

```java
// Add as a field
private final AdxEngine adxEngine = new AdxEngine();

// Replace the callAdx method:
private BidResponse callAdx(BidRequest req) {
    return adxEngine.process(req);
}
```

- [ ] **Step 8: Test the full decision pipeline**

Run Bidding Service, then test with different device ID prefixes:

```bash
# Test 1: high-value device → should match Strategy 1
curl -X POST http://localhost:9090/ad/request \
  -H "Content-Type: application/json" \
  -H "X-Auth-Token: demo-token-001" \
  -d '{"device_id":"hv-user-001","ip":"192.168.1.1","ua":"Mozilla/5.0 (iPhone)","ad_slot_code":"SLOT_001","width":320,"height":480}'

# Test 2: retargeting device → should match Strategy 4 (highest priority)
curl -X POST http://localhost:9090/ad/request \
  -H "Content-Type: application/json" \
  -H "X-Auth-Token: demo-token-001" \
  -d '{"device_id":"rt-user-001","ip":"192.168.1.1","ua":"Mozilla/5.0 (Android)","ad_slot_code":"SLOT_001","width":320,"height":480}'

# Test 3: unknown device → no match (falls to nbr=2)
curl -X POST http://localhost:9090/ad/request \
  -H "Content-Type: application/json" \
  -H "X-Auth-Token: demo-token-001" \
  -d '{"device_id":"unknown-user-001","ip":"10.0.0.1","ua":"Mozilla/5.0","ad_slot_code":"SLOT_001","width":320,"height":480}'
```

Expected: Test 1 returns ad (win), Test 2 returns ad (win, higher priority), Test 3 returns `{"code":1,"msg":"no bid"}`

- [ ] **Step 9: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/model/CampaignConfig.java
# plus modified SspHandler.java
git commit -m "feat(bidding): add ADX Engine and DSP Decision Engine with strategy matching"
```

---

### Task 6: Bidding — Tracking Server

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/handler/TrackingHandler.java`
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/tracker/EventLogger.java`
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/verticle/MainVerticle.java` (mount tracking routes)

**Interfaces:**
- Consumes: HTTP GET requests from media/advertiser tracking pixels
- Produces: 1x1 GIF responses, access-log based event records

- [ ] **Step 1: Create EventLogger.java**

```java
package com.ad.bidding.tracker;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 事件日志记录器 — 记录曝光/点击/转化到日志文件
 *
 * Phase 1: log-based, collected from log files
 * Phase 2: write to MySQL ad_tracking_log via Data Sync Module
 */
@Slf4j
public class EventLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Record a tracking event.
     * Output format (tab-separated for easy log parsing):
     *   timestamp\ttype\tcampaign_id\tstrategy_id\tdevice_id
     */
    public void logEvent(String type, Long campaignId, Long strategyId, String deviceId) {
        String now = LocalDateTime.now().format(FORMATTER);
        String line = String.format("%s\t%s\t%d\t%d\t%s",
                now, type, campaignId, strategyId, deviceId != null ? deviceId : "unknown");
        // Use a dedicated logger or write to tracking-specific file
        log.info("TRACK|{}", line);
    }
}
```

- [ ] **Step 2: Create TrackingHandler.java**

```java
package com.ad.bidding.handler;

import com.ad.bidding.tracker.EventLogger;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Tracking Server — 曝光/点击/转化监播
 *
 * Routes:
 *   GET /track/imp/:campaignId/:strategyId/:deviceId    — 曝光
 *   GET /track/click/:campaignId/:strategyId/:deviceId  — 点击
 *   POST /track/conv                                     — 转化(广告主回传)
 *   GET /track/landing/:campaignId/:strategyId/:deviceId — 落地页跳转
 */
@Slf4j
public class TrackingHandler implements Handler<RoutingContext> {

    private static final Buffer PIXEL_GIF = createPixelGif();
    private final EventLogger eventLogger = new EventLogger();

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

        eventLogger.logEvent(type,
                campaignId != null ? Long.parseLong(campaignId) : 0,
                strategyId != null ? Long.parseLong(strategyId) : 0,
                deviceId);

        if ("landing".equals(type)) {
            // Redirect to landing page
            ctx.response()
                    .setStatusCode(302)
                    .putHeader("Location", "https://lumi.example.com/product?utm_source=adx&track=1")
                    .end();
        } else {
            // Return 1x1 transparent GIF for tracking pixel
            ctx.response()
                    .putHeader("Content-Type", "image/gif")
                    .putHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .putHeader("Pragma", "no-cache")
                    .putHeader("Expires", "0")
                    .end(PIXEL_GIF);
        }
    }

    // 1x1 transparent GIF bytes (43 bytes)
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
```

- [ ] **Step 3: Mount tracking routes in MainVerticle**

In `MainVerticle.java`, inside `startHttpServer()`, after the SSP route:

```java
// Tracking routes
TrackingHandler trackingHandler = new TrackingHandler();
router.get("/track/imp/:campaignId/:strategyId/:deviceId").handler(trackingHandler);
router.get("/track/click/:campaignId/:strategyId/:deviceId").handler(trackingHandler);
router.post("/track/conv").handler(trackingHandler);
router.get("/track/landing/:campaignId/:strategyId/:deviceId").handler(trackingHandler);
```

- [ ] **Step 4: Test tracking**

```bash
# Test impression tracking (returns 1x1 GIF)
curl -v http://localhost:9090/track/imp/1/1/test-device-001
# Expected: 200 OK, Content-Type: image/gif, 43 bytes

# Test landing redirect
curl -v http://localhost:9090/track/landing/1/1/test-device-001
# Expected: 302 Found → Location: https://lumi.example.com/...
```

- [ ] **Step 5: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/handler/TrackingHandler.java
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/tracker/
# plus modified MainVerticle.java
git commit -m "feat(bidding): add Tracking Server with imp/click/conv/landing endpoints"
```

---

### Task 7: Management — Strategy RTB Config Extension

**Files:**
- Modify: `ad-platform/backend/src/main/java/com/ad/dto/StrategyDTO.java` (add RTB config fields)
- Modify: `ad-platform/backend/src/main/java/com/ad/dto/StrategyCreateDTO.java` (add RTB config fields)
- Modify: `ad-platform/backend/src/main/java/com/ad/entity/Strategy.java` (add RTB config fields if needed)
- Modify: `ad-platform/backend/src/main/resources/db/migration-v2.sql` (add strategy RTB columns)
- Create: `ad-platform/backend/src/main/java/com/ad/controller/StrategyDeployController.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/StrategyDeployService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/StrategyDeployServiceImpl.java`

**Interfaces:**
- Consumes: existing Strategy entity/DTO patterns
- Produces: Strategy deploy to Redis + API to push strategy config to Bidding Service

- [ ] **Step 1: Add RTB config columns to ad_strategy table**

Add this to `migration-v2.sql`:

```sql
-- Extend ad_strategy with RTB bidding config
ALTER TABLE ad_strategy
    ADD COLUMN bid_rate         DECIMAL(5,2) DEFAULT NULL COMMENT '出价系数(CPA×bidRate)',
    ADD COLUMN frequency_cap    INT          DEFAULT 10   COMMENT '单用户日曝光上限',
    ADD COLUMN time_range       VARCHAR(11)  DEFAULT NULL COMMENT '投放时段 09:00-23:00',
    ADD COLUMN rtb_status       TINYINT      DEFAULT 0    COMMENT 'RTB状态 0-未上线 1-已上线';
```

- [ ] **Step 2: Extend StrategyCreateDTO.java**

```java
// Add these fields to existing StrategyCreateDTO:
@Data
public class StrategyCreateDTO {
    // ... existing fields ...

    // New RTB fields
    private BigDecimal bidRate;         // 出价系数
    private Integer frequencyCap;       // 频控上限
    private String timeRange;           // 投放时段
    private List<Long> publisherIds;    // 关联的媒体ID
    private List<Long> adSlotIds;       // 关联的广告位ID
}
```

- [ ] **Step 3: Create StrategyDeployController.java**

```java
package com.ad.controller;

import com.ad.common.Result;
import com.ad.service.StrategyDeployService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class StrategyDeployController {

    private final StrategyDeployService deployService;

    /**
     * Deploy a strategy to the RTB pipeline.
     * Writes strategy config to Redis for Bidding Service to pick up.
     */
    @PostMapping("/{id}/deploy")
    public Result<Void> deploy(@PathVariable Long id) {
        deployService.deployToRtb(id);
        return Result.ok();
    }

    /**
     * Remove a strategy from the RTB pipeline.
     */
    @PostMapping("/{id}/undeploy")
    public Result<Void> undeploy(@PathVariable Long id) {
        deployService.undeployFromRtb(id);
        return Result.ok();
    }

    /**
     * Get the RTB status of a strategy.
     */
    @GetMapping("/{id}/deploy-status")
    public Result<Boolean> getDeployStatus(@PathVariable Long id) {
        return Result.ok(deployService.isDeployed(id));
    }
}
```

- [ ] **Step 4: Create StrategyDeployService.java**

```java
package com.ad.service;

public interface StrategyDeployService {
    void deployToRtb(Long strategyId);
    void undeployFromRtb(Long strategyId);
    boolean isDeployed(Long strategyId);
}
```

- [ ] **Step 5: Create StrategyDeployServiceImpl.java**

```java
package com.ad.service.impl;

import com.ad.entity.Strategy;
import com.ad.mapper.StrategyMapper;
import com.ad.service.StrategyDeployService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyDeployServiceImpl implements StrategyDeployService {

    private final StrategyMapper strategyMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RTB_STRATEGY_KEY = "rtb:strategy:";

    @Override
    @Transactional
    public void deployToRtb(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("Strategy not found: " + strategyId);
        }

        // Validate RTB config
        if (strategy.getTargetCpa() == null) {
            throw new RuntimeException("target_cpa required for RTB deployment");
        }

        // Write to Redis Hash (Bidding Service reads this)
        Map<String, String> config = new HashMap<>();
        config.put("id", String.valueOf(strategy.getId()));
        config.put("name", strategy.getName());
        config.put("targetCpa", strategy.getTargetCpa().toString());
        config.put("bidRate", strategy.getBidRate() != null ? strategy.getBidRate().toString() : "0.3");
        config.put("frequencyCap", strategy.getFrequencyCap() != null ? String.valueOf(strategy.getFrequencyCap()) : "10");
        config.put("timeRange", strategy.getTimeRange() != null ? strategy.getTimeRange() : "00:00-23:59");

        redisTemplate.opsForHash().putAll(RTB_STRATEGY_KEY + strategyId, config);

        // Publish config change to Bidding Service via Redis Pub/Sub
        redisTemplate.convertAndSend("config:changed",
                "strategy:deploy:" + strategyId);

        log.info("Strategy {} deployed to RTB pipeline", strategyId);
    }

    @Override
    @Transactional
    public void undeployFromRtb(Long strategyId) {
        redisTemplate.delete(RTB_STRATEGY_KEY + strategyId);
        redisTemplate.convertAndSend("config:changed",
                "strategy:undeploy:" + strategyId);
        log.info("Strategy {} removed from RTB pipeline", strategyId);
    }

    @Override
    public boolean isDeployed(Long strategyId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RTB_STRATEGY_KEY + strategyId));
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add ad-platform/backend/src/main/resources/db/migration-v2.sql
git add ad-platform/backend/src/main/java/com/ad/dto/StrategyCreateDTO.java
git add ad-platform/backend/src/main/java/com/ad/controller/StrategyDeployController.java
git add ad-platform/backend/src/main/java/com/ad/service/StrategyDeployService.java
git add ad-platform/backend/src/main/java/com/ad/service/impl/StrategyDeployServiceImpl.java
git commit -m "feat(management): add strategy deploy to RTB via Redis Pub/Sub"
```

---

### Task 8: Mock Media Simulator + Docker Compose

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/simulator/MediaSimulator.java`
- Create: `ad-platform/docker-compose.yml`
- Create: `ad-platform/run-demo.sh`

**Interfaces:**
- Consumes: Bidding Service running on port 9090
- Produces: Press-key demo showing full RTB pipeline working end-to-end

- [ ] **Step 1: Create docker-compose.yml**

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: ad-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: ad_platform
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./backend/src/main/resources/db/init-schema.sql:/docker-entrypoint-initdb.d/01-init-schema.sql
      - ./backend/src/main/resources/db/seed-data-mock.sql:/docker-entrypoint-initdb.d/02-seed-data.sql
      - ./backend/src/main/resources/db/migration-v2.sql:/docker-entrypoint-initdb.d/03-migration-v2.sql
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:7-alpine
    container_name: ad-redis
    ports:
      - "6379:6379"

volumes:
  mysql-data:
```

- [ ] **Step 2: Create MediaSimulator.java**

```java
package com.ad.bidding.simulator;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟媒体工具 — 发送模拟广告请求验证全链路
 *
 * Simulates multiple device types and user segments to exercise
 * different DSP strategies.
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

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx);
        Random random = new Random();
        AtomicInteger winCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        int totalRequests = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        int concurrency = args.length > 1 ? Integer.parseInt(args[1]) : 10;

        log.info("Starting media simulator: {} requests, concurrency {}", totalRequests, concurrency);

        for (int i = 0; i < totalRequests; i++) {
            int idx = i;
            String prefix = DEVICE_PREFIXES[random.nextInt(DEVICE_PREFIXES.length)];
            String deviceId = prefix + "sim-" + idx + "-" + System.nanoTime();
            String ua = USER_AGENTS[random.nextInt(USER_AGENTS.length)];

            JsonObject body = new JsonObject()
                    .put("device_id", deviceId)
                    .put("ip", "192.168." + random.nextInt(256) + "." + random.nextInt(256))
                    .put("ua", ua)
                    .put("ad_slot_code", "SLOT_001")
                    .put("width", 320)
                    .put("height", 480)
                    .put("app_package", "com.demo.media");

            client.postAbs(SSP_URL)
                    .putHeader("Content-Type", "application/json")
                    .putHeader("X-Auth-Token", AUTH_TOKEN)
                    .sendBuffer(Buffer.buffer(body.encode()))
                    .onSuccess(resp -> {
                        int total = totalCount.incrementAndGet();
                        if (resp.statusCode() == 200) {
                            JsonObject result = resp.bodyAsJsonObject();
                            if (result.getInteger("code") == 0) {
                                winCount.incrementAndGet();
                            }
                        }
                        if (total % 20 == 0 || total == totalRequests) {
                            long elapsed = System.currentTimeMillis() - startTime;
                            double qps = total / (elapsed / 1000.0);
                            log.info("Progress: {}/{} requests, win rate: {:.1f}%, QPS: {:.0f}",
                                    total, totalRequests,
                                    (double) winCount.get() / total * 100,
                                    qps);
                        }
                        if (total >= totalRequests) {
                            long elapsed = System.currentTimeMillis() - startTime;
                            double qps = total / (elapsed / 1000.0);
                            log.info("=== SIMULATION COMPLETE ===");
                            log.info("Total requests: {}", total);
                            log.info("Wins: {}", winCount.get());
                            log.info("Win rate: {:.1f}%", (double) winCount.get() / total * 100);
                            log.info("Elapsed: {}s", elapsed / 1000);
                            log.info("Average QPS: {:.0f}", qps);
                            vertx.close();
                        }
                    })
                    .onFailure(err -> {
                        int total = totalCount.incrementAndGet();
                        log.warn("Request {} failed: {}", idx, err.getMessage());
                        if (total >= totalRequests) {
                            vertx.close();
                        }
                    });

            // Rate-limit to achieve target concurrency
            if (i % concurrency == 0 && i > 0) {
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
        }
    }
}
```

- [ ] **Step 3: Create run-demo.sh**

```bash
#!/bin/bash
# LUMI ADX Phase 1 Demo Script
# Starts services and runs simulator

set -e

echo "=== LUMI ADX Phase 1 Demo ==="

# 1. Start infrastructure
echo "Starting MySQL + Redis..."
cd "$(dirname "$0")"
docker compose up -d mysql redis
echo "Waiting for MySQL..."
sleep 10

# 2. Initialize DB (already done via docker-entrypoint-initdb.d)

# 3. Start Management Service
echo "Starting Management Service..."
cd backend
mvn spring-boot:run -q &
MANAGEMENT_PID=$!
cd ..

# 4. Start Bidding Service
echo "Starting Bidding Service..."
cd bidding-service
mvn compile exec:java -Dexec.mainClass="com.ad.bidding.BiddingApplication" -q &
BIDDING_PID=$!
cd ..

echo "Waiting for services to start..."
sleep 15

# 5. Create a test publisher and ad slot via Management API
echo "Creating test publisher..."
curl -s -X POST http://localhost:8080/api/v1/publishers \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Media","code":"DEMO001","revenueShare":0.7}'

echo ""
echo "Creating test ad slot..."
curl -s -X POST http://localhost:8080/api/v1/ad-slots \
  -H "Content-Type: application/json" \
  -d '{"publisherId":1,"name":"Banner 320x480","code":"SLOT_001","slotType":1,"width":320,"height":480,"floorPrice":0.01}'

echo ""
echo "=== Services Ready ==="
echo "  Management: http://localhost:8080"
echo "  Bidding:    http://localhost:9090"
echo ""

# 6. Run simulator
echo "Running simulator (100 requests)..."
cd bidding-service
mvn compile exec:java -Dexec.mainClass="com.ad.bidding.simulator.MediaSimulator" -Dexec.args="100 10" -q
cd ..

# 7. Cleanup
echo "Stopping services..."
kill $MANAGEMENT_PID $BIDDING_PID 2>/dev/null
echo "Demo complete."
```

- [ ] **Step 4: Verify simulator works**

Run: `chmod +x ad-platform/run-demo.sh && cd ad-platform && ./run-demo.sh`
Expected: Services start, simulator sends 100 requests, win rate > 0%.

- [ ] **Step 5: Commit**

```bash
git add ad-platform/docker-compose.yml
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/simulator/
git add ad-platform/run-demo.sh
git commit -m "feat: add docker-compose, media simulator, and demo script for Phase 1"
```

---

### Task 9: Integration Test + 200 QPS Baseline

**Files:**
- Create: `ad-platform/bidding-service/src/test/java/com/ad/bidding/handler/SspHandlerTest.java`
- Create: `ad-platform/bidding-service/src/test/java/com/ad/bidding/engine/DspDecisionEngineTest.java`
- Create: `ad-platform/backend/src/test/java/com/ad/controller/PublisherControllerTest.java`

**Interfaces:**
- Consumes: all services running
- Produces: verified end-to-end chain, 200 QPS baseline

- [ ] **Step 1: Create DspDecisionEngineTest.java**

```java
package com.ad.bidding.engine;

import com.ad.bidding.model.BidRequest;
import com.ad.bidding.model.BidResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DspDecisionEngineTest {

    private DspDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DspDecisionEngine();
    }

    @Test
    void shouldBidForHighValueUser() {
        BidRequest req = new BidRequest();
        req.setDeviceId("hv-user-001");
        req.setAdSlotId(1L);
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("0.01"));

        assertTrue(resp.isWin());
        assertEquals(1L, resp.getStrategyId().longValue());
        assertTrue(resp.getPrice().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(resp.getAdMaterialUrl());
        assertNotNull(resp.getTrackImpUrl());
    }

    @Test
    void shouldPreferRetargetingOverHighValue() {
        BidRequest req = new BidRequest();
        req.setDeviceId("rt-user-001");
        req.setAdSlotId(1L);
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("0.01"));

        assertTrue(resp.isWin());
        assertEquals(4L, resp.getStrategyId().longValue());  // Retargeting should win
    }

    @Test
    void shouldNotBidForUnknownUser() {
        BidRequest req = new BidRequest();
        req.setDeviceId("unknown-user-001");
        req.setAdSlotId(1L);
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("0.01"));

        assertFalse(resp.isWin());
        assertEquals(2, resp.getNbr());  // nbr=2: no match
    }

    @Test
    void shouldRespectFloorPrice() {
        BidRequest req = new BidRequest();
        req.setDeviceId("hv-user-001");
        req.setAdSlotId(1L);
        req.setWidth(320);
        req.setHeight(480);

        BidResponse resp = engine.decide(req, new BigDecimal("99999.00"));  // Impossible floor

        assertFalse(resp.isWin());
        assertEquals(3, resp.getNbr());  // nbr=3: below floor
    }
}
```

- [ ] **Step 2: Run unit tests**

Run: `cd ad-platform/bidding-service && mvn test`
Expected: All 4 tests pass.

- [ ] **Step 3: Run 200 QPS benchmark**

```bash
# Start Bidding Service
cd ad-platform/bidding-service && mvn compile exec:java -Dexec.mainClass="com.ad.bidding.BiddingApplication" &
sleep 5

# Run simulator with 200 concurrent requests
mvn compile exec:java -Dexec.mainClass="com.ad.bidding.simulator.MediaSimulator" -Dexec.args="1000 50"

# Expected output:
# QPS: ~200+
# Win rate: ~50-60% (depends on device ID distribution)
# P95 latency: <50ms
```

- [ ] **Step 4: Commit**

```bash
git add ad-platform/bidding-service/src/test/
git commit -m "test(bidding): add unit tests and 200 QPS baseline verification"
```

---

## Phase 1 Completion Checklist

- [ ] Task 1: Multi-module restructure + DB migration — builds and SQL executes
- [ ] Task 2: Bidding Service skeleton — HTTP server starts on port 9090
- [ ] Task 3: SSP Gateway — `/ad/request` validates and enriches
- [ ] Task 4: Management Publisher + AdSlot CRUD — full API working
- [ ] Task 5: ADX + DSP Decision Engine — strategy matching + pricing
- [ ] Task 6: Tracking Server — imp/click/conv/landing endpoints
- [ ] Task 7: Strategy RTB Deploy — Redis config push
- [ ] Task 8: Docker Compose + Media Simulator — one-command demo
- [ ] Task 9: Integration test + 200 QPS baseline — tests pass, QPS ≥ 200

**Phase 1 deliverable:** `cd ad-platform && ./run-demo.sh` runs a complete RTB chain demo.
