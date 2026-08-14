# LUMI 程序化广告智能投放中台 — 产品设计文档

> 版本：V1.0
> 日期：2026-07-17
> 项目预算：80万元 | 目标：拉新首购转化
> 技术栈：Spring Boot + Vue 3 + MySQL + Redis

---

## 1. 项目概述

### 1.1 背景

LUMI 是一个新锐 DTC 美妆品牌，2026 年 7 月主推 5% 烟酰胺精华液，预算 80 万元。本次项目旨在构建一套 AI 驱动的程序化广告智能投放中台，将人工投放经验转化为可复用的系统能力，实现投放策略的标准化执行与预算的智能化调配。

### 1.2 核心价值

- **效率提升**：人工优化周期从天级缩短至小时级
- **效果保障**：基于历史数据建模的策略体系，首购 CPA 控制在目标范围内
- **可复制性**：标准化的策略模板可快速复用到其他 SKU
- **透明可控**：全链路数据可视化，预算去向可追溯

### 1.3 北极星指标

**首购新客成本（CPA）** 为本项目北极星指标。所有策略设计、系统优化围绕「在保证新客规模的前提下，持续降低首购 CPA」展开。

---

## 2. 系统架构

### 2.1 整体架构

前端（Vue 3）和后端（Spring Boot）通过 RESTful API 通信，Nginx 反向代理统一入口。

```
                    ┌──────────┐
                    │  Nginx   │ 端口 80 → /api/* → backend:8080
                    └────┬─────┤           /*     → frontend:5173 (dev) / dist (prod)
                   ┌─────┴──────┐
                   │             │
            ┌──────▼────┐ ┌─────▼──────┐
            │  Vue 3    │ │ Spring Boot│
            │  SPA      │ │ API 服务   │
            │  Element+ │ │ 端口 8080  │
            │  ECharts  │ └─────┬──────┘
            └──────┬────┘       │
                   │             │
                   └──────┬──────┘
                     ┌────▼─────┐
                     │  MySQL   │
                     │  Redis   │
                     └──────────┘
```

### 2.2 项目目录结构

```
ad-platform/
├── frontend/                    # Vue 3 前端
│   ├── src/
│   │   ├── api/                # API 封装
│   │   ├── views/
│   │   │   ├── dashboard/      # 总览看板
│   │   │   ├── strategy/       # 策略管理
│   │   │   ├── campaign/       # 计划管理
│   │   │   ├── material/       # 素材管理
│   │   │   ├── audience/       # 人群管理
│   │   │   └── rule/           # 规则引擎
│   │   ├── components/         # 通用组件
│   │   ├── stores/             # Pinia 状态管理
│   │   └── router/             # 路由
│   └── package.json
│
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/ad/
│       ├── controller/         # 控制器
│       ├── service/            # 业务逻辑
│       ├── mapper/             # MyBatis-Plus
│       ├── entity/             # 数据实体
│       ├── dto/                # 传输对象
│       ├── enums/              # 枚举
│       ├── config/             # 配置
│       └── task/               # 定时任务
│
└── docs/
    └── api/                    # API 文档
```

### 2.3 后端分层职责

| 分层 | 职责 | 关键约束 |
|------|------|----------|
| Controller | 参数校验、调用 Service、返回 DTO | 不直接操作 Entity |
| Service | 业务编排、事务、缓存 | 跨 Repository 协调 |
| Mapper (MyBatis-Plus) | 单表 CRUD + 复杂查询 | 动态 SQL 聚合 |
| Entity | 表映射 | 与数据库一一对应 |
| DTO | 接口契约 | 不包含敏感/内部字段 |

### 2.4 前端路由结构

```
/ad-platform
├── /dashboard                     # 总览看板（首页）
│   └── 核心指标卡 + 趋势图 + 渠道分布 + 素材TOP
├── /strategy                      # 策略管理
│   ├── /strategy/list             # 策略卡片列表
│   └── /strategy/:id              # 策略详情（含下钻计划列表）
├── /campaign                      # 广告计划
│   ├── /campaign/list             # 计划列表（筛选/搜索/批量启停）
│   └── /campaign/:id              # 计划详情+实时数据
├── /audience                      # 人群管理
│   └── 人群包列表 + 规模/效果数据
├── /material                      # 素材管理
│   ├── 素材库列表 + 效果排行榜
│   └── CTR/CPA 衰减曲线图
├── /rule-engine                   # 自动化规则引擎
│   ├── 规则列表（启用/禁用切换）
│   └── 规则创建/编辑（表单式配置）
└── /settings                      # 系统设置
    └── 预算配置、渠道账号等
```

