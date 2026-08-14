# LUMI DSP-ADX-SSP 程序化广告全链路平台 — 产品设计文档

> 版本：V1.0
> 日期：2026-07-17
> 技术栈：Vert.x 4.x (Bidding) + Spring Boot 3.x (Management) + MySQL + Redis
> 目标 QPS：1000 | 团队规模：5人

---

## 1. 项目概述

### 1.1 背景

LUMI 广告投放平台已完成 DSP 策略管理、规则引擎、数据看板等管理端核心能力（V1.0），但缺少实时流量交易链路。当前系统不接入任何真实媒体流量，所有数据均为模拟数据。

本次项目将在现有平台基础上，完整构建 **DSP-ADX-SSP 全链路**，打通从媒体广告请求、实时竞价、广告投放、到曝光/点击/转化回传的完整程序化广告交易闭环。

### 1.2 产品定位

面向国内程序化广告市场，两方市场模式（自有 DSP 对接自有 ADX），初期服务 5 个媒体类型和对应的广告主需求，逐步扩展。

### 1.3 核心设计原则

- **零废弃**：现有 Spring Boot 策略管理、规则引擎、数据看板全部保留
- **流量/业务解耦**：Bidding Service (Vert.x) 处理实时竞价，Management Service (Spring Boot) 处理管理端，共享 MySQL + Redis
- **简化接入**：媒体端使用自定义轻量协议而非完整 OpenRTB，降低对接门槛
- **渐进式构建**：全链路拆分为 3 个阶段，每阶段可独立上线验证

### 1.4 北极星指标

- **DSP 填充率** ≥ 60%（Bidding 返回有效出价的比例）
- **ADX 胜出率** ≥ 30%（有意向流量中最终赢得曝光比例）
- **RTB 端到端延迟** < 200ms（P95）

---

## 2. 系统架构

### 2.1 整体架构

```
                         ┌──────────────────────────────────────┐
                         │             Nginx (L7 LB)            │
                         │  /rtb/* → Bidding Service :9090     │
                         │  /api/* → Management Service :8080  │
                         └────────────────┬─────────────────────┘
                                          │
               ┌──────────────────────────┼──────────────────────────┐
               │                          │                          │
          ┌────▼────────────────────┐    │    ┌─────────────────────▼────┐
          │  Bidding Service        │         │  Management Service      │
          │  (Vert.x 4.x)          │    │    │  (Spring Boot 3.x)       │
          │                         │         │                          │
          │  SSP Gateway            │    │    │  策略管理 (现有)          │
          │  ADX Engine             │         │  规则引擎 (现有)          │
          │  DSP Decision Engine    │    │    │  人群/素材/计划 (现有)    │
          │  ─ 策略匹配             │         │  数据看板 (现有)          │
          │  ─ 人群引擎             │    │    │                          │
          │  ─ 频控引擎             │         │  新增：                   │
          │  ─ 出价计算             │    │    │  ─ 媒体/广告位管理        │
          │  ─ 素材匹配             │         │  ─ ADX 配置管理           │
          │  ─ 预算扣减             │    │    │  ─ 结算/对账系统          │
          │                         │         │  ─ 用户画像管理           │
          │  Tracking Server        │    │    │  ─ 运营策略配置           │
          │  Data Sync Module       │         │                          │
          └────┬────────────────────┘    │    └──────┬──────────────────┘
               │                          │          │
               └──────────┬───────────────┘          │
                          │                         │
                     ┌────▼─────────────────────────▼────┐
                     │            Redis Cluster           │
                     │  人群包 / 频控 / 预算 / 策略缓存   │
                     └────────────────┬───────────────────┘
                                      │
                     ┌────────────────▼───────────────────┐
                     │        MySQL (R/W 分离)             │
                     │  策略 / 计划 / 广告位 / 媒体  / 结算│
                     │  竞价日志 / 广告统计 / 用户画像     │
                     └────────────────────────────────────┘
```

### 2.2 两服务职责边界

