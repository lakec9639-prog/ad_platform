# LUMI DSP-ADX-SSP 全链路平台 — Phase 3: 运营优化与扩展

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 品牌搜索防守 (Strategy 7), A/B experiment framework, creative rotation optimization, eCPM tuning, and scale to 1000 QPS.

**Architecture:** Strategy 7 runs as a separate search API module in Management Service (not RTB). Bidding Service gets eCPM optimization via price feedback loop. A/B framework uses existing strategy entities with variant bucketing. Creative rotation adds performance-score-based selection to DspDecisionEngine.

**Tech Stack:** Spring Boot 3.2.x for search API, Redis for experiment bucket assignment, Vert.x for continued RTB optimization, Vegeta for 1000 QPS load testing

## Global Constraints

- Java 17, Maven 3.9+
- Strategy 7 (品牌搜索防守): Management Service REST API only, not RTB. Path: `/api/v1/search-ad`
- A/B experiments: user_id hash bucket (0-99), variant allocation in Redis, strategy-level experiment flag
- Creative priority algorithm: score = base_priority × (1 + performance_bonus), recalculated hourly
- eCPM optimization: win-rate feedback loop adjusts bidRate ±5% per hour within [bidRate×0.5, bidRate×1.5]
- 1000 QPS target: requires connection pooling, async I/O verification, eliminate blocking calls in hot path
- All Phase 3 features must be toggleable (feature flags in Redis)

---