> 前端技术栈优先级：Vue 3 (Composition API + `<script setup>`) + Element Plus + ECharts + Pinia。路由使用 History 模式，Nginx 配置 fallback 到 index.html。

---

## 3. 数据库设计

### 3.1 核心表清单

#### 3.1.1 BaseEntity（所有实体继承）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增主键 |
| version | int DEFAULT 0 | 乐观锁 |
| deleted | tinyint DEFAULT 0 | 软删除标记 |
| created_by | varchar(64) | 创建人 |
| updated_by | varchar(64) | 最后修改人 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

#### 3.1.2 策略域

**ad_strategy**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | varchar(50) NOT NULL | 策略名称 |
| code | varchar(20) NOT NULL UNIQUE | 编码 S1-S7 |
| status | tinyint NOT NULL DEFAULT 0 | 0-草稿 1-启用 2-暂停 3-结束 |
| objective | varchar(20) NOT NULL | CONVERT / BRAND / RETARGET |
| description | varchar(500) | 策略描述 |
| budget | decimal(12,2) DEFAULT 0 | 分配预算（元） |
| budget_ratio | decimal(5,2) DEFAULT 0 | 预算占比（%） |
| target_cpa | decimal(10,2) | 目标 CPA |
| target_cvr | decimal(5,4) | 目标 CVR |
| expected_roas | decimal(5,2) | 预期 ROAS |
| sort_order | int DEFAULT 0 | 排序 |

**ad_strategy_channel**

| 字段 | 类型 | 说明 |
|------|------|------|
| strategy_id | bigint FK | 策略 ID |
| channel | varchar(20) NOT NULL | 渠道编码 |
| budget_ratio | decimal(5,2) | 该渠道预算占比 |
| PRIMARY KEY (strategy_id, channel) | | 联合主键 |

**ad_audience**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | varchar(100) NOT NULL | 人群包名称 |
| code | varchar(20) NOT NULL UNIQUE | 人群编码 |
| source | varchar(20) NOT NULL | DMP / LOOKALIKE / RETARGET |
| size_estimate | int DEFAULT 0 | 预估人群规模 |
| status | tinyint DEFAULT 0 | 0-可用 1-暂停 |

**ad_material**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | varchar(100) NOT NULL | 素材名称 |
| code | varchar(20) NOT NULL UNIQUE | 素材编码 |
| type | tinyint NOT NULL | 1-视频 2-图片 3-图文 |
| duration | int DEFAULT 0 | 视频时长(秒) |
| status | tinyint DEFAULT 0 | 0-待审核 1-可用 2-衰减 3-停用 |
| score | decimal(5,2) DEFAULT 0 | 综合评分 |

**ad_strategy_audience** — 策略与人群多对多

| 字段 | 类型 | 说明 |
|------|------|------|
| strategy_id | bigint FK | |
| audience_id | bigint FK | |
| type | tinyint DEFAULT 0 | 0-主人群 1-扩展 2-排除 |
| PRIMARY KEY (strategy_id, audience_id) | | |

**ad_strategy_material** — 策略与素材多对多

| 字段 | 类型 | 说明 |
|------|------|------|
| strategy_id | bigint FK | |
| material_id | bigint FK | |
| sort_order | int DEFAULT 0 | 投放优先级 |
| PRIMARY KEY (strategy_id, material_id) | | |

#### 3.1.3 投放执行域

**ad_campaign**

| 字段 | 类型 | 说明 |
|------|------|------|
| strategy_id | bigint FK | 归属策略 |
| name | varchar(100) NOT NULL | 计划名称 |
| channel | varchar(20) NOT NULL | 投放渠道 |
| platform_campaign_id | varchar(64) | 平台侧计划 ID |
| budget_daily | decimal(12,2) DEFAULT 0 | 日预算 |
| bid_type | varchar(10) | OCPM / CPC / CPM |
| bid_price | decimal(10,2) | 出价 |
| status | tinyint DEFAULT 0 | 0-搭建中 1-投放中 2-暂停 3-关停 |
| launch_at | datetime | 开始投放时间 |
| stop_at | datetime | 停止时间 |

