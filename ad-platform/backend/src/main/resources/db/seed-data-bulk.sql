-- ============================================================
-- Bulk Strategy & Campaign Seed Data for Demo
-- Inserts realistic strategies and campaigns to match
-- the in-memory RTB engine config
-- ============================================================

SET NAMES utf8mb4;
USE ad_platform;

-- Safe cleanup for re-runs
DELETE FROM ad_ad_slot;
DELETE FROM ad_publisher;
DELETE FROM ad_strategy;
DELETE FROM ad_material;
ALTER TABLE ad_strategy AUTO_INCREMENT = 1;
ALTER TABLE ad_publisher AUTO_INCREMENT = 1;
ALTER TABLE ad_ad_slot AUTO_INCREMENT = 1;
ALTER TABLE ad_material AUTO_INCREMENT = 1;

-- Strategies (RTB优先级: S4>S3>S1>S2>S5>S6)
INSERT IGNORE INTO ad_strategy (name, code, status, objective, description, budget, target_cpa, target_cvr, expected_roas, budget_ratio, sort_order, bid_rate, frequency_cap, time_range, rtb_status) VALUES
('高价值人群精准转化', 'S1_HIGH_VALUE',  1, '针对高价值人群精准出价，提升ROI', '基于历史购买数据和RFM模型，锁定近30天有购买行为的高活跃用户，利用OCPM出价争取优质曝光，目标ROI≥3.5', 5000.0000, 250.0000, 0.0500, 3.5, 0.25, 1, 0.4, 10, '09:00-23:00', 1),
('新品破圈拉新',       'S2_NEW_USER',    1, '拓展新用户群体，降低拉新成本', '针对竞品品牌兴趣人群和泛美妆兴趣人群投放，通过小红书/B站种草内容降低拉新CPA，目标CPA≤300', 3000.0000, 300.0000, 0.0300, 2.0, 0.15, 2, 0.25, 5,  '08:00-22:00', 1),
('竞品截流抢夺',       'S3_COMPETE',     1, '拦截竞品流量，抢占市场份额', '在百度信息流和抖音上拦截竞品品牌词搜索流量，主推成分对比内容，抢夺竞品品牌意向用户', 4000.0000, 250.0000, 0.0400, 2.8, 0.20, 3, 0.5,  8,  '08:00-23:00', 1),
('弃单重定向强转化',   'S4_RETARGET',    1, '针对加购未下单用户强召回', '72小时内多次触达加购/收藏但未支付的用户，配合限时优惠券和满减活动，最大化回流转化率', 3000.0000, 200.0000, 0.0800, 4.0, 0.15, 4, 0.6,  5,  '00:00-23:59', 1),
('智能通投探索',       'S5_SMART',       1, '自动探索优质流量，补充剩余预算', '使用系统自动出价和扩量能力，在全渠道探索未覆盖的优质流量，补充预算消耗的同时控制CPA在目标范围内', 5000.0000, 350.0000, 0.0200, 1.5, 0.25, 5, 0.15, 10, '00:00-23:59', 1),
('兜底保量',           'S6_FALLBACK',    1, '填充剩余流量，保底消耗', '当高优先级策略未消耗完预算时自动补充投放，以较低出价覆盖长尾流量，确保整体预算利用率', 2000.0000, 500.0000, 0.0100, 1.0, 0.00, 6, 0.10, 15, '00:00-23:59', 0);

-- Publishers
INSERT INTO ad_publisher (name, code, contact, api_token, revenue_share, status) VALUES
('今日热点',  'MEDIA_TODAY',  '张明',    'token-media-today-001',  0.70, 1),
('趣闻汇',    'MEDIA_QUWEN', '李华',    'token-media-quwen-001',  0.65, 1),
('游戏快报',  'MEDIA_GAME',  '王强',    'token-media-game-001',   0.75, 1),
('科技前沿',  'MEDIA_TECH',  '赵丽',    'token-media-tech-001',   0.70, 1),
('生活精选',  'MEDIA_LIFE',  '陈静',    'token-media-life-001',   0.60, 1);

-- Ad Slots
INSERT INTO ad_ad_slot (publisher_id, name, code, slot_type, width, height, floor_price, block_category, status) VALUES
(1, '首页Banner',   'SLOT_001', 1, 320, 480,  0.01, '["gambling"]', 1),
(1, '信息流大图',   'SLOT_002', 4, 640, 320,  0.02, NULL,           1),
(2, '文章底部Banner','SLOT_003',1, 320, 100,  0.01, '["adult"]',    1),
(2, '插屏广告',     'SLOT_004', 2, 320, 480,  0.03, NULL,           1),
(3, '游戏加载页',   'SLOT_005', 2, 320, 480,  0.05, NULL,           1),
(3, '激励视频',     'SLOT_006', 3, 320, 480,  0.08, NULL,           1),
(4, '科技资讯Banner','SLOT_007',1, 728, 90,   0.02, NULL,           1),
(5, '生活推荐流',   'SLOT_008', 4, 640, 320,  0.01, '["gambling","adult"]', 1);

-- Materials
INSERT INTO ad_material (name, code, type, status, score) VALUES
('新品上市-视频',   'C001', 'video',      1, 85),
('品牌故事-图片',   'C002', 'image',      1, 78),
('限时优惠-图片',   'C003', 'image_text', 1, 92),
('双十一预热-视频', 'C004', 'video',      1, 88),
('会员专享-图片',   'C005', 'image',      1, 75),
('爆款推荐-视频',   'C006', 'video',      1, 90),
('新用户礼包-图片', 'C007', 'image',      1, 82),
('竞品对比-图文',   'C008', 'image_text', 1, 80);