| 维度 | Bidding Service | Management Service |
|------|----------------|-------------------|
| 框架 | Vert.x 4.x | Spring Boot 3.x |
| 端口 | 9090（RTB） | 8080（管理 API） |
| 实例数 | 3（可水平扩展） | 2 |
| 是否无状态 | 是（信息最终一致即可） | 是 |
| 核心能力 | SSP/ADX/DSP 实时链路 | CRUD + 报表 + 结算 |
| 服务对象 | 媒体 SDK/Server | 运营人员浏览器 |
| 数据一致性 | 最终一致，Redis 缓存驱动 | 强一致，MySQL 主库 |

### 2.3 数据流示意图

```
一次完整的广告生命周期（简化版）：

媒体请求 ←→ SSP ─→ ADX ─→ DSP Decision ←→ Redis(预算/人群/频控)
                              │
                              ├→ 出价成功 → SSP → 媒体渲染
                              │              │
                              │              ├→ 曝光监播 → Tracking → Stats
                              │              ├→ 点击监播 → Tracking → Stats
                              │              └→ 转化回传 → Tracking → Stats
                              │
                              └→ 不出价 → SSP → nbr=2 → 媒体请求下家

Management 侧异步线程：
  Tracking 日志 → 每小时聚合 → ad_stats_hourly
  ad_stats_hourly → 次日结算/报表
  策略配置变更 → MySQL → Redis Pub/Sub → Bidding 缓存刷新
```

---

## 3. 全链路请求流程

### 3.1 一次竞价请求的完整路径

```
用户打开媒体 App/网页
        │
        ▼
① 媒体端 SDK/Server → SSP Gateway
   ─ POST /ad/request
   ─ Body: { device_id, oaid, ip, ua, ad_slot_id, width, height, app_package }
   ─ 期望延迟：50ms 内响应
        │
        ▼
② SSP Gateway
   ─ 验证：ad_slot_id 是否存在、媒体是否激活
   ─ 补全：IP 解析地域、UA 解析设备类型、从 Redis 读取用户画像标签
   ─ 组装内部 BidRequest 结构体
   ─ 转发 ADX Engine (本地内存调用，<1ms)
        │
        ▼
③ ADX Engine
   ─ 查询广告位配置（底价、黑名单品类、白名单素材类型）
   ─ 调用 DSP Decision Engine
   ─ 设置超时控制（50ms alarm）
        │
        ▼
④ DSP Decision Engine
   ─ 策略匹配（按优先级遍历 6 条 RTB 策略，首条命中终止）
   ─ 人群引擎：Redis 检查用户所属人群包（O(1) SISMEMBER）
   ─ 频控引擎：Redis 检查当日该用户在各策略下的曝光/点击次数
   ─ 预算扣减：Redis DECRBY 原子操作扣除当日预算
   ─ 出价计算：策略配置目标 CPA × 出价系数
   ─ 素材匹配：广告位尺寸匹配最佳素材
   ─ 返回 BidResponse { price, ad_material_url, landing_url, tracking_urls }
        │
        ▼
⑤ ADX Engine
   ─ 检查出价 ≥ 广告位底价（否则丢弃，返回 no-bid）
   ─ 记录竞价日志（内存队列 → 批量刷入 MySQL）
   ─ 返回 SSP Gateway
        │
        ▼
⑥ SSP Gateway → 媒体
   ─ 格式化响应：{ code: 0, ad_type, html_snippet, track_imp, track_click, landing_url }
   ─ 注入监播像素：
      曝光: <img src="https://track.adx.com/imp/{cid}/{sid}/{uid}" />
      点击: <a href="https://track.adx.com/click/{cid}/{sid}/{uid}" target="落地页">
   ─ 媒体渲染广告
        │
        ▼
⑦ 用户交互 → Tracking
   ─ 曝光: 图片请求到 /track/imp → 记录 + 302 到真实落地页
   ─ 点击: 用户点击 → /track/click → 记录 + 302 跳转到广告主落地页
   ─ 转化: 广告主服务器回传 /track/conv + 监测 SDK 回传
        │
        ▼
⑧ 数据聚合（异步，非 RTB 路径）
   ─ Tracking 日志写入 Nginx access log
   ─ 异步采集到消息队列 / 直接落库
   ─ 每小时聚合写入 ad_stats_hourly
   ─ Management 每天凌晨跑结算/报表
```

### 3.2 关键时序约束

