# LUMI 投放中台 — Phase 1: 后端核心层

> **For agentic workers:** This is Phase 1 of the implementation plan. Implement all tasks in order within this phase.

**Goal:** Build the Spring Boot backend foundation: project scaffold, common classes, enums, entities, mappers, and database schema.

**Architecture:** Standard Spring Boot layered architecture with MyBatis-Plus. All entities extend BaseEntity with optimistic locking and soft delete. Mappers use MyBatis-Plus BaseMapper for CRUD and XML for complex aggregations.

---

## Global Constraints (All Phases)

- Java 17, Spring Boot 3.2.x, MyBatis-Plus 3.5.x, MySQL 8.0, Redis 7.x
- Vue 3.4+ with Composition API + `<script setup>`, Vite 5.x
- Element Plus 2.5+, ECharts 5.5+, Pinia 2.x, Axios 1.x
- API prefix: `/api/v1`, Response format: `{ code: 0, data: {...}, message: "ok" }`
- Dates: ISO 8601 (`yyyy-MM-dd`), datetime `yyyy-MM-dd HH:mm:ss`
- Soft delete on all tables, version field for optimistic locking
- All files under `ad-platform/` directory
- Controller returns DTO, never Entity directly
- Naming: Java camelCase, SQL snake_case, frontend kebab-case for files

---

### Task 1.a: Maven POM + Application Config

**Files:**
- Create: `ad-platform/backend/pom.xml`
- Create: `ad-platform/backend/src/main/resources/application.yml`
- Create: `ad-platform/backend/src/main/java/com/ad/AdApplication.java`

**Interfaces:**
- Consumes: nothing (this is the entry point)
- Produces: Spring Boot application context, Maven dependencies

<details>
<summary>pom.xml (expand)</summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>
    <groupId>com.ad</groupId>
    <artifactId>ad-platform</artifactId>
    <version>1.0.0</version>
    <name>LUMI AD Platform</name>
    <description>LUMI Programmatic Advertising Platform</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.6</mybatis-plus.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
</details>

<details>
<summary>application.yml</summary>

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: ad-platform
  datasource:
    url: jdbc:mysql://localhost:3306/ad_platform?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    property-naming-strategy: LOWER_CAMEL_CASE

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.ad.entity
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```
</details>

<details>
<summary>AdApplication.java</summary>

```java
package com.ad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdApplication.class, args);
    }
}
```
</details>

- [ ] **Step 1:** Create `pom.xml` with all dependencies
- [ ] **Step 2:** Create `application.yml` with datasource, redis, mybatis config
- [ ] **Step 3:** Create `AdApplication.java` main class
- [ ] **Step 4:** Verify Maven compiles: `cd ad-platform/backend && mvn compile -q`

---

### Task 1.b: Common Classes + Config

**Files:**
- Create: `ad-platform/backend/src/main/java/com/ad/common/BaseEntity.java`
- Create: `ad-platform/backend/src/main/java/com/ad/common/Result.java`
- Create: `ad-platform/backend/src/main/java/com/ad/common/PageResult.java`
- Create: `ad-platform/backend/src/main/java/com/ad/config/CorsConfig.java`
- Create: `ad-platform/backend/src/main/java/com/ad/config/RedisConfig.java`
- Create: `ad-platform/backend/src/main/java/com/ad/config/MyBatisPlusConfig.java`

**Interfaces:**
- Consumes: Spring Boot context from Task 1.a
- Produces: `BaseEntity`, `Result<T>`, `PageResult<T>` shared by all service layers

```java
// BaseEntity.java
package com.ad.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

```java
// Result.java
package com.ad.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result() {}

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.message = message;
        return r;
    }
}
```

```java
// PageResult.java
package com.ad.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.list = page.getRecords();
        r.total = page.getTotal();
        r.page = page.getCurrent();
        r.size = page.getSize();
        return r;
    }
}
```

