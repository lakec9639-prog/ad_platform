# LUMI 程序化广告智能投放中台

> DSP-ADX-SSP 一体化的程序化广告平台：投放策略管理、实时竞价（RTB）、自动化规则引擎与效果分析。
> 为 DTC 美妆品牌 LUMI（主推 5% 烟酰胺精华液，投放预算 80 万）构建，北极星指标为**首购新客成本（CPA）**。

## 系统架构

双服务 + 前端架构：

```
┌──────────────┐     /api/v1     ┌──────────────────────┐
│  Vue 3 前端   │ ───────────────→│  Management Service   │
│  (Vite 5173) │                 │  Spring Boot 3.2.5    │
└──────────────┘                 │  :8080 (MySQL+Redis)  │
                                 └──────────────────────┘
┌──────────────┐   /ad/request   ┌──────────────────────┐
│  媒体/模拟器   │ ───────────────→│  Bidding Service      │
│              │                 │  Vert.x 4.5.10        │
└──────────────┘                 │  :9090 (SSP/ADX/DSP)  │
                                 └──────────────────────┘
```

| 服务 | 端口 | 技术栈 | 职责 |
|------|------|--------|------|
| Management Service | 8080 | Spring Boot 3.2.5 + MyBatis-Plus + MySQL + Redis | 策略/广告组/人群/素材 CRUD、渠道账号、规则引擎、数据看板 |
| Bidding Service | 9090 | Vert.x 4.5.10 + Jackson + Redis | SSP 网关、ADX 竞价引擎、DSP 决策、跟踪服务器、实时指标 |
| 前端 | 5173 | Vue 3 + TypeScript + Element Plus + ECharts + Pinia | 管理界面（8 个路由页面） |

RTB 竞价链路：`媒体请求 → SSP 网关 → ADX Engine → DSP 决策（策略匹配 → 出价 → 预算扣减 → 素材选择）→ 返回广告 → 跟踪服务器（曝光/点击/转化）`

## 核心功能

- **数据看板**：核心指标卡（消耗/新客/CPA/ROAS）、日级趋势、渠道分布、素材 TOP 榜（Redis 按天缓存）
- **策略管理**：7 条种子策略（S1–S7），基于 2024 年 4–6 月真实投放数据校准，支持预算/渠道/人群/素材关联与状态流转
- **广告组管理**：列表筛选、批量启停、详情实时数据
- **人群 / 素材管理**：人群包、素材库、素材 CTR/CPA 衰减曲线分析
- **规则引擎**：表单式配置「指标 + 阈值 + 动作」，支持优先级仲裁、冷却期、沙箱历史回放测试、执行日志，内置「test 计划自动拦截」保护规则
- **RTB 竞价**：6 大策略优先级匹配、设备前缀定向、出价公式 `目标CPA × 出价系数 × 10`、预算引擎、频控
- **系统设置**：预算总览、渠道账号、媒体（Publisher）/广告位（AdSlot）管理

## 目录结构

```
ad-platform/
├── backend/            # Management Service（Spring Boot）
│   └── src/main/resources/db/   # 建表与种子数据 SQL
├── bidding-service/    # Bidding Service（Vert.x，SSP/ADX/DSP/跟踪）
├── frontend/           # Vue 3 管理前端
├── nginx.conf          # 反向代理配置
├── docker-compose.yml  # Redis（本地开发基础设施）
└── run-demo.sh         # 一键演示脚本
docs/
├── ref/                # 产品设计说明书、程序化广告流程、历史投放数据
└── superpowers/        # 产品 spec 与分阶段实施计划
```

## 快速开始

### 前置要求

- JDK 17、Maven 3.8+、Node.js 18+
- MySQL 8.0（本机或自行用 Docker 启动）
- Redis 7（可用 `docker compose up -d redis` 启动）

### 1. 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ad_platform DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p ad_platform < backend/src/main/resources/db/init-schema.sql
mysql -u root -p ad_platform < backend/src/main/resources/db/migration-v2.sql
mysql -u root -p ad_platform < backend/src/main/resources/db/migration-v3.sql
mysql -u root -p ad_platform < backend/src/main/resources/db/seed-data-bulk.sql
```

数据库连接配置见 `backend/src/main/resources/application.yml`（默认 `localhost:3306`，账号 `root/1234`，请按需修改）。

### 2. 配置密钥（本仓库不包含任何密钥）

**后端**：创建 `backend/src/main/resources/application-local.yml`（已被 .gitignore 排除），填入 Claude API Key（用于 AI 分析功能）：

```yaml
claude:
  api-key: 你的密钥
```

**前端**：复制 `.env.example` 为 `.env`（已被 .gitignore 排除），填入 DeepSeek API Key：

```bash
cd frontend && cp .env.example .env
# 编辑 .env，填入 VITE_AI_API_KEY=你的密钥
```

### 3. 启动服务

```bash
# Redis
docker compose up -d redis

# Management Service（:8080）
cd backend && mvn spring-boot:run

# Bidding Service（:9090）
cd bidding-service && mvn compile exec:java -Dexec.mainClass="com.ad.bidding.BiddingApplication"

# 前端（:5173）
cd frontend && npm install && npm run dev
```

访问管理界面：http://localhost:5173

### 4. 运行 RTB 演示

```bash
# 流量模拟器（参数：总请求数 QPS，如 500 请求 × 50 QPS）
cd bidding-service && mvn compile exec:java -Dexec.mainClass="com.ad.bidding.simulator.MediaSimulator" -Dexec.args="500 50"

# 实时竞价仪表盘（2 秒自动刷新）
# http://localhost:9090/dashboard
```

## 文档

| 文档 | 说明 |
|------|------|
| [开发过程记录](ad-platform/docs/DEVELOPMENT_PROCESS.md) | 完整开发记录：任务清单、表结构、启动方式、已知问题 |
| [产品设计文档](docs/superpowers/specs/2026-07-17-lumi-ad-platform-design.md) | 投放中台产品设计（策略、规则引擎、看板、AI 增强） |
| [ADX 平台设计](docs/superpowers/specs/2026-07-17-lumi-adx-platform-design.md) | RTB 竞价平台设计 |
| [实施计划](docs/superpowers/plans/) | P1/P2/P3 分阶段实施计划 |
| [产品设计说明书](docs/ref/产品设计说明书1.md) | 需求背景与业务说明 |

## 安全说明

- **密钥不入库**：API Key 统一放在 gitignored 的 `application-local.yml` / `.env` 中，模板见 `application.yml` 注释与 `.env.example`
- **仓库包含客户历史投放数据**（`docs/ref/客户历史数据_v3.csv`），请保持仓库**私有**，勿公开发布

## 网络说明（国内环境）

本仓库本地 git 已配置代理（Clash Verge `127.0.0.1:7897`）。新克隆环境如无法直连 GitHub，执行：

```bash
git config http.proxy http://127.0.0.1:7897
git config https.proxy http://127.0.0.1:7897
```
