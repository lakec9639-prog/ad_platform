# LUMI 投放中台 — Phase 2: 后端业务层

> **For agentic workers:** This is Phase 2 of the implementation plan. Implement all DTOs, Services, and Controllers in order.

**Goal:** Build all service and controller layers covering Strategy, Campaign, Audience, Material, Dashboard, and Rule Engine modules.

**Architecture:** Service-Controller pattern. ServiceImpl wraps MyBatis-Plus mapper calls. Controller accepts/returns DTOs, never entities. Dashboard uses Redis caching with per-day key pattern. Rule engine includes sandbox simulation support.

---

### Task 2.a: Strategy Service + Controller

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/dto/StrategyDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/StrategyCreateDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/StrategyStatusDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/StrategyService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/StrategyServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/StrategyController.java`

**Interfaces:**
- Consumes: StrategyMapper, StrategyChannelMapper, StrategyAudienceMapper, StrategyMaterialMapper, CampaignMapper (for stats)
- Produces: Strategy CRUD REST API at `/api/v1/strategies`

**DTOs:**
```java
// StrategyDTO.java
package com.ad.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StrategyDTO {
    private Long id;
    private String name;
    private String code;
    private Integer status;
    private String objective;
    private String description;
    private BigDecimal budget;
    private BigDecimal budgetRatio;
    private BigDecimal targetCpa;
    private BigDecimal targetCvr;
    private BigDecimal expectedRoas;
    private Integer sortOrder;
    private List<ChannelAllocation> channelAllocations;
    private List<Long> audienceIds;
    private List<Long> materialIds;

    // Summary stats from dashboard
    private BigDecimal currentCost;
    private BigDecimal currentCpa;
    private BigDecimal currentRoas;

    @Data
    public static class ChannelAllocation {
        private String channel;
        private BigDecimal budgetRatio;
    }
}
```

**Service interface:**
```java
// StrategyService.java
package com.ad.service;

import com.ad.dto.StrategyDTO;
import com.ad.dto.StrategyCreateDTO;
import java.util.List;