> 索引：`idx_strategy (strategy_id)`，`idx_channel_status (channel, status)`

#### 3.1.4 规则引擎域

**ad_rule**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | varchar(100) NOT NULL | 规则名称 |
| trigger_metric | varchar(30) NOT NULL | 触发指标 CPA/CTR/CVR/CONSUME |
| trigger_operator | varchar(10) NOT NULL | GT/LT/GTE/LTE |
| trigger_threshold | decimal(12,2) NOT NULL | 触发阈值 |
| trigger_window_hours | int DEFAULT 1 | 观察窗口(小时) |
| action_type | varchar(30) NOT NULL | PAUSE / RAISE_BID / LOWER_BID / SWAP_MATERIAL |
| action_params | json | 动作参数字典 |
| scope_type | varchar(20) | STRATEGY / CHANNEL / CAMPAIGN |
| scope_value | varchar(100) | 作用域值 |
| priority | int DEFAULT 0 | 优先级（高优先执行） |
| cooldown_minutes | int DEFAULT 60 | 冷却期（分钟内不重复触发） |
| status | tinyint DEFAULT 0 | 0-禁用 1-启用 |

**ad_rule_execution_log**

| 字段 | 类型 | 说明 |
|------|------|------|
| rule_id | bigint FK | 规则 ID |
| campaign_id | bigint FK | 触发计划 ID |
| trigger_value | decimal(12,2) | 触发实时值 |
| action_taken | varchar(100) | 动作描述 |
| result | tinyint | 0-失败 1-成功 |
| error_message | varchar(500) | 失败原因 |
| executed_at | datetime | 执行时间 |

> 索引：`idx_rule_executed (executed_at)`，`idx_rule_campaign (rule_id, campaign_id)`

#### 3.1.5 数据看板域

**ad_stats_hourly**（按月分区）

| 字段 | 类型 | 说明 |
|------|------|------|
| channel | varchar(20) NOT NULL | 渠道 |
| strategy_id | bigint | 策略（可空） |
| campaign_id | bigint | 计划（可空） |
| stat_date | date NOT NULL | 日期 |
| stat_hour | tinyint NOT NULL | 小时 |
| impressions | bigint DEFAULT 0 | 曝光 |
| clicks | int DEFAULT 0 | 点击 |
| micro_conversions | int DEFAULT 0 | **微转化数**（加购/收藏等中间行为，部分渠道有#N/A数据质量缺陷，需ETL清洗） |
| conversions | int DEFAULT 0 | 转化（首购订单数） |
| cost | decimal(12,2) DEFAULT 0 | 消耗 |
| gmv | decimal(14,2) DEFAULT 0 | 收入 |
| new_users | int DEFAULT 0 | 新客数 |

> **数据质量说明**：实际历史数据中微转化数存在空白和 #N/A 值。ETL 阶段处理：空白 / #N/A 统一记为 0，日志记录异常行。看板对此字段显示「部分渠道数据不可用」提示。
> 复合索引：`(stat_date, channel, strategy_id)`，`(campaign_id, stat_date)`
> 分区：按月 `RANGE (YEAR(stat_date)*100+MONTH(stat_date))`

---

## 4. 策略种子数据

> **数据校准说明**：以下目标 CPA 基于 2024 年 4-6 月历史投放明细数据分渠道加权计算得出（总样本量：91 天 × 5 渠道 = 约 170 条计划级日数据）。实际投产中允许 ±20% 浮动，超出触发自动告警。

系统启动时预设以下 7 条策略作为初始数据，用户可在策略管理页面调整参数。

