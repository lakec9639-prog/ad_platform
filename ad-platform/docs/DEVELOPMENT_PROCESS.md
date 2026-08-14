# LUMI ADX 程序化广告平台 — 开发过程记录

> 项目周期：2026-07-17 · 版本 V1.0.0

---

## 一、项目概述

LUMI ADX 是一个 DSP-ADX-SSP 一体化的程序化广告平台，支持实时竞价（RTB）、多渠道投放管理、策略决策与效果分析。采用 **双服务架构**：

| 服务 | 端口 | 技术栈 | 职责 |
|------|------|--------|------|
| **Management Service** | 8080 | Spring Boot 3.2.5 + MyBatis-Plus + MySQL + Redis | 策略/广告组/人群/素材 CRUD、渠道账号、规则引擎、仪表盘 |
| **Bidding Service** | 9090 | Vert.x 4.5.10 + Jackson + Redis + MySQL | SSP 网关、ADX 竞价引擎、DSP 决策、跟踪服务器、实时指标 |

**项目模块：** Maven 多模块（`ad-platform-parent` → `backend` + `bidding-service`）

---

## 二、架构设计

### RTB 竞价决策流程

```
媒体请求 → SSP(/ad/request) → ADX Engine → DSP Decision Engine
                                              ├── StrategyMatcher (S4→S3→S1→S2→S5→S6)
                                              ├── Pricer (CPA × bidRate × 10)
                                              ├── BudgetEngine (hasBudget → deduct → floor at 0)
                                              └── 返回 BidResponse (含监测 URL)
                     ← 响应广告 HTML ← 跟踪服务器 (imp/click/conv)
```

### 6 大 RTB 策略优先级

| 优先级 | 策略 | 编码 | 目标 | 出价系数 |
|--------|------|------|------|---------|
| 1 (最高) | 弃单重定向强转化 | S4 | 加购未下单用户召回 | 0.6 |
| 2 | 竞品截流抢夺 | S3 | 拦截竞品流量 | 0.5 |
| 3 | 高价值人群精准转化 | S1 | ROI 最大化 | 0.4 |
| 4 | 新品破圈拉新 | S2 | 降低拉新成本 | 0.25 |
| 5 | 智能通投探索 | S5 | 补充预算消耗 | 0.15 |
| 6 (兜底) | 兜底保量 | S6 | 填充剩余流量 | 0.10 |

### 数据库表结构

`ad_platform` 数据库（MySQL 8.0）：

| # | 表名 | 用途 | 创建 |
|---|------|------|------|
| 1 | `ad_strategy` | 投放策略 | init-schema |
| 2 | `ad_strategy_channel` | 策略渠道关联 | init-schema |
| 3 | `ad_audience` | 人群 | init-schema |
| 4 | `ad_material` | 素材 | init-schema |
| 5 | `ad_strategy_audience` | 策略人群关联 | init-schema |
| 6 | `ad_strategy_material` | 策略素材关联 | init-schema |
| 7 | `ad_campaign` | 广告计划 | init-schema |
| 8 | `ad_rule` | 规则 | init-schema |
| 9 | `ad_rule_execution_log` | 规则执行日志 | init-schema |
| 10 | `ad_stats_hourly` | 小时级统计数据 | init-schema |
| 11 | `ad_publisher` | 媒体方 | migration-v2 |
| 12 | `ad_ad_slot` | 广告位 | migration-v2 |
| 13 | `ad_bid_log` | 竞价日志 | migration-v2 |
| 14 | `ad_tracking_log` | 监播日志 | migration-v2 |
| 15 | `ad_channel_account` | 渠道账号 | migration-v3 |

---

## 三、开发阶段

### Phase 1：核心 RTB 链路打通

#### 任务 1：多模块重构 + 数据库迁移
- **创建** `ad-platform/pom.xml` 聚合父 POM，继承 `spring-boot-starter-parent`
- **修改** `backend/pom.xml` 改为子模块
- **创建** `bidding-service/pom.xml`（Vert.x 4.5.10 + Jackson + Lombok + SLF4J）
- **创建** `BiddingApplication.java` 入口 + `MainVerticle.java` HTTP 服务
- **创建** `migration-v2.sql`：publisher / ad_slot / bid_log / tracking_log + strategy RTB 字段

#### 任务 2：Bidding Service 骨架
- `config.json`（端口 9090、Redis/MySQL 配置、50ms 竞价超时）
- `RedisClientFactory.java`、Logback 配置、健康端点 `/health`

