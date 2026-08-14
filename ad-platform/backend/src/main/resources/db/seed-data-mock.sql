-- ============================================================
-- Mock Business Data: Campaigns + Hourly Stats
-- Run AFTER init-schema.sql + seed-data.sql
--
-- Generates:
--   - 15 campaigns across 6 active strategies
--   - 30 days of hourly stats (~10,800 rows @ 15×30×24)
--   - ~10,800 hourly stats rows
-- ============================================================

SET NAMES utf8mb4;
USE ad_platform;

-- Safe to re-run
DELETE FROM ad_stats_hourly;
DELETE FROM ad_campaign;
ALTER TABLE ad_campaign AUTO_INCREMENT = 1;
ALTER TABLE ad_stats_hourly AUTO_INCREMENT = 1;

-- ============================================================
-- 1. Campaigns (15 across 6 active strategies, S7 skipped)
-- ============================================================

-- Strategy 1: 巨量引擎·重定向爆款 (DOUYIN, ¥12万, CPA=250)
INSERT INTO ad_campaign (strategy_id, name, channel, platform_campaign_id, budget_daily, bid_price, bid_type, status, launch_at, created_by, created_at, updated_at) VALUES
(1, 'S1-爆款重定向-高活',   'DOUYIN', 'DY_CAMP_001', 2500.00, 12.00, 'OCPM', 1, '2026-06-18', 'system', NOW(), NOW()),
(1, 'S1-爆款重定向-潜客',   'DOUYIN', 'DY_CAMP_002', 2000.00, 10.00, 'OCPM', 1, '2026-06-20', 'system', NOW(), NOW()),
(1, 'S1-爆款重定向-扩展',   'DOUYIN', 'DY_CAMP_003', 1500.00, 8.00,  'OCPM', 1, '2026-06-22', 'system', NOW(), NOW());

-- Strategy 2: 小红书·成分党深耕 (XIAOHONGSHU, ¥10万, CPA=250)
INSERT INTO ad_campaign (strategy_id, name, channel, platform_campaign_id, budget_daily, bid_price, bid_type, status, launch_at, created_by, created_at, updated_at) VALUES
(2, 'S2-成分党-烟酰胺',    'XIAOHONGSHU', 'XHS_CAMP_001', 2000.00, 8.00,  'OCPM', 1, '2026-06-19', 'system', NOW(), NOW()),
(2, 'S2-成分党-美白精华',  'XIAOHONGSHU', 'XHS_CAMP_002', 1500.00, 8.00,  'OCPM', 1, '2026-06-21', 'system', NOW(), NOW());

-- Strategy 3: B站·新品破圈 (BILIBILI, ¥8万, CPA=300)
INSERT INTO ad_campaign (strategy_id, name, channel, platform_campaign_id, budget_daily, bid_price, bid_type, status, launch_at, created_by, created_at, updated_at) VALUES
(3, 'S3-新品破圈-美妆UP主',  'BILIBILI', 'BL_CAMP_001', 1800.00, 15.00, 'CPM', 1, '2026-06-23', 'system', NOW(), NOW()),
(3, 'S3-新品破圈-开屏曝光',  'BILIBILI', 'BL_CAMP_002', 1000.00, 20.00, 'CPM', 1, '2026-06-25', 'system', NOW(), NOW());

-- Strategy 4: 腾讯·弃单重定向 (TENCENT, ¥8万, CPA=200)
INSERT INTO ad_campaign (strategy_id, name, channel, platform_campaign_id, budget_daily, bid_price, bid_type, status, launch_at, created_by, created_at, updated_at) VALUES
(4, 'S4-弃单-限时优惠',  'TENCENT', 'TX_CAMP_001', 2000.00, 6.00, 'OCPM', 1, '2026-06-18', 'system', NOW(), NOW()),
(4, 'S4-弃单-满减券',    'TENCENT', 'TX_CAMP_002', 1500.00, 6.00, 'OCPM', 1, '2026-06-24', 'system', NOW(), NOW());