| 环节 | 预算时间 |
|------|---------|
| 媒体端到 SSP | 50ms |
| SSP → ADX → DSP（内部循环） | 50ms |
| DSP 决策（策略匹配+人群+频控+出价） | 50ms |
| ADX 返回 SSP | 20ms |
| SSP 格式化响应 | 30ms |
| **端到端总上限** | **≤200ms** |

---

## 4. 投放策略体系

### 4.1 策略分层

```
经营层策略 (Business Strategy)
  ─ 运营人员在 Management 后台配置
  ─ 含目标CPA、预算分配、人群定向、时段控制、渠道偏好
  ─ 与现有 S1-S7 策略管理功能复用
        │
        ▼ 策略关联到 Campaign / 同步到 Redis
        │
执行层规则 (Execution Rules)
  ─ DSP Decision Engine 在 RTB 中实时执行
  ─ 策略匹配 → 人群检查 → 频控检查 → 预算扣减 → 出价 → 素材匹配
  ─ 所有规则缓存在 Redis + Vert.x 本地内存
```

### 4.2 7 条可执行投放策略（6 条 RTB + 1 条搜索 API）

> **说明**：策略 7（品牌搜索防守）不经过 SSP/RTB 链路，由 Management Service 直接对接搜索引擎 API 进行关键词出价管理。其余 6 条策略在 DSP Decision Engine 中实时执行。

#### RTB 链路策略

| # | 策略名 | 目标 | 流量筛选条件 | 出价公式 | 素材策略 | 优先级 |
|---|--------|------|-------------|---------|---------|-------|
| 1 | 高价值人群精准转化 | CPA≤250，转化优先 | 用户标签命中 Lookalike/成分党/已购相似，时段 9:00-23:00 | maxBid = CPA × 0.4（保转化，不盲追高价） | C007 > C002 > 静态图 | 3 |
| 2 | 新品破圈拉新 | 曝光≥10万/天，CPA≤300 | 非已有用户 + 22-35岁女性 + 美妆兴趣标签 | maxBid = CPA × 0.25（低价拿量） | C001 > C011 | 4 |
| 3 | 竞品截流抢夺 | CPC≤2.0, CPA≤250 | 设备行为标签命中"竞品搜索/竞品App访问" | maxBid = CPA × 0.5（强意图溢价） | C008 > C002 | 2 |
| 4 | 弃单重定向强转化 | CVR≥3%, CPA≤200 | device_id/oaid 命中弃单人群列表(24h/72h) | maxBid = CPA × 0.6（最高出价，转化确定性最强） | C006 > 折扣素材 | 1 |
| 5 | 智能通投探索 | CPA≤350, 跑量占20% | 以上 4 条均未命中 + 当日总预算有余量 | maxBid = CPA × 0.15（探索出价） | 多素材 A/B 轮换 | 5 |
| 6 | 兜底不出价 | 止损 | 以上均未命中 或 预算已用完 | 返回 nbr=2（不出价） | — | 6 |

#### 搜索 API 策略

| # | 策略名 | 目标 | 管理方式 | 出价逻辑 | 预算 |
|---|--------|------|---------|---------|------|
| 7 | 品牌搜索防守 | CPA≤150, 覆盖品牌词搜索流量 | Management Service 直接调用百度搜索/巨量搜索 API 管理品牌词出价 | 固定 CPC ≤ 1.5，保证品牌词始终排名前 3 | 16 万（占总预算 20%） |

### 4.3 策略匹配执行顺序

```
收到 BidRequest → 按优先级升序检查各 RTB 策略条件：
  ─ 策略 4 (弃单重定向)  → 命中 → 出价 (优先级最高，转化确定性最强)
  ─ 策略 3 (竞品截流)    → 命中 → 出价
  ─ 策略 1 (高价值人群)  → 命中 → 出价
  ─ 策略 2 (新品破圈)    → 命中 → 出价 (低价拿量)
  ─ 策略 5 (智能通投)    → 命中且预算有余量 → 出价 (探索新流量)
  ─ 策略 6 (兜底)        → 不出价

搜索策略 7 (品牌搜索防守) 独立运行，由 Management Service 定时任务
通过搜索引擎 API 管理关键词出价，不与 RTB 链路冲突。
```

**优先规则**：每个 RTB 请求最多命中一条策略，不重复出价。高确定性策略（弃单重定向）优先消耗预算，保证核心 KPI；通投策略只消耗剩余预算用于探索新流量。搜索策略独立并行运行。