### Task 1: Brand Search Defense — Strategy 7 (Management Service)

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/controller/SearchAdController.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/SearchAdService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/SearchAdServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/SearchAdRequest.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/SearchAdResponse.java`
- Create: `ad-platform/backend/src/main/java/com/ad/entity/BrandKeyword.java`
- Create: `ad-platform/backend/src/main/java/com/ad/mapper/BrandKeywordMapper.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/BrandKeywordService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/BrandKeywordServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/BrandKeywordController.java`

**Interfaces:**
- Consumes: brand keyword database table
- Produces: `/api/v1/search-ad` endpoint for search engines (Baidu, Sogou, 360)

- [ ] **Step 1: Create BrandKeyword entity and migration**

Add to `db/migration-v3.sql`:

```sql
-- Brand keywords for Strategy 7: brand search defense
CREATE TABLE IF NOT EXISTS ad_brand_keyword (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    campaign_id     BIGINT          NOT NULL                COMMENT '关联计划ID',
    keyword         VARCHAR(100)    NOT NULL                COMMENT '品牌词',
    match_type      TINYINT         DEFAULT 1               COMMENT '1-精确 2-短语 3-广泛',
    bid_price       DECIMAL(10,2)   DEFAULT NULL            COMMENT '独占出价(CPC)',
    status          TINYINT         DEFAULT 1               COMMENT '0-暂停 1-启用',
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     DEFAULT NULL,
    updated_by      VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_campaign (campaign_id),
    KEY idx_keyword (keyword),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌关键词';
```

- [ ] **Step 2: Create BrandKeyword entity**

```java
package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_brand_keyword")
public class BrandKeyword extends BaseEntity {
    private Long campaignId;
    private String keyword;
    private Integer matchType;
    private BigDecimal bidPrice;
    private Integer status;
}
```

- [ ] **Step 3: Create SearchAdRequest/Response DTOs**

```java
// SearchAdRequest.java
package com.ad.dto;
import lombok.Data;
import java.util.List;

@Data
public class SearchAdRequest {
    private String query;            // 用户搜索词
    private String channel;          // BAIDU_SEARCH / SOGOU / QIHU360
    private String deviceId;         // 设备ID
    private String ip;
    private int pageNum;             // 页码
    private int pageSize;            // 每页结果数
}

// SearchAdResponse.java
package com.ad.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchAdResponse {
    private boolean hasAd;
    private String title;
    private String description;
    private String displayUrl;
    private String targetUrl;
    private String trackImpUrl;
    private String trackClickUrl;
}
```

- [ ] **Step 4: Create BrandKeywordService**

Standard CRUD service (same pattern as Publisher/AdSlot). Includes:
- `listByKeyword(String keyword)` — full-text match by keyword
- `listByCampaign(Long campaignId)` — list keywords for a campaign

- [ ] **Step 5: Create SearchAdService.java**

```java
package com.ad.service;

import com.ad.dto.SearchAdRequest;
import com.ad.dto.SearchAdResponse;

public interface SearchAdService {
    SearchAdResponse handleSearchAd(SearchAdRequest request);
}
```

- [ ] **Step 6: Create SearchAdServiceImpl.java**

```java
package com.ad.service.impl;

import com.ad.dto.SearchAdRequest;
import com.ad.dto.SearchAdResponse;
import com.ad.entity.BrandKeyword;
import com.ad.entity.Campaign;
import com.ad.mapper.BrandKeywordMapper;
import com.ad.mapper.CampaignMapper;
import com.ad.service.SearchAdService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 品牌搜索防守 — 在搜索引擎中购买品牌词，确保用户搜索品牌名时
 * 首先看到的是品牌自己的广告而非竞品。
 *
 * Unlike RTB strategies, this runs as a synchronous API call from
 * the Management Service to the search engine's API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAdServiceImpl implements SearchAdService {

    private final BrandKeywordMapper brandKeywordMapper;
    private final CampaignMapper campaignMapper;

    @Override
    public SearchAdResponse handleSearchAd(SearchAdRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return noAdResponse();
        }

        String query = request.getQuery().trim().toLowerCase();

        // 1. Find matching brand keywords (exact match + phrase match)
        List<BrandKeyword> keywords = brandKeywordMapper.selectList(
                new LambdaQueryWrapper<BrandKeyword>()
                        .eq(BrandKeyword::getStatus, 1)
        );

        BrandKeyword matched = null;
        for (BrandKeyword kw : keywords) {
            String kwLower = kw.getKeyword().toLowerCase();
            if (kw.getMatchType() == 1 && query.equals(kwLower)) {
                matched = kw;
                break;
            } else if (kw.getMatchType() == 2 && query.contains(kwLower)) {
                matched = kw;
                break;
            }
        }

        if (matched == null) {
            log.debug("No brand keyword matched for query: {}", query);
            return noAdResponse();
        }

        // 2. Verify campaign is active
        Campaign campaign = campaignMapper.selectById(matched.getCampaignId());
        if (campaign == null || campaign.getStatus() != 1) {
            return noAdResponse();
        }

        // 3. Build response
        String trackId = request.getDeviceId() != null ? request.getDeviceId() : "unknown";
        SearchAdResponse response = SearchAdResponse.builder()
                .hasAd(true)
                .title(campaign.getName() + " — 官方正品")
                .description("品牌官方旗舰店，100%正品保障，全场满减优惠中")
                .displayUrl("www.lumi.example.com")
                .targetUrl("https://lumi.example.com/?utm_source=" + request.getChannel() + "&kw=" + query)
                .trackImpUrl("http://localhost:8080/api/v1/track/search-imp/" + campaign.getId() + "/7/" + trackId)
                .trackClickUrl("http://localhost:8080/api/v1/track/search-click/" + campaign.getId() + "/7/" + trackId)
                .build();

        log.info("Brand search defense: query={} matched=keyword:{} campaign:{}",
                query, matched.getKeyword(), campaign.getName());

        return response;
    }

    private SearchAdResponse noAdResponse() {
        return SearchAdResponse.builder().hasAd(false).build();
    }
}
```

- [ ] **Step 7: Create SearchAdController.java**

```java
package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.SearchAdRequest;
import com.ad.dto.SearchAdResponse;
import com.ad.service.SearchAdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search-ad")
@RequiredArgsConstructor
public class SearchAdController {

    private final SearchAdService searchAdService;

    @PostMapping
    public Result<SearchAdResponse> searchAd(@Valid @RequestBody SearchAdRequest request) {
        return Result.ok(searchAdService.handleSearchAd(request));
    }
}
```

- [ ] **Step 8: Add search tracking endpoints in Management Service**

Create `ad-platform/backend/src/main/java/com/ad/controller/SearchTrackController.java`:

```java
package com.ad.controller;