#### 任务 3：SSP 网关
- `BidRequest.java` / `BidResponse.java` / `AdResponse.java` 模型
- `SspHandler.java`：POST `/ad/request` → 解析/验证/丰富 → 调用 ADX → 返回广告 HTML

#### 任务 4：管理端 Publisher + AdSlot CRUD
- 实体、DTO、Mapper、Service、Controller 全链路（各 14 个文件）
- `GET/POST/PUT/DELETE /api/v1/publishers` + `/api/v1/ad-slots`

#### 任务 5：ADX 引擎 + DSP 决策引擎
- `CampaignConfig.java`（@Builder 模式，含 MaterialOption 内部类）
- `StrategyMatcher.java`：设备 ID 前缀匹配（hv-/rt-/cp-/new-/unknown-），频控 + 时段校验
- `Pricer.java`：出价公式 `targetCPA × bidRate × 10`
- `BudgetEngine.java`：ConcurrentHashMap 预算管理，5 策略初始预算
- `DspDecisionEngine.java`：编排管道（match → price → floor check → deduct → material → track URL）
- `AdxEngine.java`：包装器，BID_WIN/BID_LOSE 日志

#### 任务 6：跟踪服务器
- `EventLogger.java`：TSV 格式日志
- `TrackingHandler.java`：1×1 透明 GIF（imp/click/conv）+ 302 跳转（landing）

#### 任务 7：Strategy RTB 部署
- `StrategyDeployController.java`：deploy/undeploy/deploy-status
- `StrategyDeployServiceImpl.java`：Redis Hash + Pub/Sub `config:changed`

#### 任务 8：Docker Compose + 模拟器
- `docker-compose.yml`（MySQL 8.0 + Redis 7）
- `MediaSimulator.java`：Vert.x WebClient，随机设备前缀，QPS 实时输出
- `run-demo.sh`：全自动启动脚本

#### 任务 9：单元测试
- `PricerTest.java`：出价计算、null 处理、四舍五入
- `BudgetEngineTest.java`：预算检查、扣减、归零、重置
- `StrategyMatcherTest.java`：设备匹配、频控阻断
- `DspDecisionEngineTest.java`：完整管道（胜出/无匹配/底价不足）

#### 任务 10：实时仪表盘 + 增强模拟器
- `MetricsCollector.java`：按策略聚合的 ConcurrentHashMap 指标
- `StatsHandler.java`：GET `/stats` JSON 快照
- `DashboardHandler.java`：Chart.js HTML 仪表盘（2 秒自动刷新）
- `MediaSimulator.java` 重写：50 QPS 持续压力 + 用户旅程模拟（bid→imp→click→conv）

### Phase 1.5：功能补全

#### 策略描述支持
- `seed-data-bulk.sql` 策略增加详细中文描述
- 列表页添加"编辑"按钮与弹窗
- 详情页添加描述展示行 + 编辑对话框

#### 渠道账号管理
- **创建** `migration-v3.sql`：`ad_channel_account` 表 + 5 条种子数据
- 后端：Controller/Service/Mapper/DTO 全链路 CRUD（8 个文件）
- 前端：API 封装 + SettingsPage 完整表格/添加/编辑/删除

---

## 四、后端文件清单

### Management Service（`backend/src/main/java/com/ad/`）

| 包 | 文件 | 职责 |
|----|------|------|
| — | `AdApplication.java` | Spring Boot 入口 |
| `common` | `BaseEntity.java`, `Result.java`, `PageResult.java` | 基础类 |
| `config` | `CorsConfig.java`, `RedisConfig.java`, `MyBatisPlusConfig.java` | 配置 |
| `enums` | `StrategyStatus.java`, `CampaignStatus.java`, `Channel.java` 等 11 个枚举 | 业务枚举 |
| `entity` | `Strategy.java`, `Campaign.java`, `Audience.java`, `Material.java`, `Publisher.java`, `AdSlot.java`, `ChannelAccount.java`, `Rule.java` 等 15+ | 数据实体 |
| `mapper` | 对应 11 个 Mapper 接口 | MyBatis-Plus 数据访问 |
| `dto` | StrategyDTO/CreateDTO, CampaignDTO/CreateDTO, PublisherDTO/CreateDTO, ChannelAccountDTO/CreateDTO 等 15+ | 数据传输对象 |
| `service` | 9 个 Service 接口 + Impl | 业务逻辑 |
| `controller` | 10 个 REST Controller | API 端点 |

