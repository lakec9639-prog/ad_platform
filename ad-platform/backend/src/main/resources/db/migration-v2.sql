-- ============================================================
-- Migration V2: DSP-ADX-SSP tables for Phase 1
-- ============================================================

USE ad_platform;

-- 1. 媒体方表
CREATE TABLE IF NOT EXISTS ad_publisher (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL                COMMENT '媒体名称',
    code            VARCHAR(20)     NOT NULL UNIQUE         COMMENT '媒体编码',
    contact         VARCHAR(50)     DEFAULT NULL            COMMENT '联系人',
    api_token       VARCHAR(64)     NOT NULL                COMMENT '接入Token',
    revenue_share   DECIMAL(5,2)    DEFAULT 0.70            COMMENT '媒体分成比例',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '0-禁用 1-激活',
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     DEFAULT NULL,
    updated_by      VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='媒体方';

-- 2. 广告位表
CREATE TABLE IF NOT EXISTS ad_ad_slot (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    publisher_id    BIGINT          NOT NULL                COMMENT '所属媒体',
    name            VARCHAR(100)    NOT NULL                COMMENT '广告位名称',
    code            VARCHAR(20)     NOT NULL UNIQUE         COMMENT '广告位编码',
    slot_type       TINYINT         NOT NULL DEFAULT 1      COMMENT '1-Banner 2-插屏 3-激励视频 4-原生',
    width           INT             NOT NULL                COMMENT '广告位宽度',
    height          INT             NOT NULL                COMMENT '广告位高度',
    floor_price     DECIMAL(10,2)   DEFAULT 0.00            COMMENT '底价(CPM)',
    block_category  VARCHAR(500)    DEFAULT NULL            COMMENT '屏蔽品类ID列表(JSON)',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '0-禁用 1-激活',
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     DEFAULT NULL,
    updated_by      VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_publisher (publisher_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告位';

-- 3. 竞价日志表
CREATE TABLE IF NOT EXISTS ad_bid_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    ad_slot_id      BIGINT          NOT NULL                COMMENT '广告位ID',
    campaign_id     BIGINT          DEFAULT NULL            COMMENT '命中的计划ID',
    strategy_id     BIGINT          DEFAULT NULL            COMMENT '命中的策略ID',
    device_id       VARCHAR(64)     DEFAULT NULL            COMMENT '设备ID(脱敏)',
    bid_price       DECIMAL(10,2)   DEFAULT 0.00            COMMENT '出价(CPM)',
    floor_price     DECIMAL(10,2)   DEFAULT 0.00            COMMENT '底价',
    win             TINYINT         DEFAULT 0               COMMENT '是否胜出',
    nbr             TINYINT         DEFAULT NULL            COMMENT '不出价原因码',
    bid_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '竞价时间',
    latency_ms      INT             DEFAULT 0               COMMENT '竞价耗时(ms)',
    PRIMARY KEY (id),
    KEY idx_bid_at (bid_at),
    KEY idx_slot (ad_slot_id),
    KEY idx_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞价日志';

-- 4. 监播日志表
CREATE TABLE IF NOT EXISTS ad_tracking_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    campaign_id     BIGINT          DEFAULT NULL            COMMENT '计划ID',
    strategy_id     BIGINT          DEFAULT NULL            COMMENT '策略ID',
    device_id       VARCHAR(64)     DEFAULT NULL            COMMENT '设备ID(脱敏)',
    track_type      TINYINT         NOT NULL                COMMENT '1-曝光 2-点击 3-转化',
    track_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    PRIMARY KEY (id),
    KEY idx_track_at (track_at),
    KEY idx_type (track_type),
    KEY idx_campaign (campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监播日志';

-- 5. Extend ad_strategy with RTB bidding config
ALTER TABLE ad_strategy
    ADD COLUMN bid_rate         DECIMAL(5,2) DEFAULT NULL COMMENT '出价系数(CPA*bidRate)',
    ADD COLUMN frequency_cap    INT          DEFAULT 10   COMMENT '单用户日曝光上限',
    ADD COLUMN time_range       VARCHAR(11)  DEFAULT NULL COMMENT '投放时段 09:00-23:00',
    ADD COLUMN rtb_status       TINYINT      DEFAULT 0    COMMENT 'RTB状态 0-未上线 1-已上线';