public interface StrategyService {
    List<StrategyDTO> listAll();
    StrategyDTO getById(Long id);
    Long create(StrategyCreateDTO dto);
    void update(Long id, StrategyCreateDTO dto);
    void updateStatus(Long id, Integer status);
}
```

**Controller:**
```java
// StrategyController.java
package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.StrategyCreateDTO;
import com.ad.dto.StrategyDTO;
import com.ad.dto.StrategyStatusDTO;
import com.ad.service.StrategyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class StrategyController {
    private final StrategyService strategyService;

    @GetMapping
    public Result<List<StrategyDTO>> list() {
        return Result.ok(strategyService.listAll());
    }

    @GetMapping("/{id}")
    public Result<StrategyDTO> getById(@PathVariable Long id) {
        return Result.ok(strategyService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody StrategyCreateDTO dto) {
        return Result.ok(strategyService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StrategyCreateDTO dto) {
        strategyService.update(id, dto);
        return Result.ok(null);
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StrategyStatusDTO dto) {
        strategyService.updateStatus(id, dto.getStatus());
        return Result.ok(null);
    }
}
```

**Key implementation details in StrategyServiceImpl:**
- `listAll()`: query all strategies ordered by sort_order, for each compute currentCost/currentCpa from ad_stats_hourly
- `create(dto)`: insert strategy + channel allocations + audience/material associations in a transaction
- `update(id, dto)`: update strategy + delete & re-insert channel/audience/material relations in a transaction
- `updateStatus(id, status)`: validate status transition (DRAFT→ACTIVE→PAUSED→ENDED), update field

- [ ] **Step 1:** Create `StrategyDTO.java`, `StrategyCreateDTO.java`, `StrategyStatusDTO.java`
- [ ] **Step 2:** Create `StrategyService.java` interface
- [ ] **Step 3:** Create `StrategyServiceImpl.java` with transaction management (@Transactional)
- [ ] **Step 4:** Create `StrategyController.java`
- [ ] **Step 5:** Verify API works: start backend, `curl http://localhost:8080/api/v1/strategies` returns `{"code":0,"data":[]}`

---

### Task 2.b: Campaign Service + Controller

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/dto/CampaignDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/CampaignCreateDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/BatchStatusDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/CampaignService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/CampaignServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/CampaignController.java`

**Interfaces:**
- Consumes: CampaignMapper, StatsHourlyMapper
- Produces: Campaign CRUD + batch status REST API at `/api/v1/campaigns`

```java
// CampaignDTO.java
package com.ad.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CampaignDTO {
    private Long id;
    private Long strategyId;
    private String strategyName;
    private String name;
    private String channel;
    private String platformCampaignId;
    private BigDecimal budgetDaily;
    private String bidType;
    private BigDecimal bidPrice;
    private Integer status;
    private LocalDateTime launchAt;
    private LocalDateTime stopAt;

    // Real-time stats (from ad_stats_hourly aggregation)
    private BigDecimal currentCost;
    private Integer currentConversions;
    private BigDecimal currentCpa;
    private BigDecimal currentRoas;
}
```

```java
// CampaignController.java
package com.ad.controller;

import com.ad.common.PageResult;
import com.ad.common.Result;
import com.ad.dto.BatchStatusDTO;
import com.ad.dto.CampaignCreateDTO;
import com.ad.dto.CampaignDTO;
import com.ad.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;

    @GetMapping
    public Result<PageResult<CampaignDTO>> list(
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(campaignService.list(strategyId, channel, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<CampaignDTO> getById(@PathVariable Long id) {
        return Result.ok(campaignService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CampaignCreateDTO dto) {
        return Result.ok(campaignService.create(dto));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody BatchStatusDTO dto) {
        campaignService.updateStatus(id, dto.getStatus());
        return Result.ok(null);
    }

    @PatchMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@RequestBody BatchStatusDTO dto) {
        campaignService.batchUpdateStatus(dto.getIds(), dto.getStatus());
        return Result.ok(null);
    }
}
```

- [ ] **Step 1:** Create `CampaignDTO.java`, `CampaignCreateDTO.java`, `BatchStatusDTO.java`
- [ ] **Step 2:** Create `CampaignService.java` interface
- [ ] **Step 3:** Create `CampaignServiceImpl.java` — paginated list with filters, batch status via CampaignMapper.updateBatchStatus
- [ ] **Step 4:** Create `CampaignController.java`
- [ ] **Step 5:** Test batch status: `curl -X PATCH http://localhost:8080/api/v1/campaigns/batch-status -H "Content-Type: application/json" -d '{"ids":[1,2],"status":2}'`

---

### Task 2.c: Audience + Material Services + Controllers

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/dto/AudienceDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/MaterialDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/AudienceService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/AudienceServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/AudienceController.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/MaterialService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/MaterialServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/MaterialController.java`

**Interfaces:**
- Consumes: AudienceMapper, MaterialMapper, StatsHourlyMapper
- Produces: Audience CRUD at `/api/v1/audiences`, Material CRUD at `/api/v1/materials`

**AudienceService interface:**
```java
public interface AudienceService {
    List<AudienceDTO> listAll();
    Long create(AudienceDTO dto);
    Map<String, Object> getStats(Long id, LocalDate startDate, LocalDate endDate);
}
```

**MaterialService interface:**
```java
public interface MaterialService {
    List<MaterialDTO> listAll();
    Long create(MaterialDTO dto);
    List<Map<String, Object>> getDecayCurve(Long id, LocalDate startDate, LocalDate endDate);
}
```

- [ ] **Step 1:** Create `AudienceDTO.java`, `MaterialDTO.java`
- [ ] **Step 2:** Create AudienceService + AudienceServiceImpl + AudienceController (`GET /audiences`, `POST /audiences`, `GET /audiences/{id}/stats`)
- [ ] **Step 3:** Create MaterialService + MaterialServiceImpl + MaterialController (`GET /materials`, `POST /materials`, `GET /materials/{id}/decay`)
- [ ] **Step 4:** Verify all endpoints return `{"code":0,"data":...}`

---

### Task 2.d: Rule Engine Service + Controller

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/dto/RuleDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/RuleCreateDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/SandboxTestDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/RuleService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/RuleServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/RuleController.java`

**Interfaces:**
- Consumes: RuleMapper, RuleExecutionLogMapper, StatsHourlyMapper
- Produces: Rule CRUD + sandbox test at `/api/v1/rules`

**RuleDTO:**
```java
@Data
public class RuleDTO {
    private Long id;
    private String name;
    private String triggerMetric;
    private String triggerOperator;
    private BigDecimal triggerThreshold;
    private Integer triggerWindowHours;
    private String actionType;
    private String actionParams;  // JSON string
    private String scopeType;
    private String scopeValue;
    private Integer priority;
    private Integer cooldownMinutes;
    private Integer isSystem;     // 1 = built-in, cannot delete/disable
    private Integer status;
}
```

**SandboxTestDTO:**
```java
@Data
public class SandboxTestDTO {
    private Long ruleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer triggerCount;
    private Integer affectedCampaignCount;
    private BigDecimal estimatedBudgetSaved;
    private List<SandboxTrigger> triggers;
}

@Data
public class SandboxTrigger {
    private LocalDate date;
    private Long campaignId;
    private BigDecimal triggerValue;
    private String actionDescription;
}
```

**RuleServiceImpl sandbox logic:**
```java
public SandboxTestDTO simulate(SandboxTestDTO dto) {
    // 1. Query StatsHourly for the date range, grouped by campaign (or strategy/channel based on scope)
    // 2. For each data point, evaluate: does trigger_metric (CPA/CTR/CVR/CONSUME) exceed threshold with operator?
    // 3. Count triggers + affected campaigns
    // 4. For PAUSE action, estimate budget saved: sum(cost) of paused campaigns after trigger point
    // 5. Return SandboxTestDTO with triggerCount, affectedCampaignCount, triggers list
}
```

**RuleController — sandbox endpoint:**
```java
@PostMapping("/{id}/test")
public Result<SandboxTestDTO> test(@PathVariable Long id, @RequestBody SandboxTestRequest request) {
    return Result.ok(ruleService.simulate(id, request.getStartDate(), request.getEndDate()));
}
```

- [ ] **Step 1:** Create `RuleDTO.java`, `RuleCreateDTO.java`, `SandboxTestDTO.java`, `SandboxTestRequest.java`
- [ ] **Step 2:** Create `RuleService.java` interface with CRUD + simulate method
- [ ] **Step 3:** Create `RuleServiceImpl.java` — handle isSystem constraint (system rules cannot be deleted/disabled), sandbox simulation engine
- [ ] **Step 4:** Create `RuleController.java` (`GET /rules`, `POST /rules`, `PUT /rules/{id}`, `PATCH /rules/{id}/status`, `GET /rules/{id}/logs`, `POST /rules/{id}/test`)
- [ ] **Step 5:** Verify sandbox: `curl -X POST http://localhost:8080/api/v1/rules/1/test -H "Content-Type: application/json" -d '{"startDate":"2026-07-01","endDate":"2026-07-07"}'`

---

### Task 2.e: Dashboard Service + Controller

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/dto/DashboardOverviewDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/dto/TrendDTO.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/DashboardService.java`
- Create: `ad-platform/backend/src/main/java/com/ad/service/impl/DashboardServiceImpl.java`
- Create: `ad-platform/backend/src/main/java/com/ad/controller/DashboardController.java`

**Interfaces:**
- Consumes: StatsHourlyMapper, RedisTemplate
- Produces: Dashboard overview, trends, channel distribution, material top at `/api/v1/dashboard/*`

**DashboardOverviewDTO:**
```java
@Data
public class DashboardOverviewDTO {
    private BigDecimal totalCost;
    private Integer totalNewUsers;
    private BigDecimal cpa;           // totalCost / totalConversions
    private BigDecimal roas;          // totalGmv / totalCost
    private Integer totalConversions;
    private BigDecimal totalGmv;
    private BigDecimal totalImpressions;
    private Integer totalClicks;
    private BigDecimal budgetTotal;   // 800000
    private BigDecimal budgetProgress; // totalCost / budgetTotal
    private BigDecimal budgetRemaining;
    private BigDecimal ctr;           // clicks / impressions
    private BigDecimal cvr;           // conversions / clicks
}
```

**Redis caching logic in DashboardServiceImpl:**
```java
public DashboardOverviewDTO getOverview(LocalDate startDate, LocalDate endDate) {
    // Build key list for each day in range
    List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1)).toList();
    List<String> cachedKeys = dates.stream()
        .map(d -> "dash:overview:" + d)
        .toList();

    // Bulk check Redis
    List<Object> cached = redisTemplate.opsForValue().multiGet(cachedKeys);

    // Aggregate from cache hits
    Map<LocalDate, Map<String, Object>> result = new HashMap<>();
    List<LocalDate> missDates = new ArrayList<>();

    for (int i = 0; i < dates.size(); i++) {
        if (cached.get(i) != null) {
            result.put(dates.get(i), (Map<String, Object>) cached.get(i));
        } else {
            missDates.add(dates.get(i));
        }
    }

    // Query MySQL for miss dates
    if (!missDates.isEmpty()) {
        LocalDate queryStart = missDates.get(0);
        LocalDate queryEnd = missDates.get(missDates.size() - 1);
        List<Map<String, Object>> dailyData = statsHourlyMapper.dailyTrends(queryStart, queryEnd);

        for (Map<String, Object> row : dailyData) {
            LocalDate d = ((java.sql.Date) row.get("stat_date")).toLocalDate();
            result.put(d, row);
            redisTemplate.opsForValue().set("dash:overview:" + d, row, 5, TimeUnit.MINUTES);
        }
    }

    // Aggregate all daily data into overview DTO
    // ... sum cost, conversions, new_users, gmv, impressions, clicks
    // Compute CPA = cost / conversions, ROAS = gmv / cost, etc.
}
```

**DashboardController:**
```java
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardOverviewDTO> overview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getOverview(start, end));
    }

    @GetMapping("/trends")
    public Result<List<TrendDTO>> trends(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getTrends(start, end));
    }

    @GetMapping("/channel-dist")
    public Result<List<Map<String, Object>>> channelDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getChannelDistribution(start, end));
    }

    @GetMapping("/material-top")
    public Result<List<Map<String, Object>>> materialTop(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "5") int limit) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.ok(dashboardService.getMaterialTop(start, end, limit));
    }
}
```

- [ ] **Step 1:** Create `DashboardOverviewDTO.java`, `TrendDTO.java`
- [ ] **Step 2:** Create `DashboardService.java` interface
- [ ] **Step 3:** Create `DashboardServiceImpl.java` with Redis caching + MySQL fallback + daily aggregation logic
- [ ] **Step 4:** Create `DashboardController.java` with all 4 endpoints
- [ ] **Step 5:** Verify: `curl http://localhost:8080/api/v1/dashboard/overview?startDate=2026-07-01&endDate=2026-07-17`

---

### Task 2.f: Seed Data SQL

**Files:**
- Create: `ad-platform/backend/src/main/resources/db/seed-data.sql`

**Interfaces:**
- Consumes: init-schema.sql from Task 1.d
- Produces: 7 strategies, 13 audiences, 12 materials with channel/audience/material relations

**Seed data content — Strategy records:**
```sql
USE ad_platform;

-- Strategy channel allocations for each S1-S7
-- S1: 巨量引擎·重定向爆款, budget=120000, 15%, target_cpa=250
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, updated_by, created_at, updated_at)
VALUES ('巨量引擎·重定向爆款', 'S1', 1, 'CONVERT', '基于Lookalike老客模型扩展相似人群，利用KOC种草素材进行重定向投放', 120000.00, 15.00, 250.00, 2.00, 1, 'system', 'system', NOW(), NOW());
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (1, 'DOUYIN', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (1, 5, 0);  -- AUD005 Lookalike_老客_1%
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (1, 7, 1);  -- C007 KOC种草30s

-- S2: 小红书·成分党深耕, budget=100000, 12.5%, target_cpa=250
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, updated_by, created_at, updated_at)
VALUES ('小红书·成分党深耕', 'S2', 1, 'BRAND', '针对成分党人群投放成分解析素材，提升品牌专业认知', 100000.00, 12.50, 250.00, 1.50, 2, 'system', 'system', NOW(), NOW());
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (2, 'XIAOHONGSHU', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (2, 4, 0);  -- AUD004 成分党_烟酰胺
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (2, 2, 1);  -- C002 成分图解析
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (2, 7, 2);  -- C007 KOC种草30s

-- S3: B站·新品破圈, budget=80000, 10%, target_cpa=300
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, updated_by, created_at, updated_at)
VALUES ('B站·新品破圈', 'S3', 1, 'BRAND', 'B站新品破圈投放，面向美妆兴趣用户群', 80000.00, 10.00, 300.00, 1.20, 3, 'system', 'system', NOW(), NOW());
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (3, 'BILIBILI', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (3, 1, 0);  -- AUD001 美妆兴趣_精华液
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (3, 1, 1);  -- C001 KOL测评15s

-- S4: 腾讯·弃单重定向, budget=80000, 10%, target_cpa=200
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, updated_by, created_at, updated_at)
VALUES ('腾讯·弃单重定向', 'S4', 1, 'RETARGET', '针对弃单人群进行限时优惠触达，追回流失订单', 80000.00, 10.00, 200.00, 2.50, 4, 'system', 'system', NOW(), NOW());
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (4, 'TENCENT', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (4, 6, 0);  -- AUD006 弃单人群_24h
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (4, 6, 1);  -- C006 限时优惠海报

-- S5: 百度·竞品截流, budget=120000, 15%, target_cpa=250
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, updated_by, created_at, updated_at)
VALUES ('百度·竞品截流', 'S5', 1, 'CONVERT', '针对竞品兴趣人群投放成分对比素材，抢夺竞品意向用户', 120000.00, 15.00, 250.00, 1.80, 5, 'system', 'system', NOW(), NOW());
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (5, 'BAIDU_FEED', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (5, 3, 0);  -- AUD003 竞品种草_HBN
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (5, 8, 1);  -- C008 成分对比测评

-- S6: 品牌搜索防御, budget=160000, 20%, target_cpa=150
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, updated_by, created_at, updated_at)
VALUES ('品牌搜索防御', 'S6', 1, 'CONVERT', '品牌词搜索拦截，覆盖首触+末触28%归因订单', 160000.00, 20.00, 150.00, 3.00, 6, 'system', 'system', NOW(), NOW());
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (6, 'BAIDU_SEARCH', 60.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (6, 'DOUYIN', 40.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (6, 12, 0);  -- AUD012 品牌词搜索人群
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (6, 11, 1);  -- C011 明星测评精剪

-- S7: AI·智能优选通投, budget=140000, 17.5%, target_cpa=280
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, updated_by, created_at, updated_at)
VALUES ('AI·智能优选通投', 'S7', 0, 'CONVERT', '全渠道通投验证，AI智能优选素材和人群组合', 140000.00, 17.50, 280.00, 1.50, 7, 'system', 'system', NOW(), NOW());
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (7, 'DOUYIN', 30.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (7, 'XIAOHONGSHU', 25.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (7, 'BILIBILI', 20.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (7, 'TENCENT', 15.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (7, 'BAIDU_FEED', 10.00);

-- Audiences (13 records from AUD001 to AUD013)
INSERT INTO ad_audience (id, name, code, source, size_estimate, status, created_by, created_at, updated_at)
VALUES
(1, '美妆兴趣_精华液', 'AUD001', 'DMP', 500000, 0, 'system', NOW(), NOW()),
(2, '美妆兴趣_美白', 'AUD002', 'DMP', 350000, 0, 'system', NOW(), NOW()),
(3, '竞品种草_HBN', 'AUD003', 'DMP', 200000, 0, 'system', NOW(), NOW()),
(4, '成分党_烟酰胺', 'AUD004', 'DMP', 150000, 0, 'system', NOW(), NOW()),
(5, 'Lookalike_老客_1%', 'AUD005', 'LOOKALIKE', 80000, 0, 'system', NOW(), NOW()),
(6, '弃单人群_24h', 'AUD006', 'RETARGET', 2400, 0, 'system', NOW(), NOW()),
(7, 'Lookalike_老客_2%', 'AUD007', 'LOOKALIKE', 150000, 0, 'system', NOW(), NOW()),
(8, 'DMP_拉新_v2', 'AUD008', 'DMP', 280000, 0, 'system', NOW(), NOW()),
(9, '测试包_勿删', 'AUD009', 'DMP', 500, 1, 'system', NOW(), NOW()),
(10, 'DMP_精华_拉新_v3', 'AUD010', 'DMP', 320000, 0, 'system', NOW(), NOW()),
(11, 'DMP_粉丝_page', 'AUD011', 'DMP', 45000, 0, 'system', NOW(), NOW()),
(12, '品牌词搜索人群', 'AUD012', 'DMP', 100000, 0, 'system', NOW(), NOW()),
(13, 'DMP_拉新_v4', 'AUD013', 'DMP', 300000, 0, 'system', NOW(), NOW());

-- Materials (C001-C012)
INSERT INTO ad_material (id, name, code, type, duration, status, score, created_by, created_at, updated_at)
VALUES
(1, 'KOL测评15s', 'C001', 1, 15, 1, 85.00, 'system', NOW(), NOW()),
(2, '成分图解析', 'C002', 2, 0, 1, 75.00, 'system', NOW(), NOW()),
(3, '品牌故事30s', 'C003', 1, 30, 1, 70.00, 'system', NOW(), NOW()),
(4, '新品首发15s', 'C004', 1, 15, 1, 80.00, 'system', NOW(), NOW()),
(5, '精华液痛点', 'C005', 3, 0, 1, 72.00, 'system', NOW(), NOW()),
(6, '限时优惠海报', 'C006', 2, 0, 1, 78.00, 'system', NOW(), NOW()),
(7, 'KOC种草30s', 'C007', 1, 30, 1, 92.00, 'system', NOW(), NOW()),
(8, '成分对比测评', 'C008', 3, 0, 1, 82.00, 'system', NOW(), NOW()),
(9, '试用装领取', 'C009', 2, 0, 1, 68.00, 'system', NOW(), NOW()),
(10, '达播混剪B站', 'C010', 1, 30, 1, 76.00, 'system', NOW(), NOW()),
(11, '明星测评精剪', 'C011', 1, 15, 1, 90.00, 'system', NOW(), NOW()),
(12, '成分溯源', 'C012', 1, 30, 0, 65.00, 'system', NOW(), NOW());

-- Built-in system rule: test campaign detection
INSERT INTO ad_rule (name, trigger_metric, trigger_operator, trigger_threshold, trigger_window_hours, action_type, action_params, scope_type, priority, cooldown_minutes, is_system, status, created_by, created_at, updated_at)
VALUES ('测试计划自动检测', 'CONSUME', 'GT', 500, 1, 'PAUSE_CAMPAIGN', '{"reason":"疑似测试计划"}', 'CAMPAIGN', 100, 1440, 1, 1, 'system', NOW(), NOW());

-- Rule: CPA spike protection
INSERT INTO ad_rule (name, trigger_metric, trigger_operator, trigger_threshold, trigger_window_hours, action_type, action_params, scope_type, priority, cooldown_minutes, is_system, status, created_by, created_at, updated_at)
VALUES ('CPA超标自动暂停', 'CPA', 'GT', 500, 24, 'PAUSE_CAMPAIGN', '{"reason":"CPA连续超标","alert":true}', 'STRATEGY', 90, 1440, 0, 1, 'system', NOW(), NOW());
```

- [ ] **Step 1:** Create `seed-data.sql` with all INSERT statements
- [ ] **Step 2:** Execute against MySQL: `mysql -u root -p < backend/src/main/resources/db/seed-data.sql`
- [ ] **Step 3:** Verify: `SELECT COUNT(*) FROM ad_strategy` returns 7, `SELECT COUNT(*) FROM ad_audience` returns 13