- [ ] **Step 1:** Create `BaseEntity.java` with id, version, deleted, createdBy, updatedBy, createdAt, updatedAt
- [ ] **Step 2:** Create `Result.java` with ok() and fail() static factories
- [ ] **Step 3:** Create `PageResult.java` with of() factory from IPage
- [ ] **Step 4:** Create `CorsConfig.java` allowing localhost:5173 origin
- [ ] **Step 5:** Create `RedisConfig.java` with RedisTemplate<String, Object> and Jackson2JsonRedisSerializer
- [ ] **Step 6:** Create `MyBatisPlusConfig.java` with @MapperScan("com.ad.mapper")
- [ ] **Step 7:** Verify compile: `cd ad-platform/backend && mvn compile -q`

---

### Task 1.c: Enums

**Files:** Create all files under `ad-platform/backend/src/main/java/com/ad/enums/`

**Interfaces:**
- Consumes: nothing
- Produces: Enum types used by entities and DTOs

Create these enum files:

**Channel.java:**
```java
package com.ad.enums;

public enum Channel {
    DOUYIN("巨量引擎"),
    XIAOHONGSHU("小红书"),
    BILIBILI("B站"),
    TENCENT("腾讯广告"),
    BAIDU_FEED("百度信息流"),
    BAIDU_SEARCH("百度搜索");

    public final String label;
    Channel(String label) { this.label = label; }
}
```

**StrategyStatus.java:**
```java
package com.ad.enums;

public enum StrategyStatus {
    DRAFT(0, "草稿"),
    ACTIVE(1, "启用"),
    PAUSED(2, "暂停"),
    ENDED(3, "结束");

    public final int code;
    public final String label;
    StrategyStatus(int code, String label) { this.code = code; this.label = label; }

    public static StrategyStatus fromCode(int code) {
        for (StrategyStatus s : values()) if (s.code == code) return s;
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
```

**Create remaining enums following same pattern:**
- `CampaignStatus.java` — SETUP(0), RUNNING(1), PAUSED(2), STOPPED(3)
- `RuleActionType.java` — PAUSE_CAMPAIGN, ACTIVATE_CAMPAIGN, RAISE_BID, LOWER_BID, SWAP_MATERIAL, ADJUST_BUDGET, SEND_ALERT (use string values, no code)
- `AudienceSource.java` — DMP, LOOKALIKE, RETARGET
- `MaterialType.java` — VIDEO(1), IMAGE(2), IMAGE_TEXT(3)
- `MaterialStatus.java` — PENDING(0), ACTIVE(1), DECAYING(2), STOPPED(3)
- `TriggerMetric.java` — CPA, CTR, CVR, CONSUME
- `Operator.java` — GT, LT, GTE, LTE
- `AudienceStrategyType.java` — MAIN(0), EXTEND(1), EXCLUDE(2)
- `ScopeType.java` — STRATEGY, CHANNEL, CAMPAIGN
- `RuleExecutionResult.java` — FAIL(0), SUCCESS(1)

- [ ] **Step 1:** Create all enum files with proper code/label fields
- [ ] **Step 2:** Verify compile: `cd ad-platform/backend && mvn compile -q`

---

### Task 1.d: Database Schema SQL

**Files:**
- Create: `ad-platform/backend/src/main/resources/db/init-schema.sql`

**Interfaces:**
- Consumes: nothing
- Produces: 10 MySQL tables with indexes, partitions, and FK constraints

Create the full schema with these 10 tables:

```sql
-- ad_platform database
CREATE DATABASE IF NOT EXISTS ad_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ad_platform;

-- 1. ad_strategy
CREATE TABLE ad_strategy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '策略名称',
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '编码 S1-S7',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-草稿 1-启用 2-暂停 3-结束',
    objective VARCHAR(20) NOT NULL COMMENT 'CONVERT/BRAND/RETARGET',
    description VARCHAR(500) COMMENT '策略描述',
    budget DECIMAL(12,2) DEFAULT 0 COMMENT '分配预算（元）',
    budget_ratio DECIMAL(5,2) DEFAULT 0 COMMENT '预算占比（%）',
    target_cpa DECIMAL(10,2) COMMENT '目标CPA',
    target_cvr DECIMAL(5,4) COMMENT '目标CVR',
    expected_roas DECIMAL(5,2) COMMENT '预期ROAS',
    sort_order INT DEFAULT 0 COMMENT '排序',
    version INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB COMMENT='策略表';

-- 2. ad_strategy_channel
CREATE TABLE ad_strategy_channel (
    strategy_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL COMMENT '渠道编码',
    budget_ratio DECIMAL(5,2) COMMENT '该渠道预算占比',
    PRIMARY KEY (strategy_id, channel)
) ENGINE=InnoDB COMMENT='策略渠道分配';

-- 3. ad_audience
CREATE TABLE ad_audience (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '人群包名称',
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '人群编码',
    source VARCHAR(20) NOT NULL COMMENT 'DMP/LOOKALIKE/RETARGET',
    size_estimate INT DEFAULT 0 COMMENT '预估人群规模',
    status TINYINT DEFAULT 0 COMMENT '0-可用 1-暂停',
    version INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB COMMENT='人群包';

-- 4. ad_material
CREATE TABLE ad_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '素材名称',
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '素材编码',
    type TINYINT NOT NULL COMMENT '1-视频 2-图片 3-图文',
    duration INT DEFAULT 0 COMMENT '视频时长(秒)',
    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-可用 2-衰减 3-停用',
    score DECIMAL(5,2) DEFAULT 0 COMMENT '综合评分',
    version INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB COMMENT='素材库';

-- 5. ad_strategy_audience
CREATE TABLE ad_strategy_audience (
    strategy_id BIGINT NOT NULL,
    audience_id BIGINT NOT NULL,
    type TINYINT DEFAULT 0 COMMENT '0-主人群 1-扩展 2-排除',
    PRIMARY KEY (strategy_id, audience_id)
) ENGINE=InnoDB COMMENT='策略人群关联';

-- 6. ad_strategy_material
CREATE TABLE ad_strategy_material (
    strategy_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0 COMMENT '投放优先级',
    PRIMARY KEY (strategy_id, material_id)
) ENGINE=InnoDB COMMENT='策略素材关联';

-- 7. ad_campaign
CREATE TABLE ad_campaign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_id BIGINT COMMENT '归属策略',
    name VARCHAR(100) NOT NULL COMMENT '计划名称',
    channel VARCHAR(20) NOT NULL COMMENT '投放渠道',
    platform_campaign_id VARCHAR(64) COMMENT '平台侧计划ID',
    budget_daily DECIMAL(12,2) DEFAULT 0 COMMENT '日预算',
    bid_type VARCHAR(10) COMMENT 'OCPM/CPC/CPM',
    bid_price DECIMAL(10,2) COMMENT '出价',
    status TINYINT DEFAULT 0 COMMENT '0-搭建中 1-投放中 2-暂停 3-关停',
    launch_at DATETIME COMMENT '开始投放时间',
    stop_at DATETIME COMMENT '停止时间',
    version INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_strategy (strategy_id),
    INDEX idx_channel_status (channel, status)
) ENGINE=InnoDB COMMENT='广告计划';

-- 8. ad_rule
CREATE TABLE ad_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    trigger_metric VARCHAR(30) NOT NULL COMMENT '触发指标 CPA/CTR/CVR/CONSUME',
    trigger_operator VARCHAR(10) NOT NULL COMMENT 'GT/LT/GTE/LTE',
    trigger_threshold DECIMAL(12,2) NOT NULL COMMENT '触发阈值',
    trigger_window_hours INT DEFAULT 1 COMMENT '观察窗口(小时)',
    action_type VARCHAR(30) NOT NULL COMMENT 'PAUSE/RAISE_BID/LOWER_BID/SWAP_MATERIAL',
    action_params JSON COMMENT '动作参数字典',
    scope_type VARCHAR(20) COMMENT 'STRATEGY/CHANNEL/CAMPAIGN',
    scope_value VARCHAR(100) COMMENT '作用域值',
    priority INT DEFAULT 0 COMMENT '优先级',
    cooldown_minutes INT DEFAULT 60 COMMENT '冷却期(分钟)',
    is_system TINYINT DEFAULT 0 COMMENT '1-系统内置(不可删除/禁用)',
    status TINYINT DEFAULT 0 COMMENT '0-禁用 1-启用',
    version INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB COMMENT='自动化规则';

-- 9. ad_rule_execution_log
CREATE TABLE ad_rule_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT COMMENT '规则ID',
    campaign_id BIGINT COMMENT '触发计划ID',
    trigger_value DECIMAL(12,2) COMMENT '触发实时值',
    action_taken VARCHAR(100) COMMENT '动作描述',
    result TINYINT COMMENT '0-失败 1-成功',
    error_message VARCHAR(500) COMMENT '失败原因',
    executed_at DATETIME COMMENT '执行时间',
    INDEX idx_rule_executed (executed_at),
    INDEX idx_rule_campaign (rule_id, campaign_id)
) ENGINE=InnoDB COMMENT='规则执行日志';

-- 10. ad_stats_hourly (partitioned by month)
CREATE TABLE ad_stats_hourly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel VARCHAR(20) NOT NULL COMMENT '渠道',
    strategy_id BIGINT COMMENT '策略ID',
    campaign_id BIGINT COMMENT '计划ID',
    stat_date DATE NOT NULL COMMENT '日期',
    stat_hour TINYINT NOT NULL COMMENT '小时',
    impressions BIGINT DEFAULT 0 COMMENT '曝光',
    clicks INT DEFAULT 0 COMMENT '点击',
    micro_conversions INT DEFAULT 0 COMMENT '微转化数',
    conversions INT DEFAULT 0 COMMENT '转化',
    cost DECIMAL(12,2) DEFAULT 0 COMMENT '消耗',
    gmv DECIMAL(14,2) DEFAULT 0 COMMENT '收入',
    new_users INT DEFAULT 0 COMMENT '新客数',
    INDEX idx_date_channel_strategy (stat_date, channel, strategy_id),
    INDEX idx_campaign_date (campaign_id, stat_date)
) ENGINE=InnoDB COMMENT='小时级统计数据'
PARTITION BY RANGE (YEAR(stat_date) * 100 + MONTH(stat_date)) (
    PARTITION p202607 VALUES LESS THAN (202608),
    PARTITION p202608 VALUES LESS THAN (202609),
    PARTITION p202609 VALUES LESS THAN (202610),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

- [ ] **Step 1:** Create `init-schema.sql` with all 10 table DDL statements
- [ ] **Step 2:** Execute against MySQL: `mysql -u root -p < backend/src/main/resources/db/init-schema.sql`
- [ ] **Step 3:** Verify all tables exist: `mysql -u root -p -e "USE ad_platform; SHOW TABLES;"`

---

### Task 1.e: All Entities

**Files:** Create each file under `ad-platform/backend/src/main/java/com/ad/entity/`

- `Strategy.java`
- `StrategyChannel.java`
- `Audience.java`
- `Material.java`
- `StrategyAudience.java`
- `StrategyMaterial.java`
- `Campaign.java`
- `Rule.java`
- `RuleExecutionLog.java`
- `StatsHourly.java`

**Interfaces:**
- Consumes: BaseEntity from Task 1.b, enums from Task 1.c
- Produces: Entity classes mapped to all 10 tables

```java
// Strategy.java
package com.ad.entity;