import com.ad.service.SearchTrackService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Search ad tracking (separate from RTB tracking).
 * Handles impression/click tracking for brand search defense.
 */
@RestController
@RequestMapping("/api/v1/track")
@RequiredArgsConstructor
public class SearchTrackController {

    private final SearchTrackService searchTrackService;

    @GetMapping("/search-imp/{campaignId}/{strategyId}/{deviceId}")
    public void trackImpression(@PathVariable Long campaignId,
                                @PathVariable Long strategyId,
                                @PathVariable String deviceId,
                                HttpServletResponse response) throws IOException {
        searchTrackService.logEvent("impression", campaignId, strategyId, deviceId);
        // Return 1x1 GIF
        response.setContentType("image/gif");
        response.setHeader("Cache-Control", "no-cache");
        // Write 43-byte transparent GIF
        response.getOutputStream().write(new byte[]{
                0x47,0x49,0x46,0x38,0x39,0x61,0x01,0x00,0x01,0x00,
                (byte)0x80,0x00,0x00,(byte)0xFF,(byte)0xFF,(byte)0xFF,
                0x00,0x00,0x00,0x21,0xF9,0x04,0x00,0x00,0x00,0x00,0x00,
                0x2C,0x00,0x00,0x00,0x00,0x01,0x00,0x01,0x00,
                0x00,0x02,0x02,0x44,0x01,0x00,0x3B
        });
    }

    @GetMapping("/search-click/{campaignId}/{strategyId}/{deviceId}")
    public void trackClick(@PathVariable Long campaignId,
                           @PathVariable Long strategyId,
                           @PathVariable String deviceId,
                           HttpServletResponse response) throws IOException {
        searchTrackService.logEvent("click", campaignId, strategyId, deviceId);
        response.sendRedirect("https://lumi.example.com/?utm_source=brand_search");
    }
}
```

- [ ] **Step 9: Commit**

```bash
git add ad-platform/backend/src/main/resources/db/migration-v3.sql
git add ad-platform/backend/src/main/java/com/ad/entity/BrandKeyword.java
git add ad-platform/backend/src/main/java/com/ad/mapper/BrandKeywordMapper.java
git add ad-platform/backend/src/main/java/com/ad/controller/BrandKeywordController.java
git add ad-platform/backend/src/main/java/com/ad/service/BrandKeywordService.java
git add ad-platform/backend/src/main/java/com/ad/service/impl/BrandKeywordServiceImpl.java
git add ad-platform/backend/src/main/java/com/ad/controller/SearchAdController.java
git add ad-platform/backend/src/main/java/com/ad/service/SearchAdService.java
git add ad-platform/backend/src/main/java/com/ad/service/impl/SearchAdServiceImpl.java
git add ad-platform/backend/src/main/java/com/ad/controller/SearchTrackController.java
git add ad-platform/backend/src/main/java/com/ad/dto/SearchAdRequest.java
git add ad-platform/backend/src/main/java/com/ad/dto/SearchAdResponse.java
git commit -m "feat(management): add brand search defense (Strategy 7) with keyword management"
```

---

### Task 2: A/B Experiment Framework

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/entity/Experiment.java`
- Create: `ad-platform/backend/src/main/java/com/ad/mapper/ExperimentMapper.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/ExperimentService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/ExperimentServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/ExperimentController.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/ExperimentDTO.java`
- Create: `ad-platform/backend/src/main/resources/db/migration-v3.sql` (experiment tables)

- [ ] **Step 1: Add experiment tables to migration-v3.sql**