### Bidding Service（`bidding-service/src/main/java/com/ad/bidding/`）

| 包 | 文件 | 职责 |
|----|------|------|
| — | `BiddingApplication.java` | Vert.x 入口 |
| `config` | `RedisClientFactory.java` | Redis 客户端工厂 |
| `model` | `BidRequest.java`, `BidResponse.java`, `AdResponse.java`, `CampaignConfig.java` | 竞价模型 |
| `engine` | `StrategyMatcher.java`, `Pricer.java`, `BudgetEngine.java`, `DspDecisionEngine.java`, `AdxEngine.java` | 竞价决策 |
| `handler` | `SspHandler.java`, `TrackingHandler.java`, `StatsHandler.java`, `DashboardHandler.java` | HTTP 路由处理 |
| `tracker` | `EventLogger.java` | 事件日志 |
| `verticle` | `MainVerticle.java` | Vert.x 主 Verticle |
| `simulator` | `MediaSimulator.java` | 流量模拟器 |
| `stats` | `MetricsCollector.java` | 实时指标 |

---

## 五、前端路由与页面

| 路径 | 页面 | 功能 |
|------|------|------|
| `/dashboard` | 仪表盘 | 趋势图、渠道分布、物料排行、核心 KPI |
| `/strategy/list` | 策略列表 | 策略卡片 + 编辑/暂停/详情 |
| `/strategy/:id` | 策略详情 | 基本信息 + 渠道分配 + 编辑 |
| `/campaign/list` | 广告组列表 | 广告组 CRUD + 状态管理 |
| `/campaign/:id` | 广告组详情 | 广告组详细信息 |
| `/audience` | 人群管理 | 人群 CRUD |
| `/material/list` | 素材列表 | 素材 CRUD |
| `/material/analysis` | 素材衰减分析 | 素材评分趋势 |
| `/rule-engine` | 规则引擎 | 自动化规则 + 沙箱测试 |
| `/settings` | 系统设置 | 预算总览 + 渠道账号管理 |

---

## 六、数据库迁移清单

| 文件 | 新增表 | 说明 |
|------|--------|------|
| `init-schema.sql` | 10 张基础表 | 原始建表（策略/广告组/素材/人群/规则/统计） |
| `migration-v2.sql` | 4 张表 + strategy 扩展 | DSP-ADX-SSP 竞价相关表 |
| `migration-v3.sql` | `ad_channel_account` | 渠道账号管理 |

---

## 七、已知问题 & 待办

| 优先级 | 事项 |
|--------|------|
| P2 | Redis Pub/Sub 数据同步（Management ↔ Bidding） |
| P2 | 预算实时更新推送（WebSocket 或轮询） |
| P2 | 竞价日志入库（ad_bid_log / ad_tracking_log 持久化） |
| P3 | 品牌搜索防守（Strategy 7，独立搜索 API） |
| P3 | 规则引擎触发真正 RTB 操作（目前仅状态变更） |
| P3 | OAuth 认证 + 操作审计日志 |
| P4 | Web 前端用户管理 / 角色权限 |

---

## 八、启动方式

```bash
# 1. 数据库初始化
mysql -u root -p1234 ad_platform < backend/src/main/resources/db/init-schema.sql
mysql -u root -p1234 ad_platform < backend/src/main/resources/db/migration-v2.sql
mysql -u root -p1234 ad_platform < backend/src/main/resources/db/migration-v3.sql
mysql -u root -p1234 ad_platform < backend/src/main/resources/db/seed-data-bulk.sql

# 2. 启动 Redis
docker compose up -d redis

# 3. 启动 Management Service（端口 8080）
cd backend && mvn spring-boot:run

# 4. 启动 Bidding Service（端口 9090）
cd bidding-service && mvn compile exec:java -Dexec.mainClass="com.ad.bidding.BiddingApplication"

# 5. 启动前端
cd frontend && npm run dev

# 6. 运行压力测试
cd bidding-service && mvn compile exec:java -Dexec.mainClass="com.ad.bidding.simulator.MediaSimulator" -Dexec.args="500 50"

# 7. 查看仪表盘
# http://localhost:9090/dashboard
```

---

## 九、技术栈

| 类别 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, Vert.x 4.5.10 |
| 数据库 | MySQL 8.0, Redis 7 |
| 前端 | Vue 3, TypeScript, Element Plus, Chart.js |
| 构建 | Maven (multi-module), Vite |
| 监测 | 1×1 透明像素（imp/click）, 302 跳转（land） |