| 编码 | 策略名称 | 目标 | 主投渠道 | 主人群 | 主素材 | 预算(万) | 占比 | 目标CPA | 数据依据 |
|------|----------|------|----------|--------|--------|----------|------|---------|----------|
| S1 | 巨量引擎·重定向爆款 | CONVERT | 巨量引擎 | Lookalike_老客_1%(AUD005) | C007 KOC种草30s | 12 | 15% | ≤250 | `lumi-jh-laxin-3` 历史CPA均值266，优选高转化素材降本 |
| S2 | 小红书·成分党深耕 | BRAND | 小红书 | 成分党_烟酰胺(AUD004) | C002 成分图解析 | 10 | 12.5% | ≤250 | 成分党计划历史CPA 96-221间波动，C002实际CTR 1.8% |
| S3 | B站·新品破圈 | BRAND | Bilibili | 美妆兴趣_精华液(AUD001) | C001 KOL测评15s | 8 | 10% | ≤300 | B站历史CPA 199-284，CPM 19-41，曝光成本低但转化率低 |
| S4 | 腾讯·弃单重定向 | RETARGET | 腾讯广告 | 弃单人群_24h(AUD006) | C006 限时优惠海报 | 8 | 10% | ≤200 | 弃单人群规模2400人，CVR理应最高；AUD009(测试包)已排除 |
| S5 | 百度·竞品截流 | CONVERT | 百度信息流 | 竞品种草_HBN(AUD003) | C008 成分对比测评 | 12 | 15% | ≤250 | `LUMI_精华_拉新_v2` CPA 116-365波动大，设熔断保护 |
| S6 | 品牌搜索防御 | CONVERT | 百度搜索+巨量搜索 | 品牌词搜索人群 | C011 明星测评精剪 | 16 | 20% | ≤150 | 归因数据：品牌搜索覆盖~28%订单(首触+末触)，意图最强CPA应最低 |
| S7 | AI·智能优选通投 | CONVERT | 全渠道通投 | 系统智能推荐 | C007 KOC种草30s | 14 | 17.5% | ≤280 | 无历史数据验证，保守设预算上限，跑量验证后调优 |

**关键修正项说明**：

1. **S1 目标CPA 150→250**：数据回测 `lumi-jh-laxin-3` 计划6次投放场均CPA≈266（最高338，最低157），目标设为250并附熔断
2. **S2 C002 CTR 从>3.0%修正为1.8%**：素材表C002实际CTR为1.8%，素材权重调低，改用C007作为备选
3. **S4 人群AUD009→AUD006**：AUD009名为"测试包_勿删"（来源未知，仅用1次），替换为AUD006弃单人群_24小时（2400人定期更新）
4. **S6 从AI策略改为品牌搜索防御**：归因数据显示品牌词搜索贡献~28%首购订单，且CPA显著低于其他渠道
5. **S7 AI策略预算从30%压缩至17.5%**：无历史数据支撑，保守设置

> 系统初始化时通过 Liquibase/Flyway 或 SQL 脚本导入上述数据到 `ad_strategy`、`ad_audience`、`ad_material` 及相关关联表。

---

## 5. 核心 API 设计

### 5.1 通用约定

- 统一前缀：`/api/v1`
- 分页：`?page=1&size=20`，返回 `{ list, total, page, size }`
- 日期：ISO 8601 (`yyyy-MM-dd`)
- 响应格式：`{ code: 0, data: {...}, message: "ok" }`

### 5.2 策略管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /strategies | 策略列表（含效果摘要） |
| GET | /strategies/{id} | 策略详情 |
| POST | /strategies | 创建策略 |
| PUT | /strategies/{id} | 更新策略 |
| PATCH | /strategies/{id}/status | 启用/暂停/结束 |

### 5.3 人群管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /audiences | 人群包列表 |
| POST | /audiences | 创建人群包 |
| GET | /audiences/{id}/stats | 人群效果数据 |

### 5.4 素材管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /materials | 素材库列表 |
| POST | /materials | 创建素材 |
| GET | /materials/{id}/decay | 素材衰减曲线 |

### 5.5 投放计划

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /campaigns | 计划列表（策略/渠道筛选） |
| GET | /campaigns/{id} | 计划详情+实时数据 |
| POST | /campaigns | 创建计划 |
| PATCH | /campaigns/{id}/status | 启停单个计划 |
| PATCH | /campaigns/batch-status | **批量启停计划** |

### 5.6 数据看板

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /dashboard/overview?start_date=&end_date= | 总览核心指标 |
| GET | /dashboard/trends?start_date=&end_date= | 日级趋势 |
| GET | /dashboard/channel-dist?start_date=&end_date= | 渠道分布 |
| GET | /dashboard/material-top?start_date=&end_date= | 素材 TOP |

### 5.7 规则引擎

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /rules | 规则列表 |
| POST | /rules | 创建规则 |
| PUT | /rules/{id} | 编辑规则 |
| PATCH | /rules/{id}/status | 启用/禁用 |
| GET | /rules/{id}/logs | 执行日志 |
| POST | /rules/{id}/test | **沙箱模拟测试** |