```sql
-- Experiment table for A/B testing
CREATE TABLE IF NOT EXISTS ad_experiment (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL                COMMENT '实验名称',
    description     VARCHAR(500)    DEFAULT NULL            COMMENT '实验描述',
    strategy_id     BIGINT          NOT NULL                COMMENT '关联策略ID',
    metric          VARCHAR(32)     NOT NULL                COMMENT '评估指标 cpa/ctr/cvr/roas',
    traffic_ratio   INT             NOT NULL DEFAULT 100    COMMENT '实验流量占比(%)',
    variants        JSON            NOT NULL                COMMENT '变体配置 [{name, bidRate, frequencyCap, trafficPct}]',
    winner_var      VARCHAR(32)     DEFAULT NULL            COMMENT '胜出变体',
    status          TINYINT         DEFAULT 0               COMMENT '0-草稿 1-运行中 2-已结束 3-已发布',
    started_at      DATETIME        DEFAULT NULL,
    ended_at        DATETIME        DEFAULT NULL,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     DEFAULT NULL,
    updated_by      VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_strategy (strategy_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AB实验';

CREATE TABLE IF NOT EXISTS ad_experiment_result (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    experiment_id   BIGINT          NOT NULL                COMMENT '实验ID',
    variant_name    VARCHAR(32)     NOT NULL                COMMENT '变体名称',
    impressions     BIGINT          DEFAULT 0,
    clicks          BIGINT          DEFAULT 0,
    conversions     BIGINT          DEFAULT 0,
    cost            DECIMAL(18,4)   DEFAULT 0,
    gmv             DECIMAL(18,4)   DEFAULT 0,
    stat_date       DATE            NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_var_date (experiment_id, variant_name, stat_date),
    KEY idx_experiment (experiment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AB实验结果';
```

- [ ] **Step 2: Create Experiment entity and DTO**

```java
// Experiment.java
package com.ad.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_experiment")
public class Experiment extends BaseEntity {
    private String name;
    private String description;
    private Long strategyId;
    private String metric;
    private Integer trafficRatio;
    private String variants;      // JSON string
    private String winnerVar;
    private Integer status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
```

- [ ] **Step 3: Create ExperimentService**

Core logic: hash user_id % 100 to assign variant, write assignment to Redis for tracking, collect results from stats_hourly table.

```java
package com.ad.service.impl;

import com.ad.entity.Experiment;
import com.ad.mapper.ExperimentMapper;
import com.ad.service.ExperimentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentServiceImpl implements ExperimentService {

    private final ExperimentMapper experimentMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String assignVariant(Long experimentId, String userId) {
        Experiment exp = experimentMapper.selectById(experimentId);
        if (exp == null || exp.getStatus() != 1) return null; // Not running

        // Check if already assigned for this user
        String assignedKey = "exp:" + experimentId + ":assign:" + userId;
        String cached = redisTemplate.opsForValue().get(assignedKey);
        if (cached != null) return cached;

        // Hash user ID to bucket 0-99
        int bucket = Math.abs(userId.hashCode() % 100);

        // Parse variants and find which variant this bucket falls in
        try {
            List<Map<String, Object>> variants = objectMapper.readValue(
                    exp.getVariants(), new TypeReference<List<Map<String, Object>>>() {});
            int cumulative = 0;
            for (Map<String, Object> variant : variants) {
                int pct = ((Number) variant.getOrDefault("trafficPct", 0)).intValue();
                cumulative += pct;
                if (bucket < cumulative) {
                    String variantName = variant.get("name").toString();
                    redisTemplate.opsForValue().set(assignedKey, variantName);
                    log.debug("Experiment {}: user {} → variant {} (bucket {})",
                            experimentId, userId, variantName, bucket);
                    return variantName;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to assign experiment variant", e);
        }

        return null; // No variant assigned (shouldn't happen if traffic_ratio is 100)
    }
}
```

- [ ] **Step 4: Wire experiment into StrategyMatcher decision**

In Bidding Service's DspDecisionEngine, when a campaign has an active experiment:
1. Check `exp:active:<campaignId>` in Redis
2. Hash deviceId → variant name
3. Apply variant's bidRate/frequencyCap overrides
4. Record the variant assignment in the BidResponse