import com.ad.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_strategy")
public class Strategy extends BaseEntity {
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
}
```

```java
// StrategyChannel.java
package com.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("ad_strategy_channel")
public class StrategyChannel {
    private Long strategyId;
    private String channel;
    private BigDecimal budgetRatio;
}
```

Create remaining entities following the same pattern. Key details per entity:

- **Audience:** fields: name, code, source, sizeEstimate, status (no @TableId needed — extends BaseEntity which has it)
- **Material:** fields: name, code, type, duration, status, score
- **StrategyAudience:** fields: strategyId, audienceId, type (no BaseEntity — it's a join table without id/version/deleted)
- **StrategyMaterial:** fields: strategyId, materialId, sortOrder (join table)
- **Campaign:** fields: strategyId, name, channel, platformCampaignId, budgetDaily, bidType, bidPrice, status, launchAt, stopAt
- **Rule:** fields: name, triggerMetric, triggerOperator, triggerThreshold, triggerWindowHours, actionType, actionParams (String — JSON stored as text), scopeType, scopeValue, priority, cooldownMinutes, isSystem, status
- **RuleExecutionLog:** fields: ruleId, campaignId, triggerValue, actionTaken, result, errorMessage, executedAt (no BaseEntity — it's an audit log without version/deleted)
- **StatsHourly:** fields: channel, strategyId, campaignId, statDate, statHour, impressions, clicks, microConversions, conversions, cost, gmv, newUsers (no BaseEntity — fact table, no version/deleted)

- [ ] **Step 1:** Create `Strategy.java`, `StrategyChannel.java`
- [ ] **Step 2:** Create `Audience.java`, `Material.java`, `StrategyAudience.java`, `StrategyMaterial.java`
- [ ] **Step 3:** Create `Campaign.java`
- [ ] **Step 4:** Create `Rule.java`, `RuleExecutionLog.java`
- [ ] **Step 5:** Create `StatsHourly.java`
- [ ] **Step 6:** Verify compile: `cd ad-platform/backend && mvn compile -q`

---

### Task 1.f: All Mappers + XML

**Files:** Create under `ad-platform/backend/src/main/java/com/ad/mapper/` and `ad-platform/backend/src/main/resources/mapper/`

**Mappers (Java interfaces):**
- `StrategyMapper.java`
- `StrategyChannelMapper.java`
- `AudienceMapper.java`
- `MaterialMapper.java`
- `StrategyAudienceMapper.java`
- `StrategyMaterialMapper.java`
- `CampaignMapper.java`
- `RuleMapper.java`
- `RuleExecutionLogMapper.java`
- `StatsHourlyMapper.java`

**XML files (under `resources/mapper/`):**
- `StrategyMapper.xml`
- `CampaignMapper.xml`
- `StatsHourlyMapper.xml`
- `RuleExecutionLogMapper.xml`

**Interfaces:**
- Consumes: Entities from Task 1.e
- Produces: Data access layer used by all services

```java
// StrategyMapper.java
package com.ad.mapper;