---

## 5. Bidding Service 详细设计

### 5.1 模块清单

```
Bidding Service (Vert.x 4.x)
├── SSP Gateway
│   ├── POST /ad/request           # 媒体广告请求入口
│   ├── 请求验证 (Token/广告位/媒体状态)
│   ├── 用户信息补全 (IP→地域, UA→设备, Redis→画像)
│   └── OpenRTB 内部结构体组装
│
├── ADX Engine
│   ├── 广告位配置查询
│   ├── 拍卖裁决 (两方市场，自有 DSP 独家竞价，出价 ≥ 底价即胜出)
│   ├── 底价检查
│   └── 竞价日志记录
│
├── DSP Decision Engine
│   ├── StrategyMatcher      策略匹配器
│   ├── AudienceEngine       人群引擎 (Redis SISMEMBER)
│   ├── FrequencyEngine      频控引擎 (Redis 计数器 + TTL)
│   ├── BudgetEngine         预算扣减 (Redis DECRBY)
│   ├── Pricer               出价计算
│   └── MaterialMatcher      素材匹配
│
├── Tracking Server
│   ├── GET /track/imp/{cid}/{sid}/{uid}     曝光
│   ├── GET /track/click/{cid}/{sid}/{uid}   点击
│   ├── POST /track/conv                     转化（广告主回传）
│   ├── GET /track/landing/{cid}/{sid}/{uid} 落地页跳转
│   └→ 3xx 重定向到广告主真实落地页
│
└── Data Sync
    ├── 策略缓存加载 (1min / Redis Pub/Sub 触发)
    ├── 人群包同步到 Redis
    ├── 预算/频控状态持久化
    └── 小时级统计写入 ad_stats_hourly
```

### 5.2 Data Sync 机制

Management 和 Bidding 通过 Redis 事件驱动同步，不引入 MQ：

```
Management 侧操作：
  运营修改策略/人群 → MySQL 持久化 → Redis 写入/更新 → PUBLISH config:changed

Bidding 侧：
  SUBSCRIBE config:changed → 遍历消息 → 重新加载对应缓存 → 确认完成
```

| 数据 | 同步方式 | 加载时机 | 失效策略 |
|------|---------|---------|---------|
| 策略配置 | Redis Hash + Pub/Sub | 启动加载 + 变更推送 | 全量加载间隔 60s 兜底 |
| 人群包 | Redis Set (SISMEMBER) | 启动加载 + 变更推送 | 单个人群 Key 独立 TTL 1h |
| 预算余额 | Redis String (DECRBY) | 每次出价实时扣减 | 每天凌晨从 MySQL 重置 |
| 频控数据 | Redis String + TTL | 每次曝光/点击实时计数 | TTL = 当天剩余秒数 |
| 广告位配置 | Redis String | 启动加载 + 变更推送 | TTL 5min + 按需刷新 |

### 5.3 预算熔断三级保护

| 级别 | 条件 | 动作 |
|------|------|------|
| 一级 | 日消耗 > 日预算 × 80% | Bidding 降低出价系数 ×0.8 |
| 二级 | 日消耗 > 日预算 × 100% | 暂停策略 2/5（探索类），仅保留策略 1/3/4（转化类） |
| 三级 | 日消耗 > 日预算 × 120% | 全部暂停，返回不出价，发送告警 |

---

## 6. Management Service 新增模块

### 6.1 媒体 / 广告位管理

现有 Spring Boot 项目中新增模块：

| 接口 | 说明 |
|------|------|
| GET /api/v1/publishers | 媒体列表 |
| POST /api/v1/publishers | 创建媒体（含 Token 生成） |
| PUT /api/v1/publishers/{id} | 更新媒体信息 |
| GET /api/v1/ad-slots | 广告位列表 |
| POST /api/v1/ad-slots | 创建广告位（设定底价、尺寸、品类限制） |
| PUT /api/v1/ad-slots/{id} | 更新广告位配置 |

### 6.2 搜索品牌词管理

针对策略 7（品牌搜索防守），新增搜索引擎 API 对接模块：

| 接口 | 说明 |
|------|------|
| GET /api/v1/search/brand-keywords | 品牌词列表及当前出价 |
| POST /api/v1/search/brand-keywords | 添加品牌词 |
| PUT /api/v1/search/brand-keywords/{id} | 更新品牌词出价/状态 |
| POST /api/v1/search/sync | 手动触发同步到搜索引擎平台 |