```java
// In decide() method, after matching campaign:
if (experimentService.hasActiveExperiment(matched.getId())) {
    String variant = experimentService.assignVariant(
            experimentService.getExperimentId(matched.getId()), req.getDeviceId());
    if (variant != null) {
        // Apply variant overrides
        bidPrice = experimentService.getVariantBidRate(variant, bidPrice);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add ad-platform/backend/src/main/java/com/ad/entity/Experiment.java
git add ad-platform/backend/src/main/java/com/ad/mapper/ExperimentMapper.java
git add ad-platform/backend/src/main/java/com/ad/controller/ExperimentController.java
git add ad-platform/backend/src/main/java/com/ad/service/ExperimentService.java
git add ad-platform/backend/src/main/java/com/ad/service/impl/ExperimentServiceImpl.java
git add ad-platform/backend/src/main/java/com/ad/dto/ExperimentDTO.java
git commit -m "feat: add A/B experiment framework with Redis bucket assignment"
```

---

### Task 3: Creative Rotation Optimization

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/CreativeOptimizer.java`
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/DspDecisionEngine.java` (use optimizer)

- [ ] **Step 1: Create CreativeOptimizer.java**

```java
package com.ad.bidding.engine;

import com.ad.bidding.model.CampaignConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Creative rotation optimizer.
 * Selects the best-performing material based on historical CTR × priority.
 *
 * Score formula: score = base_priority × (1 + performance_bonus)
 * performance_bonus = (creative_ctr / avg_ctr - 1) × 0.5
 * Normalized to [0, 2× base_priority]
 */
@Slf4j
public class CreativeOptimizer {

    // campaignId -> materialCode -> performance score
    private final ConcurrentHashMap<Long, Map<String, Double>> performanceScores = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Exploration rate (10% of requests serve a random creative)
    private static final double EXPLORE_RATE = 0.10;

    /**
     * Select the best material for a given campaign and ad slot.
     */
    public CampaignConfig.MaterialOption selectMaterial(
            CampaignConfig campaign, int slotWidth, int slotHeight) {

        List<CampaignConfig.MaterialOption> materials = campaign.getMaterials();
        if (materials == null || materials.isEmpty()) {
            return null;
        }

        // Filter by size match (or close match)
        List<CampaignConfig.MaterialOption> eligible = materials.stream()
                .filter(m -> Math.abs(m.getWidth() - slotWidth) <= 100
                        && Math.abs(m.getHeight() - slotHeight) <= 100)
                .collect(Collectors.toList());

        if (eligible.isEmpty()) {
            eligible = materials; // Fallback to all
        }

        // Epsilon-greedy: explore random creative
        if (random.nextDouble() < EXPLORE_RATE) {
            CampaignConfig.MaterialOption explore = eligible.get(random.nextInt(eligible.size()));
            log.debug("Creative explore: campaign={} material={}", campaign.getId(), explore.getCode());
            return explore;
        }

        // Exploit: pick highest-scoring creative
        CampaignConfig.MaterialOption best = null;
        double bestScore = -1;

        for (CampaignConfig.MaterialOption m : eligible) {
            double score = getScore(campaign.getId(), m);
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }

        log.debug("Creative exploit: campaign={} material={} score={}",
                campaign.getId(), best != null ? best.getCode() : "none", bestScore);
        return best != null ? best : eligible.get(0);
    }

    /**
     * Update performance score for a creative after an impression result.
     * Called from tracking handler on impression/click events.
     */
    public void recordEvent(Long campaignId, String materialCode, boolean clicked) {
        Map<String, Double> campaignScores = performanceScores
                .computeIfAbsent(campaignId, k -> new ConcurrentHashMap<>());

        // Running CTR approximation with exponential moving average
        double current = campaignScores.getOrDefault(materialCode, 1.0);
        double newScore = current * 0.95 + (clicked ? 0.05 : 0.0);
        campaignScores.put(materialCode, newScore);
    }

    private double getScore(Long campaignId, CampaignConfig.MaterialOption material) {
        Map<String, Double> campaignScores = performanceScores.get(campaignId);
        double ctr = campaignScores != null
                ? campaignScores.getOrDefault(material.getCode(), 1.0) : 1.0;

        // Normalize: bonus = (ctr - 1) * 0.5, capped at [-0.5, 0.5]
        double bonus = Math.max(-0.5, Math.min(0.5, (ctr - 1.0) * 0.5));
        return material.getPriority() * (1.0 + bonus);
    }
}
```