-- Strategy 5: 百度·竞品截流 (BAIDU_FEED, ¥12万, CPA=250)
INSERT INTO ad_campaign (strategy_id, name, channel, platform_campaign_id, budget_daily, bid_price, bid_type, status, launch_at, created_by, created_at, updated_at) VALUES
(5, 'S5-竞品截流-HBN',   'BAIDU_FEED', 'BD_CAMP_001', 2500.00, 10.00, 'OCPM', 1, '2026-06-19', 'system', NOW(), NOW()),
(5, 'S5-竞品截流-成分对比', 'BAIDU_FEED', 'BD_CAMP_002', 2000.00, 10.00, 'OCPM', 1, '2026-06-22', 'system', NOW(), NOW());

-- Strategy 6: 品牌搜索防御 (BAIDU_SEARCH + DOUYIN, ¥16万, CPA=150)
INSERT INTO ad_campaign (strategy_id, name, channel, platform_campaign_id, budget_daily, bid_price, bid_type, status, launch_at, created_by, created_at, updated_at) VALUES
(6, 'S6-品牌搜索-品牌词',  'BAIDU_SEARCH', 'BS_CAMP_001', 2500.00, 5.00,  'CPC', 1, '2026-06-18', 'system', NOW(), NOW()),
(6, 'S6-品牌搜索-长尾词',  'BAIDU_SEARCH', 'BS_CAMP_002', 1500.00, 3.00,  'CPC', 1, '2026-06-20', 'system', NOW(), NOW()),
(6, 'S6-品牌防御-抖音',    'DOUYIN', 'DY_CAMP_004', 2000.00, 8.00,  'OCPM', 1, '2026-06-21', 'system', NOW(), NOW());

-- ============================================================
-- 2. Generate hourly stats (temp table → INSERT)
-- ============================================================

DROP TEMPORARY TABLE IF EXISTS _campaign_params;

CREATE TEMPORARY TABLE _campaign_params AS
WITH RECURSIVE
dates AS (
    SELECT DATE_SUB(CURDATE(), INTERVAL 29 DAY) AS dt
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY) FROM dates WHERE dt < CURDATE()
),
hours AS (
    SELECT 0 AS h UNION SELECT 1  UNION SELECT 2  UNION SELECT 3
    UNION SELECT 4  AS h UNION SELECT 5  UNION SELECT 6  UNION SELECT 7
    UNION SELECT 8  AS h UNION SELECT 9  UNION SELECT 10 UNION SELECT 11
    UNION SELECT 12 AS h UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 AS h UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
    UNION SELECT 20 AS h UNION SELECT 21 UNION SELECT 22 UNION SELECT 23
)
SELECT
    c.id        AS campaign_id,
    c.strategy_id,
    c.channel,
    d.dt        AS stat_date,
    h.h         AS stat_hour,
    -- Base hourly impressions — per-campaign scale that keeps daily
    -- cost near 60-80% of budget_daily given each channel's CPC/CTR.
    -- Daytime (8-23)  = full rate; night (0-7) = 10% of daytime.
    -- Random ±30% per row for natural-looking variance.
    ROUND(
        (CASE c.id
            WHEN 1  THEN 1800  WHEN 2  THEN 1500  WHEN 3  THEN 1200
            WHEN 4  THEN 1600  WHEN 5  THEN 1300  WHEN 6  THEN 1200
            WHEN 7  THEN 800   WHEN 8  THEN 1500  WHEN 9  THEN 1200
            WHEN 10 THEN 1300  WHEN 11 THEN 1000  WHEN 12 THEN 3000
            WHEN 13 THEN 1800  WHEN 14 THEN 1200  WHEN 15 THEN 1500
        END) * CASE WHEN h.h BETWEEN 8 AND 23 THEN 1.0 ELSE 0.10 END
        * (0.7 + RAND() * 0.6),
    0) AS impressions,
    -- CTR per campaign (%) — ranges 2.5%-5.0%
    CASE c.id
        WHEN 1  THEN 0.035 WHEN 2  THEN 0.030 WHEN 3  THEN 0.025
        WHEN 4  THEN 0.040 WHEN 5  THEN 0.028 WHEN 6  THEN 0.050
        WHEN 7  THEN 0.045 WHEN 8  THEN 0.038 WHEN 9  THEN 0.032
        WHEN 10 THEN 0.042 WHEN 11 THEN 0.035 WHEN 12 THEN 0.040
        WHEN 13 THEN 0.045 WHEN 14 THEN 0.042 WHEN 15 THEN 0.036
    END AS ctr,
    -- CVR by channel — search converts highest, B站 lowest
    CASE c.channel
        WHEN 'DOUYIN'       THEN 0.025
        WHEN 'XIAOHONGSHU'  THEN 0.020
        WHEN 'BILIBILI'     THEN 0.015
        WHEN 'TENCENT'      THEN 0.030
        WHEN 'BAIDU_FEED'   THEN 0.022
        WHEN 'BAIDU_SEARCH' THEN 0.040
    END AS cvr,
    -- CPC by channel (¥)
    CASE c.channel
        WHEN 'DOUYIN'       THEN 2.5
        WHEN 'XIAOHONGSHU'  THEN 1.8
        WHEN 'BILIBILI'     THEN 1.5
        WHEN 'TENCENT'      THEN 2.0
        WHEN 'BAIDU_FEED'   THEN 2.8
        WHEN 'BAIDU_SEARCH' THEN 1.5
    END AS cpc
