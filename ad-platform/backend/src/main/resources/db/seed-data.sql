-- LUMI AD Platform seed data
-- Generated from spec §4: 7 strategies, 13 audiences, 12 materials, 2 system rules

SET NAMES utf8mb4;
USE ad_platform;

-- ============================================================
-- Audiences (AUD001-AUD013)
-- ============================================================
INSERT INTO ad_audience (id, name, code, source, size_estimate, status, created_by, created_at, updated_at) VALUES
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

-- ============================================================
-- Materials (C001-C012)
-- ============================================================
INSERT INTO ad_material (id, name, code, type, duration, status, score, created_by, created_at, updated_at) VALUES
(1, 'KOL测评15s', 'C001', 'video', 15, 1, 85.00, 'system', NOW(), NOW()),
(2, '成分图解析', 'C002', 'image', 0, 1, 75.00, 'system', NOW(), NOW()),
(3, '品牌故事30s', 'C003', 'video', 30, 1, 70.00, 'system', NOW(), NOW()),
(4, '新品首发15s', 'C004', 'video', 15, 1, 80.00, 'system', NOW(), NOW()),
(5, '精华液痛点', 'C005', 'image_text', 0, 1, 72.00, 'system', NOW(), NOW()),
(6, '限时优惠海报', 'C006', 'image', 0, 1, 78.00, 'system', NOW(), NOW()),
(7, 'KOC种草30s', 'C007', 'video', 30, 1, 92.00, 'system', NOW(), NOW()),
(8, '成分对比测评', 'C008', 'image_text', 0, 1, 82.00, 'system', NOW(), NOW()),
(9, '试用装领取', 'C009', 'image', 0, 1, 68.00, 'system', NOW(), NOW()),
(10, '达播混剪B站', 'C010', 'video', 30, 1, 76.00, 'system', NOW(), NOW()),
(11, '明星测评精剪', 'C011', 'video', 15, 1, 90.00, 'system', NOW(), NOW()),
(12, '成分溯源', 'C012', 'video', 30, 0, 65.00, 'system', NOW(), NOW());

-- ============================================================
-- Strategies (S1-S7)
-- ============================================================

-- S1: 巨量引擎·重定向爆款
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, created_at, updated_at)
VALUES ('巨量引擎·重定向爆款', 'S1', 1, 'CONVERT', '基于Lookalike老客模型扩展相似人群，利用KOC种草素材进行重定向投放', 120000.00, 15.00, 250.00, 2.00, 1, 'system', NOW(), NOW());
SET @s1_id = LAST_INSERT_ID();
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s1_id, 'DOUYIN', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (@s1_id, 5, 0);
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (@s1_id, 7, 1);

-- S2: 小红书·成分党深耕
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, created_at, updated_at)
VALUES ('小红书·成分党深耕', 'S2', 1, 'BRAND', '针对成分党人群投放成分解析素材，提升品牌专业认知', 100000.00, 12.50, 250.00, 1.50, 2, 'system', NOW(), NOW());
SET @s2_id = LAST_INSERT_ID();
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s2_id, 'XIAOHONGSHU', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (@s2_id, 4, 0);
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (@s2_id, 2, 1);
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (@s2_id, 7, 2);

-- S3: B站·新品破圈
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, created_at, updated_at)
VALUES ('B站·新品破圈', 'S3', 1, 'BRAND', 'B站新品破圈投放，面向美妆兴趣用户群', 80000.00, 10.00, 300.00, 1.20, 3, 'system', NOW(), NOW());
SET @s3_id = LAST_INSERT_ID();
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s3_id, 'BILIBILI', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (@s3_id, 1, 0);
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (@s3_id, 1, 1);

-- S4: 腾讯·弃单重定向
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, created_at, updated_at)
VALUES ('腾讯·弃单重定向', 'S4', 1, 'RETARGET', '针对弃单人群进行限时优惠触达，追回流失订单', 80000.00, 10.00, 200.00, 2.50, 4, 'system', NOW(), NOW());
SET @s4_id = LAST_INSERT_ID();
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s4_id, 'TENCENT', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (@s4_id, 6, 0);
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (@s4_id, 6, 1);

-- S5: 百度·竞品截流
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, created_at, updated_at)
VALUES ('百度·竞品截流', 'S5', 1, 'CONVERT', '针对竞品兴趣人群投放成分对比素材，抢夺竞品意向用户', 120000.00, 15.00, 250.00, 1.80, 5, 'system', NOW(), NOW());
SET @s5_id = LAST_INSERT_ID();
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s5_id, 'BAIDU_FEED', 100.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (@s5_id, 3, 0);
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (@s5_id, 8, 1);

-- S6: 品牌搜索防御
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, created_at, updated_at)
VALUES ('品牌搜索防御', 'S6', 1, 'CONVERT', '品牌词搜索拦截，覆盖首触+末触28%归因订单', 160000.00, 20.00, 150.00, 3.00, 6, 'system', NOW(), NOW());
SET @s6_id = LAST_INSERT_ID();
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s6_id, 'BAIDU_SEARCH', 60.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s6_id, 'DOUYIN', 40.00);
INSERT INTO ad_strategy_audience (strategy_id, audience_id, type) VALUES (@s6_id, 12, 0);
INSERT INTO ad_strategy_material (strategy_id, material_id, sort_order) VALUES (@s6_id, 11, 1);

-- S7: AI·智能优选通投 (DRAFT)
INSERT INTO ad_strategy (name, code, status, objective, description, budget, budget_ratio, target_cpa, expected_roas, sort_order, created_by, created_at, updated_at)
VALUES ('AI·智能优选通投', 'S7', 0, 'CONVERT', '全渠道通投验证，AI智能优选素材和人群组合', 140000.00, 17.50, 280.00, 1.50, 7, 'system', NOW(), NOW());
SET @s7_id = LAST_INSERT_ID();
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s7_id, 'DOUYIN', 30.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s7_id, 'XIAOHONGSHU', 25.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s7_id, 'BILIBILI', 20.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s7_id, 'TENCENT', 15.00);
INSERT INTO ad_strategy_channel (strategy_id, channel, budget_ratio) VALUES (@s7_id, 'BAIDU_FEED', 10.00);

-- ============================================================
-- System Rules
-- ============================================================

-- Rule 1: 测试计划自动检测 (built-in, cannot be deleted/disabled)
INSERT INTO ad_rule (name, trigger_metric, trigger_operator, trigger_threshold, trigger_window_hours, action_type, action_params, scope_type, priority, cooldown_minutes, is_system, status, created_by, created_at, updated_at)
VALUES ('测试计划自动检测', 'CONSUME', 'GT', 500.00, 1, 'PAUSE_CAMPAIGN', '{"reason":"疑似测试计划","keywords":"test,测试,删我"}', 'CAMPAIGN', 100, 1440, 1, 1, 'system', NOW(), NOW());

-- Rule 2: CPA超标自动暂停 (user-configurable)
INSERT INTO ad_rule (name, trigger_metric, trigger_operator, trigger_threshold, trigger_window_hours, action_type, action_params, scope_type, priority, cooldown_minutes, is_system, status, created_by, created_at, updated_at)
VALUES ('CPA超标自动暂停', 'CPA', 'GT', 500.00, 24, 'PAUSE_CAMPAIGN', '{"reason":"CPA连续超标","alert":true}', 'STRATEGY', 90, 1440, 0, 1, 'system', NOW(), NOW());