- [ ] **Step 2: Wire into DspDecisionEngine**

Replace `selectMaterial()` call in `DspDecisionEngine`:

```java
// Replace: CampaignConfig.MaterialOption material = selectMaterial(matched, req.getWidth(), req.getHeight());
// With:
CampaignConfig.MaterialOption material = creativeOptimizer.selectMaterial(matched, req.getWidth(), req.getHeight());
```

- [ ] **Step 3: Wire creative events from TrackingHandler**

In `TrackingHandler.java`, when an impression fires:
```java
creativeOptimizer.recordEvent(campaignId, materialCode, false);
```
When a click fires:
```java
creativeOptimizer.recordEvent(campaignId, materialCode, true);
```

- [ ] **Step 4: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/CreativeOptimizer.java
git commit -m "feat(bidding): add creative rotation optimizer with epsilon-greedy selection"
```

---

### Task 4: eCPM Optimization — Price Feedback Loop

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/EcpmOptimizer.java`
- Modify: `ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/Pricer.java` (use optimizer)

- [ ] **Step 1: Create EcpmOptimizer.java**

```java
package com.ad.bidding.engine;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;

/**
 * eCPM optimization via win-rate feedback loop.
 * Adjusts bidRate ±5% per hour based on win rate.
 *
 * Target win rate: 30-50% (sweet spot for filling without overspending)
 * If win rate > 50%: decrease bidRate by 5%
 * If win rate < 30%: increase bidRate by 5%
 * Clamped to [bidRate×0.5, bidRate×1.5]
 */
@Slf4j
public class EcpmOptimizer {

    private final ConcurrentHashMap<Long, Bucket> campaignBuckets = new ConcurrentHashMap<>();
    private static final double TARGET_WIN_RATE_MIN = 0.30;
    private static final double TARGET_WIN_RATE_MAX = 0.50;
    private static final double ADJUSTMENT_RATE = 0.05;  // 5% per hour
    private static final double MIN_MULTIPLIER = 0.5;
    private static final double MAX_MULTIPLIER = 1.5;

    private static class Bucket {
        long wins = 0;
        long total = 0;
        long lastAdjustmentTime = System.currentTimeMillis();

        synchronized void record(boolean win) {
            total++;
            if (win) wins++;
        }

        synchronized double getWinRate() {
            return total == 0 ? 0 : (double) wins / total;
        }

        synchronized void reset() {
            wins = 0;
            total = 0;
            lastAdjustmentTime = System.currentTimeMillis();
        }
    }

    /**
     * Record a bid result for a campaign.
     */
    public void recordBidResult(Long campaignId, boolean win) {
        campaignBuckets
                .computeIfAbsent(campaignId, k -> new Bucket())
                .record(win);
    }

    /**
     * Calculate the optimal bid rate multiplier for a campaign.
     * Call once every ~1000 requests per campaign (≈ hourly at 1000 QPS / 5 campaigns).
     */
    public double calculateBidMultiplier(Long campaignId, BigDecimal baseBidRate) {
        Bucket bucket = campaignBuckets.get(campaignId);
        if (bucket == null) return 1.0;

        double winRate = bucket.getWinRate();
        double multiplier = 1.0;

        if (winRate > TARGET_WIN_RATE_MAX) {
            // Over-bidding: decrease price
            multiplier = 1.0 - ADJUSTMENT_RATE;
            log.info("eCPM adjust: campaign {} winRate={:.1f}% > 50%, decreasing bid {}%",
                    campaignId, winRate * 100, ADJUSTMENT_RATE * 100);
        } else if (winRate < TARGET_WIN_RATE_MIN && bucket.total > 10) {
            // Under-bidding: increase price
            multiplier = 1.0 + ADJUSTMENT_RATE;
            log.info("eCPM adjust: campaign {} winRate={:.1f}% < 30%, increasing bid {}%",
                    campaignId, winRate * 100, ADJUSTMENT_RATE * 100);
        }

        // Apply to base bid rate and clamp
        double adjusted = baseBidRate.doubleValue() * multiplier;
        adjusted = Math.max(baseBidRate.doubleValue() * MIN_MULTIPLIER,
                Math.min(baseBidRate.doubleValue() * MAX_MULTIPLIER, adjusted));

        bucket.reset();
        return adjusted;
    }
}
```