import com.ad.entity.Strategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StrategyMapper extends BaseMapper<Strategy> {
}
```

All simple CRUD mappers follow the exact same pattern (BaseMapper extension). Create for: StrategyChannelMapper, AudienceMapper, MaterialMapper, StrategyAudienceMapper, StrategyMaterialMapper, CampaignMapper, RuleMapper.

**CampaignMapper.xml** — needed for batch status update:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ad.mapper.CampaignMapper">
    <update id="updateBatchStatus">
        UPDATE ad_campaign SET status = #{status}, updated_at = NOW()
        WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        AND deleted = 0
    </update>
</mapper>
```

**StatsHourlyMapper.xml** — dashboard aggregation queries:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ad.mapper.StatsHourlyMapper">
    <!-- Dashboard overview aggregation -->
    <select id="sumByDateRange" resultType="java.util.Map">
        SELECT
            COALESCE(SUM(cost), 0) AS total_cost,
            COALESCE(SUM(conversions), 0) AS total_conversions,
            COALESCE(SUM(new_users), 0) AS total_new_users,
            COALESCE(SUM(gmv), 0) AS total_gmv,
            COALESCE(SUM(impressions), 0) AS total_impressions,
            COALESCE(SUM(clicks), 0) AS total_clicks
        FROM ad_stats_hourly
        WHERE stat_date BETWEEN #{startDate} AND #{endDate}
    </select>

    <!-- Daily trends -->
    <select id="dailyTrends" resultType="java.util.Map">
        SELECT
            stat_date,
            COALESCE(SUM(cost), 0) AS cost,
            COALESCE(SUM(conversions), 0) AS conversions,
            COALESCE(SUM(new_users), 0) AS new_users,
            COALESCE(SUM(gmv), 0) AS gmv,
            COALESCE(SUM(impressions), 0) AS impressions,
            COALESCE(SUM(clicks), 0) AS clicks
        FROM ad_stats_hourly
        WHERE stat_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY stat_date
        ORDER BY stat_date
    </select>

    <!-- Channel distribution -->
    <select id="channelDistribution" resultType="java.util.Map">
        SELECT
            channel,
            COALESCE(SUM(cost), 0) AS cost,
            COALESCE(SUM(conversions), 0) AS conversions,
            COALESCE(SUM(impressions), 0) AS impressions,
            COALESCE(SUM(clicks), 0) AS clicks,
            COALESCE(SUM(gmv), 0) AS gmv
        FROM ad_stats_hourly
        WHERE stat_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY channel
        ORDER BY cost DESC
    </select>

    <!-- Material top N -->
    <select id="materialTop" resultType="java.util.Map">
        SELECT
            campaign_id,
            COALESCE(SUM(cost), 0) AS cost,
            COALESCE(SUM(conversions), 0) AS conversions,
            COALESCE(SUM(clicks), 0) AS clicks,
            COALESCE(SUM(impressions), 0) AS impressions
        FROM ad_stats_hourly
        WHERE stat_date BETWEEN #{startDate} AND #{endDate}
          AND campaign_id IS NOT NULL
        GROUP BY campaign_id
        ORDER BY cost DESC
        LIMIT #{limit}
    </select>

    <!-- Strategy performance summary -->
    <select id="strategyPerformance" resultType="java.util.Map">
        SELECT
            strategy_id,
            COALESCE(SUM(cost), 0) AS cost,
            COALESCE(SUM(conversions), 0) AS conversions,
            COALESCE(SUM(new_users), 0) AS new_users,
            COALESCE(SUM(gmv), 0) AS gmv
        FROM ad_stats_hourly
        WHERE stat_date BETWEEN #{startDate} AND #{endDate}
          AND strategy_id IS NOT NULL
        GROUP BY strategy_id
        ORDER BY cost DESC
    </select>