---

## 6. 策略系统 Loop 设计

### 6.1 Loop 定义

```
目标 (Objective)    综合CPA ≤ 250元，新客 ≥ 4,000人，ROAS ≥ 1.5
                    品牌搜索CPA ≤ 150元（防守型目标）
   ↓
输入 (Input)        预算配置、策略参数、人群包、素材库、渠道API实时数据、
                    品牌搜索词排名、归因数据（首触+末触）
   ↓
处理 (Process)      规则引擎评估 → 冲突仲裁 → 动作执行 → 效果记录
   ↓
输出 (Output)       计划启停、出价调整、素材轮换、预算调配、品牌词出价保护
   ↓
反馈 (Feedback)     ad_stats_hourly 聚合 → CPA/CTR/CVR/ROAS/微转化率
                    归因数据（分渠道首触/末触贡献占比）
   ↓
测试 (Test)         沙箱回放：上线前用历史数据验证规则有效性
   ↓
迭代 (Iterate)      每周复盘：TOP规则保留，低效规则下线或调参
   ↓
停止 (Stop)         连续3天CPA > 目标×200% → 熔断，全切人工
```

### 6.2 各环节详细定义

| 元素 | 定义 | 说明 |
|------|------|------|
| 目标 | CPA ≤ 250 元，新客 ≥ 4,000 人，ROAS ≥ 1.5，品牌搜索 CPA ≤ 150 | 系统所有优化动作以北极星指标为收敛方向。归因数据显示品牌搜索覆盖~28%订单，设为独立防守目标 |
| 输入 | 预算、策略配置、人群+素材关联、渠道 API 回传的曝光/点击/转化、品牌词搜索排名、归因首触/末触数据 | 分为静态参数（策略定义）和动态数据（实时流+归因表） |
| 处理 | 规则引擎每周期扫描，匹配触发条件 → 优先级仲裁 → 执行动作 | 高优先规则覆盖低优先，互不冲突可并行 |
| 输出 | PAUSE / RAISE_BID / LOWER_BID / SWAP_MATERIAL / ADJUST_BUDGET | 输出自动写入 ad_rule_execution_log 留痕 |
| 反馈 | ad_stats_hourly 按小时聚合 → 后端计算 CPA/CTR/CVR/ROAS/微转化率 → 缓存到 Redis | 反馈数据是下一轮规则评估的输入。微转化率存在部分渠道数据质量缺陷，看板提示「部分数据不可用」 |
| 测试 | 沙箱模式：用户配置规则后选择 N 天历史数据回放，输出触发统计 | 测试通过才允许激活，防止误判上线 |
| 迭代 | 每周一自动生成规则效果报告：命中次数、准确率、误伤率 | 准确率 < 60% 的规则自动标记为待优化 |
| 停止 | 预算熔断三级保护 + 人工一键切换为纯监控模式 | 系统异常不扩散，兜底人工 |

### 6.3 Loop 周期

```
采集 ──[每小时]──→ 评估 ──[触发?]──→ 执行 ──[写入]──→ 日志
  ↑                                                    │
  └────────────────── 反馈闭环 ──────────────────────────┘

每日凌晨：渠道间预算再分配（基于前7天ROAS）
每周一  ：规则效果复盘 + 人群包效果刷新 + 素材优先级重排
```

---

## 7. MVP 功能清单

### P0（必须）

| 模块 | 功能 | 说明 |
|------|------|------|
| 策略管理 | 7 大策略 CRUD | 含预算分配、人群/素材关联 |
| 策略管理 | 状态流转 | 草稿→启用→暂停→结束 |
| 数据看板 | 核心指标卡 | 消耗/新客/CPA/ROAS + 预算进度 |
| 数据看板 | 日级趋势图 | 双 Y 轴：消耗柱图 + CPA 折线 |
| 数据看板 | 策略下钻 | 策略卡 → 详情页 → 计划列表 |

### P1（重要）

| 模块 | 功能 | 说明 |
|------|------|------|
| 数据看板 | 渠道分布 + 渠道对比表 | 饼图 + 表格 |
| 数据看板 | 素材排行榜 + 衰减曲线 | |
| 人群管理 | 人群包列表 | 规模/效果数据 |
| 计划管理 | 计划列表 + 单/批量启停 | 含筛选和搜索 |
| 规则引擎 | 规则配置（表单式） | 下拉选指标 → 填阈值 → 选动作 |