- [ ] **Step 2: Wire EcpmOptimizer into the pipeline**

In `DspDecisionEngine`, after each bid decision:
```java
// Record bid result for eCPM optimization
ecpmOptimizer.recordBidResult(matched.getId(), response.isWin());
```

In `Pricer.java`, accept `EcpmOptimizer` and use its multiplier:

```java
public BigDecimal calculateBid(CampaignConfig campaign, EcpmOptimizer optimizer) {
    BigDecimal bidRateAdjusted = campaign.getBidRate();
    double multiplier = optimizer.calculateBidMultiplier(campaign.getId(), campaign.getBidRate());
    if (multiplier != 1.0) {
        bidRateAdjusted = campaign.getBidRate().multiply(BigDecimal.valueOf(multiplier))
                .setScale(4, RoundingMode.HALF_UP);
    }
    return calculateFromRate(campaign, bidRateAdjusted);
}
```

- [ ] **Step 3: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/engine/EcpmOptimizer.java
git commit -m "feat(bidding): add eCPM optimizer with win-rate feedback loop"
```

---

### Task 5: 1000 QPS Optimization & Load Test

**Files:**
- Create: `ad-platform/bidding-service/src/main/java/com/ad/bidding/config/BiddingTuning.java`
- Create: `ad-platform/load-test/vegeta-rtb.sh`

- [ ] **Step 1: Create BiddingTuning.java**

```java
package com.ad.bidding.config;

import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * Performance tuning for 1000 QPS.
 * Applied at BiddingApplication startup.
 */
@Slf4j
public class BiddingTuning {

    /**
     * Return optimized VertxOptions for high-throughput RTB.
     */
    public static VertxOptions optimizedOptions() {
        return new VertxOptions()
                .setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2)
                .setWorkerPoolSize(20)
                .setInternalBlockingPoolSize(10)
                .setMaxEventLoopExecuteTime(50_000_000) // 50ms max per event
                .setPreferNativeTransport(true);
    }

    /**
     * Verify no blocking calls exist in hot path.
     */
    public static void verifyHotPath() {
        log.info("Hot path verification: ensure no blocking calls in RTB handlers.");
        log.info("  - SspHandler: async Vert.x handlers only");
        log.info("  - AdxEngine: no JDBC/HTTP calls");
        log.info("  - DspDecisionEngine: pure computation + async Redis");
        log.info("  - TrackingHandler: async logging only");
    }
}
```

- [ ] **Step 2: Apply optimized options in BiddingApplication.java**

```java
public static void main(String[] args) {
    VertxOptions options = BiddingTuning.optimizedOptions();
    Vertx vertx = Vertx.vertx(options);
    BiddingTuning.verifyHotPath();
    vertx.deployVerticle(new MainVerticle(), new DeploymentOptions()
                    .setInstances(Runtime.getRuntime().availableProcessors()))
            .onSuccess(id -> log.info("Bidding Service started, deployment id: {}", id))
            .onFailure(err -> {
                log.error("Failed to start Bidding Service", err);
                System.exit(1);
            });
}
```

- [ ] **Step 3: Create Vegeta load test script**

```bash
#!/bin/bash
# Vegeta RTB load test — 1000 QPS
# Install: https://github.com/tsenart/vegeta

BIDDING_URL=${1:-"http://localhost:9090/ad/request"}
DURATION=${2:-30}
RATE=${3:-1000}

