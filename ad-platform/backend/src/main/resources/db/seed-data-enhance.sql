-- ============================================================
-- Enhanced Data: Test Campaigns + Weekend Pattern + Aging
-- Run AFTER seed-data-mock.sql
-- Adds "real world" patterns observed in the CSV data
-- ============================================================

SET NAMES utf8mb4;
USE ad_platform;

-- ============================================================
-- 1. Test Campaigns (simulate real test_删我 patterns from CSV)
-- ============================================================

-- Add test campaigns that burn budget with 0 conversions
INSERT INTO ad_campaign (strategy_id, name, channel, platform_campaign_id, budget_daily, bid_price, bid_type, status, launch_at, created_by, created_at, updated_at) VALUES
(4, 'test_删我_0615',   'TENCENT',     'TX_TEST_001', 500.00,  5.00,  'OCPM', 1, '2026-06-15', 'operator', NOW(), NOW()),
(5, 'test_删我_0620',   'BAIDU_FEED',  'BD_TEST_001', 300.00,  8.00,  'OCPM', 1, '2026-06-20', 'operator', NOW(), NOW()),
(1, '测试_勿动_0625',   'DOUYIN',      'DY_TEST_001', 400.00,  6.00,  'OCPM', 1, '2026-06-25', 'operator', NOW(), NOW());

-- Add test stats: some spend but near-zero conversions
INSERT INTO ad_stats_hourly (channel, strategy_id, campaign_id, stat_date, stat_hour, impressions, clicks, micro_conversions, conversions, new_users, cost, gmv)
SELECT
    c.channel,
    c.strategy_id,
    c.id,
    d.dt,
    h.h,
    ROUND(200 + RAND() * 800) AS impressions,
    ROUND(5 + RAND() * 20) AS clicks,
    0 AS micro_conversions,
    ROUND(RAND() * 0.3) AS conversions,  -- mostly 0
    ROUND(RAND() * 0.2) AS new_users,     -- mostly 0
    ROUND((5 + RAND() * 20) * 2.5, 2) AS cost,
    0 AS gmv
FROM ad_campaign c
CROSS JOIN (
    SELECT DATE_SUB(CURDATE(), INTERVAL a DAY) AS dt FROM (
        SELECT 0 AS a UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
    ) days
) d
CROSS JOIN (
    SELECT 10 AS h UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17
) h
WHERE c.id IN (16, 17, 18)
  AND d.dt >= c.launch_at;

-- ============================================================
-- 2. Weekend Effect: Reduce conversions, raise CPA on Sat/Sun
-- ============================================================

-- Multiply conversions by 0.6 on weekends (higher CPA)
UPDATE ad_stats_hourly
SET conversions = GREATEST(ROUND(conversions * 0.6), 0),
    new_users = GREATEST(ROUND(new_users * 0.6), 0),
    micro_conversions = GREATEST(ROUND(micro_conversions * 0.6), 0)
WHERE DAYOFWEEK(stat_date) IN (1, 7)  -- Sunday=1, Saturday=7
  AND campaign_id <= 15;

-- ============================================================
-- 3. Material Fatigue: Older materials show declining CTR
--    C001/C003 (used since campaign launch) degrade over time
--    C007 (highest rated) stays strong
-- ============================================================

-- Reduce clicks for campaigns using aging materials
-- S3 uses C001 (KOL测评15s) - degrade CTR by 20%
UPDATE ad_stats_hourly s
JOIN ad_campaign c ON s.campaign_id = c.id
SET s.clicks = GREATEST(ROUND(s.clicks * 0.80), 0)
WHERE c.strategy_id = 3  -- S3 uses C001
  AND s.stat_date >= DATE_SUB(CURDATE(), INTERVAL 15 DAY);

-- S5 uses C008 (成分对比测评) - moderate degradation
UPDATE ad_stats_hourly s
JOIN ad_campaign c ON s.campaign_id = c.id
SET s.clicks = GREATEST(ROUND(s.clicks * 0.90), 0)
WHERE c.strategy_id = 5  -- S5 uses C008
  AND s.stat_date >= DATE_SUB(CURDATE(), INTERVAL 10 DAY);

-- S2 uses C002 (成分图解析) - stays strong, no change

-- ============================================================
-- 4. "Winning" Campaign Pattern: S1-高活 shows improving CPA
--    (simulating the lumi-jh-laxin-3 pattern from CSV)
-- ============================================================

UPDATE ad_stats_hourly
SET conversions = ROUND(conversions * 1.3),
    new_users = ROUND(new_users * 1.3),
    micro_conversions = ROUND(micro_conversions * 1.3)
WHERE campaign_id = 1  -- S1-高活 (best performer)
  AND DAYOFWEEK(stat_date) IN (2, 4, 6);  -- Mon, Wed, Fri - best days

-- ============================================================
-- 5. Summary Report
-- ============================================================

SELECT '=== Enhanced Data Summary ===' AS msg;

SELECT 'Test campaigns added' AS `desc`, COUNT(*) AS cnt FROM ad_campaign WHERE id >= 16;
SELECT CONCAT('Weekend conversions adjusted: ', COUNT(*)) AS result FROM ad_stats_hourly WHERE DAYOFWEEK(stat_date) IN (1, 7) AND campaign_id <= 15;
SELECT CONCAT('Aging materials adjusted: ', COUNT(*)) AS result FROM ad_stats_hourly s JOIN ad_campaign c ON s.campaign_id = c.id WHERE s.stat_date >= DATE_SUB(CURDATE(), INTERVAL 15 DAY) AND c.strategy_id IN (3, 5);

SELECT '=== Final Total Overview (30 days) ===' AS '';
SELECT
    FORMAT(SUM(impressions), 0) AS impressions,
    FORMAT(SUM(clicks), 0) AS clicks,
    SUM(conversions) AS conversions,
    ROUND(SUM(cost), 2) AS total_cost,
    ROUND(SUM(gmv), 2) AS total_gmv,
    ROUND(SUM(cost) / NULLIF(SUM(conversions), 0), 2) AS cpa,
    ROUND(SUM(gmv) / NULLIF(SUM(cost), 0), 2) AS roas
FROM ad_stats_hourly;