搜索引擎管理为独立模块，不与 RTB 链路共享数据通道。Management Service 通过定时任务同步品牌词出价到百度搜索/巨量搜索平台 API。

### 6.3 ADX 配置管理

| 接口 | 说明 |
|------|------|
| GET /api/v1/adx/config | ADX 全局配置（超时、底价全局浮动等） |
| GET /api/v1/adx/rules | ADX 流量过滤规则（黑名单 App/品类） |

### 6.4 结算 / 对账系统

| 接口 | 说明 |
|------|------|
| GET /api/v1/settlement/daily | 日报表（广告主侧 + 媒体侧） |
| GET /api/v1/settlement/strategy/{id} | 策略级报表 |

结算模型：
- **广告主支付 = wins × bid_price**（DSP 出价成交）
- **媒体收入 = wins × publisher_revenue**（ADX 与媒体约定的分成比例）
- **平台收入 = 广告主支付 - 媒体收入**

---

## 7. 数据库扩展

### 7.1 新增表

#### ad_publisher（媒体方）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| name | varchar(100) | 媒体名称 |
| code | varchar(20) UNIQUE | 媒体编码 |
| contact | varchar(50) | 联系人 |
| api_token | varchar(64) | 接入 Token（SSP 验证用） |
| revenue_share | decimal(5,2) | 媒体分成比例（如 0.70） |
| status | tinyint | 0-禁用 1-激活 |
| created_at | datetime | |

#### ad_ad_slot（广告位）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| publisher_id | bigint FK | 所属媒体 |
| name | varchar(100) | 广告位名称 |
| code | varchar(20) UNIQUE | 广告位编码 |
| slot_type | tinyint | 1-Banner 2-插屏 3-激励视频 4-原生 |
| width | int | 广告位宽度 |
| height | int | 广告位高度 |
| floor_price | decimal(10,2) | 底价（CPM） |
| block_category | varchar(500) | 屏蔽品类 ID 列表（JSON 数组） |
| status | tinyint | 0-禁用 1-激活 |

#### ad_bid_log（竞价日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| ad_slot_id | bigint | 广告位 ID |
| campaign_id | bigint | 命中的计划 ID |
| strategy_id | bigint | 命中的策略 ID |
| device_id | varchar(64) | 设备 ID（脱敏） |
| bid_price | decimal(10,2) | 出价（CPM） |
| floor_price | decimal(10,2) | 底价 |
| win | tinyint | 是否胜出 |
| nbr | tinyint | 不出价原因码 |
| bid_at | datetime | 竞价时间 |
| latency_ms | int | 竞价耗时 |

#### ad_tracking_log（监播日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| campaign_id | bigint | 计划 ID |
| strategy_id | bigint | 策略 ID |
| device_id | varchar(64) | 设备 ID（脱敏） |
| track_type | tinyint | 1-曝光 2-点击 3-转化 |
| track_at | datetime | 时间 |

---

## 8. 数据看板新增

基于现有 ECharts + Element Plus 看板增加：

| 看板模块 | 指标 | 数据源 |
|---------|------|--------|
| **实时竞价监控** | QPS 实时曲线、响应时间 P50/P95/P99、胜出率 | Redis 实时计数 + WebSocket 推送到前端 |
| **媒体侧报表** | 各媒体填充率、CPM、收入 | ad_stats_hourly 聚合 |
| **ADX 状态** | 广告位请求量、底价命中率、No-bid 原因分布 | ad_bid_log 聚合 |
| **结算对账** | 广告主成本 vs 媒体收入 vs 平台收入 | ad_stats_hourly + ad_bid_log 对账 |
| **频控效率** | 各策略频控拦截率、频控对 CPA 的影响分析 | ad_tracking_log 聚合 |

---

## 9. 阶段规划

### 阶段 1：核心 RTB 链路打通（3-4 周）

**目标**：SSP → ADX → DSP → 出价 → 曝光/点击回传，全链路跑通