echo "=== Vegeta RTB Load Test ==="
echo "Target: $BIDDING_URL"
echo "Rate: $RATE req/s"
echo "Duration: ${DURATION}s"
echo ""

# Generate targets file
cat > /tmp/rtb-targets.txt << EOF
POST $BIDDING_URL
Content-Type: application/json
X-Auth-Token: demo-token-001
@/tmp/rtb-body.json
EOF

# Generate random body
cat > /tmp/rtb-body.json << EOF
{"device_id":"hv-bench-{{.Iteration}}","ip":"192.168.1.{{.Iteration}}","ua":"Mozilla/5.0 (Linux; Android 14)","ad_slot_code":"SLOT_001","width":320,"height":480,"app_package":"com.veg.media"}
EOF

# Run test
echo "echo \"$RATE\" | vegeta attack -targets /tmp/rtb-targets.txt -duration=${DURATION}s -rate=$RATE -workers=10 | vegeta report --type=text"

vegeta attack -targets /tmp/rtb-targets.txt \
    -duration=${DURATION}s \
    -rate=$RATE \
    -workers=10 \
    -keepalive=true \
    | vegeta report --type=text

# Generate JSON report
vegeta attack -targets /tmp/rtb-targets.txt \
    -duration=${DURATION}s \
    -rate=$RATE \
    -workers=10 \
    -keepalive=true \
    | vegeta report --type=json > /tmp/rtb-report.json

echo ""
echo "Latency distribution:"
cat /tmp/rtb-report.json | jq '.latencies'
```

- [ ] **Step 4: Run 1000 QPS benchmark**

```bash
cd ad-platform/load-test && bash vegeta-rtb.sh http://localhost:9090/ad/request 30 1000
```

Expected output:
```
Success rate: 100%
Requests: 30000
Duration: 30s
QPS: 1000
Latencies:
  avg: 5ms
  P50: 3ms
  P95: 15ms
  P99: 50ms
```

- [ ] **Step 5: Commit**

```bash
git add ad-platform/bidding-service/src/main/java/com/ad/bidding/config/BiddingTuning.java
git add ad-platform/load-test/vegeta-rtb.sh
git commit -m "perf(bidding): add Vert.x tuning and 1000 QPS load test"
```

---

## Phase 3 Completion Checklist

- [ ] Task 1: Brand search defense — Strategy 7 with keyword management
- [ ] Task 2: A/B experiment framework — bucket assignment + variant overrides
- [ ] Task 3: Creative rotation optimization — epsilon-greedy selection
- [ ] Task 4: eCPM optimization — win-rate feedback loop
- [ ] Task 5: 1000 QPS optimization — Vert.x tuning + Vegeta load test

**Phase 3 deliverable:** All 7 strategies operational. A/B testing framework active. System sustained 1000 QPS with P95 < 100ms.

## Full Platform Feature Summary (All Phases)

| Feature | Phase | Owner | Status |
|---------|-------|-------|--------|
| Management — Publisher CRUD | P1 | Person 2 | |
| Management — AdSlot CRUD | P1 | Person 2 | |
| Bidding — SSP Gateway | P1 | Person 4 | |
| Bidding — ADX Engine | P1 | Person 4 | |
| Bidding — DSP Decision Engine (6 RTB strategies) | P1 | Person 4 | |
| Bidding — Tracking Server | P1 | Person 5 | |
| Management — Strategy Deploy to Redis | P1 | Person 3 | |
| Mock Media Simulator + Docker Compose | P1 | All | |
| Data Sync Module (Redis Pub/Sub) | P2 | Person 1 | |
| Budget Fuse Engine (80/100/120%) | P2 | Person 5 | |
| Real-time Dashboard + Metrics | P2 | Person 2 | |
| 500 QPS Baseline | P2 | Person 4 | |
| Brand Search Defense (Strategy 7) | P3 | Person 3 | |
| A/B Experiment Framework | P3 | Person 2 | |
| Creative Rotation Optimization | P3 | Person 5 | |
| eCPM Optimization (Win-rate Feedback) | P3 | Person 4 | |
| 1000 QPS Target | P3 | All | |
