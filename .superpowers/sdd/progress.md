# SDD Progress Ledger

Started: 2026-07-17

Task 1: Multi-Module Restructure + DB Migration
- ad-platform/pom.xml (aggregator parent)
- backend/pom.xml (modified to child module)
- bidding-service/pom.xml
- BiddingApplication.java
- migration-v2.sql (4 new tables + strategy RTB columns)

Task 2: Bidding Service Skeleton
- config.json (port 9090, Redis/MySQL, tracking baseUrl, timeout 50ms)
- logback.xml (console + rolling file, 7-day retention)
- RedisClientFactory.java
- MainVerticle.java (HTTP server, health endpoint)

Task 3: SSP Gateway
- BidRequest.java
- BidResponse.java
- AdResponse.java
- SspHandler.java (POST /ad/request, pipeline: parse→validate→enrich→call ADX→return)
- MainVerticle.java (mounted /ad/request route)

Task 4: Management Publisher + AdSlot CRUD
- Publisher.java, AdSlot.java (entities)
- PublisherDTO.java, PublisherCreateDTO.java, AdSlotDTO.java, AdSlotCreateDTO.java
- PublisherMapper.java, AdSlotMapper.java
- PublisherService.java/Impl, AdSlotService.java/Impl
- PublisherController.java, AdSlotController.java

Task 5: ADX Engine + DSP Decision Engine
- CampaignConfig.java (@Builder pattern, inner MaterialOption)
- StrategyMatcher.java (5 campaigns, 6 strategies, S1/S4 device-prefix matching)
- Pricer.java (targetCPA × bidRate × 10)
- BudgetEngine.java (ConcurrentHashMap, 5 initial budgets)
- DspDecisionEngine.java (match→price→floor check→deduct→select material→track URLs)
- AdxEngine.java (orchestration wrapper, BID_WIN/BID_LOSE counters)
- SspHandler.java (wired AdxEngine instead of stub)

Task 6: Tracking Server
- EventLogger.java (tab-separated TRACK| log format)
- TrackingHandler.java (1x1 GIF for imp/click/conv, 302 redirect for landing)
- MainVerticle.java (mounted imp/click/conv/landing routes)

Task 7: Strategy RTB Deploy
- StrategyCreateDTO.java (added bidRate, frequencyCap, timeRange, publisherIds, adSlotIds)
- StrategyDeployController.java (deploy/undeploy/deploy-status)
- StrategyDeployService.java/Impl (Redis Hash + Pub/Sub)

Task 8: Docker Compose + Media Simulator + Demo Script
- docker-compose.yml (MySQL 8.0 + Redis 7)
- MediaSimulator.java (Vert.x WebClient, random device prefixes, QPS reporting)
- run-demo.sh (infra→services→seed data→simulator→cleanup)

Task 9: Unit Tests (Bidding Engine)
- PricerTest.java (bid calculation, null handling, rounding)
- BudgetEngineTest.java (has-budget, deduct, floor at zero, reset)
- StrategyMatcherTest.java (device-prefix matching, frequency cap)
- DspDecisionEngineTest.java (full pipeline: win, no-bid, floor failure, new/cp device)

Task 10: Real-time Dashboard + 50 QPS Simulator
- MetricsCollector.java (in-memory strategy-level metrics, ConcurrentHashMap)
- StatsHandler.java (GET /stats — JSON snapshot of all metrics)
- DashboardHandler.java (GET /dashboard — Chart.js HTML dashboard, 2s auto-refresh)
- SspHandler.java (wired MetricsCollector, reports bid outcomes per strategy)
- TrackingHandler.java (reports imp/click/conv to MetricsCollector)
- AdxEngine.java (wired MetricsCollector)
- MainVerticle.java (mounted /stats + /dashboard routes)
- MediaSimulator.java (rewritten: 50 QPS sustained, full user journey: bid→imp→click→conv)
- seed-data-bulk.sql (8 slots, 5 publishers, 6 strategies, 8 materials)