</mapper>
```

**CampaignMapper.java** — add batch update method:
```java
package com.ad.mapper;

import com.ad.entity.Campaign;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CampaignMapper extends BaseMapper<Campaign> {
    int updateBatchStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);
}
```

**StatsHourlyMapper.java** — add aggregation methods:
```java
package com.ad.mapper;

import com.ad.entity.StatsHourly;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsHourlyMapper extends BaseMapper<StatsHourly> {
    Map<String, Object> sumByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    List<Map<String, Object>> dailyTrends(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    List<Map<String, Object>> channelDistribution(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    List<Map<String, Object>> materialTop(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("limit") int limit);
    List<Map<String, Object>> strategyPerformance(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
```

For `RuleExecutionLogMapper.java`, add method for log queries:
```java
@Mapper
public interface RuleExecutionLogMapper extends BaseMapper<RuleExecutionLog> {
    List<RuleExecutionLog> selectByRuleId(@Param("ruleId") Long ruleId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);
}
```

- [ ] **Step 1:** Create all 10 Mapper Java interfaces (simple ones extend BaseMapper, Campaign/StatsHourly/RuleExecutionLog have extra methods)
- [ ] **Step 2:** Create `CampaignMapper.xml` with `updateBatchStatus`
- [ ] **Step 3:** Create `StatsHourlyMapper.xml` with all 5 aggregation queries
- [ ] **Step 4:** Verify compile: `cd ad-platform/backend && mvn compile`
</details>