### P2（增强）

| 模块 | 功能 | 说明 |
|------|------|------|
| 规则引擎 | 自动启停执行 | 定时任务扫描 |
| 规则引擎 | **沙箱模拟测试** | 用历史数据验证规则 |
| 规则引擎 | 执行日志列表 | 可追溯、可过滤 |

---

## 8. 规则引擎设计

### 8.1 RuleActionType 枚举

```java
public enum RuleActionType {
    PAUSE_CAMPAIGN,      // 暂停计划
    ACTIVATE_CAMPAIGN,   // 启用计划
    RAISE_BID,           // 提价
    LOWER_BID,           // 降价
    SWAP_MATERIAL,       // 替换素材
    ADJUST_BUDGET,       // 调整预算
    SEND_ALERT           // 发送告警
}
```

### 8.2 冲突仲裁

多条规则同时命中时，按以下顺序裁决：

1. `priority` 高者优先执行
2. 同优先级按 `updated_at` 先后（后更新的优先）
3. 动作互不冲突的规则可并行执行
4. 冲突动作（如 RAISE_BID vs LOWER_BID），高优先覆盖，同优先均不执行并记录冲突日志

### 8.3 沙箱测试流程

```
用户配置规则 → 选择历史数据窗口（N天） → 系统回放数据
→ 标记哪些时间点会触发规则 → 输出「触发了 X 次，影响了 Y 个计划」
→ 展示模拟执行结果摘要 → 用户确认后激活
```

### 8.3.1 内置规则：test 计划自动检测

> **数据驱动**：2024 年 4-6 月历史数据中，`test_删我` 类计划横跨小红书、百度信息流、B站、腾讯广告 4 个渠道，累计消耗估算约 2 万元以上，首购订单总数仅个位数。以下规则自动拦截同类计划。

| 触发条件 | 执行动作 | 判定依据 |
|----------|----------|----------|
| 计划名包含 `test`/`测试`/`删我` 关键词 | 自动暂停并标记「疑似测试计划」 | 日消耗超 500 元且转化 = 0 |
| 连续 3 天 CPA > 目标值 × 300% 且转化数 < 5 | 自动暂停，发送告警 | 消耗 > 2000 元，严重偏离预期 |
| 计划名包含上述关键词但手动放行 | 降低优先级至最低，仅消耗监控 | 需运营在规则配置页主动确认 |

> 此规则不可删除、不可禁用（灰锁），仅可调整触发阈值。执行沙箱测试时可直观看到：回放历史数据，`test_删我` 计划会被提前暂停，节省预算。

### 8.4 定时执行策略

| 层级 | 频率 | 说明 |
|------|------|------|
| 消耗监控 | 每小时 | 异常消耗告警 |
| 出价调整 | 每日 2 次 | 午间 + 凌晨 |
| 计划启停 | 每日 1 次 | 凌晨执行，基于 3 天数据 |
| 预算调配 | 每日 1 次 | 基于 7 天 ROAS |
| 素材轮换 | 每周 2 次 | 基于衰减曲线 |
| 人群优化 | 每周 1 次 | 基于 14 天 CPA |

---

## 9. 数据看板设计

### 9.1 总览看板布局

```
┌──────────────────────────────────────────────────────┐
│  项目名称     时间筛选 [日/周/月]   预算进度条 80万   │
├──────────┬──────────┬──────────┬──────────────────────┤
│ 总消耗    │ 新客数    │ CPA      │ ROAS                 │
│ ¥320,000  │ 2,460     │ ¥130     │ 1.75                 │
│ ↓12%      │ ↑18%     │ ↓8%     │ ↑0.15                │
├──────────┴──────────┴──────────┴──────────────────────┤
│           日级趋势图（双Y轴）                          │
│   ┌──────────────────────────────────────────────┐   │
│   │  消耗柱图 + CPA折线                           │   │
│   └──────────────────────────────────────────────┘   │
├──────────────────────┬───────────────────────────────┤
│  渠道分布            │  素材效果 TOP5 (CTR排名)      │
│  ┌────────────┐      │  1. C007 KOC     CTR 3.5%   │
│  │  饼图       │      │  2. C011 明星    CTR 3.3%   │
│  └────────────┘      │  3. C004 新品    CTR 2.6%   │
│                      │  注：C007消耗2.2万成本最低    │
│                      │  C011消耗2.7万但CVR 0.72%    │
└──────────────────────┴───────────────────────────────┘
```