FROM ad_campaign c
CROSS JOIN dates d
CROSS JOIN hours h
WHERE d.dt >= DATE(c.launch_at)
ORDER BY c.id, d.dt, h.h;

-- Insert into stats_hourly using the pre-computed params
INSERT INTO ad_stats_hourly
    (channel, strategy_id, campaign_id, stat_date, stat_hour,
     impressions, clicks, micro_conversions, conversions, new_users, cost, gmv)
SELECT
    cp.channel,
    cp.strategy_id,
    cp.campaign_id,
    cp.stat_date,
    cp.stat_hour,
    cp.impressions,
    GREATEST(ROUND(cp.impressions * cp.ctr), 0)              AS clicks,
    GREATEST(ROUND(cp.impressions * cp.ctr * cp.cvr * 10), 0) AS micro_conversions,
    GREATEST(ROUND(cp.impressions * cp.ctr * cp.cvr), 0)      AS conversions,
    GREATEST(ROUND(cp.impressions * cp.ctr * cp.cvr * 0.5), 0) AS new_users,
    ROUND(cp.impressions * cp.ctr * cp.cpc, 2)                AS cost,
    ROUND(cp.impressions * cp.ctr * cp.cvr * 60 * (0.8 + RAND() * 0.4), 2) AS gmv
FROM _campaign_params cp;

DROP TEMPORARY TABLE IF EXISTS _campaign_params;

-- ============================================================
-- 3. Summary report
-- ============================================================

SELECT '=== Campaigns Created ===' AS msg, COUNT(*) AS cnt FROM ad_campaign;
SELECT '=== Hourly Stats Inserted ===' AS msg, COUNT(*) AS cnt FROM ad_stats_hourly;

SELECT 'Channel Cost Breakdown' AS '';
SELECT channel,
       COUNT(DISTINCT campaign_id) AS campaigns,
       FORMAT(SUM(impressions), 0) AS impressions,
       FORMAT(SUM(clicks), 0) AS clicks,
       SUM(conversions) AS conversions,
       ROUND(SUM(cost), 2) AS cost,
       ROUND(SUM(gmv), 2) AS gmv
FROM ad_stats_hourly
GROUP BY channel
ORDER BY cost DESC;

SELECT 'Total Overview (30 days)' AS '';
SELECT
    ROUND(SUM(cost), 2)         AS total_cost,
    SUM(conversions)            AS total_conversions,
    FORMAT(SUM(impressions), 0) AS total_impressions,
    FORMAT(SUM(clicks), 0)      AS total_clicks,
    ROUND(SUM(gmv), 2)          AS total_gmv,
    ROUND(SUM(cost) / NULLIF(SUM(conversions), 0), 2) AS overall_cpa,
    ROUND(SUM(gmv) / NULLIF(SUM(cost), 0), 2)         AS overall_roas
FROM ad_stats_hourly;
