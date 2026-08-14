-- ============================================================
-- Migration V3: Channel Account Table
-- ============================================================

USE ad_platform;

CREATE TABLE IF NOT EXISTS ad_channel_account (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL                COMMENT '账号名称',
    channel         VARCHAR(32)     NOT NULL                COMMENT '渠道编码: DOUYIN/TENCENT/BAIDU_FEED/XIAOHONGSHU/BILIBILI',
    app_id          VARCHAR(128)    DEFAULT NULL            COMMENT '渠道应用ID',
    app_secret      VARCHAR(256)    DEFAULT NULL            COMMENT '渠道密钥(加密存储)',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '0-禁用 1-启用',
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     DEFAULT NULL,
    updated_by      VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_channel (channel),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道账号';

-- Seed data for demo
INSERT INTO ad_channel_account (name, channel, app_id, app_secret, status) VALUES
('巨量引擎-主账户',    'DOUYIN',      'dy_app_001', 'sk_demo_douyin_secret', 1),
('腾讯广告-主账户',    'TENCENT',     'tx_app_001', 'sk_demo_tencent_secret', 1),
('百度信息流-主账户',  'BAIDU_FEED',  'bd_app_001', 'sk_demo_baidu_secret', 1),
('小红书-品牌账户',    'XIAOHONGSHU', 'xhs_app_001','sk_demo_xiaohongshu_secret', 1),
('B站-花火账户',      'BILIBILI',    'bl_app_001', 'sk_demo_bilibili_secret', 1);