### 9.2 Redis 缓存策略

看板数据聚合逻辑：API 接受 `start_date` 和 `end_date` 范围参数，后端首先检查该日期范围内每一天是否有独立缓存 Key，若全部命中则直接聚合返回；若有缺失天则从 MySQL `ad_stats_hourly` 查询缺失数据并回填缓存。

| Key 模式 | Value | TTL | 说明 |
|----------|-------|-----|------|
| `dash:overview:{date}` | JSON | 5 min | 按天缓存核心指标，API 范围查询时逐天聚合 |
| `dash:trends:{start}:{end}` | JSON | 5 min | 趋势数据 |
| `dash:channel:{date}` | JSON | 5 min | 渠道分布 |
| `dash:material-top:{date}` | JSON | 5 min | 素材排行 |
| `rule:lock:{rule_id}` | 1 | 按 cooldown | 规则防重复执行 |

---

## 10. AI 增强设计

### 10.1 AI 集成点

| 场景 | AI 能力 | 输入 | 输出 | MVP 处理方式 |
|------|---------|------|------|-------------|
| 策略推荐 | 基于历史 CPA/ROAS 推荐预算分配 | 渠道+人群效果数据 | 策略参数建议 | 后端预留 AiAdviceService，返回规则加权结果 |
| 文案变体 | 高转化素材特征提炼文案方向 | 原素材 + CTR 排名 | 3-5 条文案变体 | 前端预留"AI 生成"按钮，MVP 返回模板文案 |
| 异常诊断 | 多维度下钻分析 CPA 飙升原因 | 异常时段的分渠道/人群数据 | 排查建议清单 | 后端提供结构化排查清单（非 LLM） |
| 日报生成 | 聚合数据输出自然语言报告 | 昨日投放汇总 | 业务分析报告 | MVP 用模板化报告，V2.0 升级 LLM |

### 10.2 AI 服务接口

```java
public interface AiAdviceService {
    // 基于历史 CPA 加权推荐预算分配
    BudgetAdvice recommendBudget(LocalDate date);

    // 诊断指定维度的异常，返回排查项列表
    DiagnosisResult diagnose(String dimension, LocalDate start, LocalDate end);

    // 生成运营日报文本
    String generateDailyReport(LocalDate date);
}

// MVP 实现：不依赖外部 LLM，基于规则和数据统计返回建议
// 确保系统可独立运行，AI 增强只是可插拔升级
```

### 10.3 效率提升路径

```
MVP（当前）    规则自动化替代人工盯盘 → 减少 60% 重复盯盘操作
   ↓
V1.5         智能出价 + 素材轮换自动化 → 减少 80% 人工调整
   ↓
V2.0         LLM 策略生成 + 异常自动诊断 → 减少 90% 分析工作量
```

---

## 11. 风险控制

### 11.1 预算熔断

| 级别 | 条件 | 动作 |
|------|------|------|
| 一级 | 日消耗 > 日预算 × 120% | 降速 + 告警 |
| 二级 | 日消耗 > 日预算 × 150% | 暂停非核心计划 + 人工介入 |
| 三级 | 连续 3 天 CPA > 目标 × 200% | 全部切换人工模式 |

### 11.2 规则安全

- 沙箱测试通过的规则才能激活
- 出价调整单次不超过 ±20%
- 预算调配单次不超过该渠道原预算 ±20%
- 规则误判人工审核通道

---

## 12. 迭代路线图

| 版本 | 周期 | 核心功能 | 目标 |
|------|------|----------|------|
| V1.0 | 第 1 个月 | 策略管理 + 数据看板 + 规则引擎 | 跑通闭环，自动覆盖 50% |
| V1.5 | 第 2-3 月 | 智能出价 + 素材自动轮换 + Lookalike 自动化 | 自动覆盖 80%，CPA 降 10% |
| V2.0 | 第 4-6 月 | 多触点归因 + AI 策略生成 + 跨渠道预算调度 | 完整智能投放中台 |