| 任务 | 说明 |
|------|------|
| 新建 Bidding Service (Vert.x) 项目骨架 | Maven 多模块 |
| SSP Gateway | 媒体请求接入 + 验证 + 用户补全 |
| ADX Engine | 广告位配置查询 + 底价检查 + 竞价日志 |
| DSP Decision Engine | 策略匹配 + 人群引擎 + 频控 + 出价 + 预算扣减 |
| Tracking Server | 曝光/点击监播、落地页跳转 |
| Management 新增媒体/广告位管理 | 媒体 CRUD + 广告位 CRUD + Token 生成 |
| Management 新增策略运营配置 | 策略上线到 RTB 产线 |
| 一次端到端联调测试 | Postman 模拟 → SSP → ADX → DSP → Tracking 全链路 |
| 阶段交付 | 全链路 Demo，单机 200 QPS 基准测试 |

### 阶段 2：引擎加固与数据闭环（2-3 周）

**目标**：系统稳定、数据完整、可运维

| 任务 | 说明 |
|------|------|
| Data Sync Module | 策略/人群/预算的 Redis 与 MySQL 双向同步 |
| 预算熔断三级保护 | 实时熔断逻辑 |
| 结算/对账系统 | 日报表、策略报表 |
| Data Sync Module 日志采集 | Tracking 日志 → 聚合写入 ad_stats_hourly |
| 实时看板 | QPS/响应时间/胜出率 WebSocket 推送 |
| 错误处理 | 超时熔断、降级、fallback |
| Nginx 配置 | RTB 路由、限流、access log 配置 |
| 阶段交付 | 稳定运行、看板可观测、无数据丢失 |

### 阶段 3：运营优化与扩展（2-3 周）

**目标**：性能调优、运营效率和系统完善

| 任务 | 说明 |
|------|------|
| 智能通投探索策略算法优化 | 基于胜出率动态调价 |
| 频控策略优化 | 跨策略频控合并、频控阈值自适应 |
| 素材衰减自动轮换 | 基于 CTR 趋势自动切换素材 |
| 1000 QPS 压力测试 | 确立性能基线，优化瓶颈 |
| 异常告警 | 策略 CPA 偏离告警 + 系统异常告警 |
| 运营文档 | 媒体接入文档、策略配置指南 |
| 阶段交付 | 可直接上线运营 |

---

## 10. 运营策略配置说明

### 10.1 如何配置一条策略上线到 RTB

运营人员在 Management 后台操作：

1. **策略管理 → 新建策略**（复用现有界面）
   - 填入目标 CPA、预算、描述等
   - 策略状态设为"启用"

2. **策略详情 → 投放规则**
   - 选择投放渠道（初期建议每个策略只选 1 个渠道，便于效果评估和归因）
   - 选择人群包（如 Lookalike_老客_1%）
   - 选择素材（优先度排序）
   - 出价系数：0.4（保转化）、0.25（低价拿量）等

3. **策略详情 → 频控设置**
   - 单用户日曝光上限：10 次/策略
   - 单用户日点击上限：3 次/策略

4. **确认上线**
   - 系统验证：人群包非空、素材尺寸匹配、预算已分配
   - 验证不通过：前端逐项提示失败原因（如"素材 C002 尺寸 1080×1920 与广告位 320×480 不匹配"），不允许上线
   - 验证通过后，策略数据通过 Redis Pub/Sub 同步到 Bidding Service
   - Bidding 在下一个请求周期自动应用

5. **查看效果**
   - 策略详情页的"实时数据"标签下可观察：
   - 当前填充率、胜出率、实时 CPA、频控拦截率

---

## 11. 风险与应对

| 风险 | 概率 | 影响 | 应对 |
|------|------|------|------|
| 2 个服务 + Redis + MySQL 运维压力大 | 高 | 高 | 共享库减少中间件，Docker Compose 本地化，初期不引入 K8s |
| Vert.x 学习曲线 | 中 | 中 | 核心逻辑控制在 500 行以内，Vert.x 只做 HTTP + Redis 操作 |
| 预算扣减的并发一致性 | 中 | 高 | Redis DECRBY 原子操作 + 熔断兜底，不追求 exactly-once |
| 媒体方对接进度不确定 | 中 | 中 | 自建模拟媒体工具端到端验证，不依赖真实媒体即可开发 |
| 5 人同时维护两个代码库 | 低 | 中 | 同一 Git 仓库，Maven 多模块分 module |
