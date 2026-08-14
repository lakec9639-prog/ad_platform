-- ============================================================
-- Ad Platform Database Initialization Script
-- ============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS ad_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ad_platform;

-- -----------------------------------------------------------
-- 1. 投放策略表 (ad_strategy)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_strategy (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    name            VARCHAR(100)    NOT NULL                 COMMENT '策略名称',
    code            VARCHAR(64)     NOT NULL                 COMMENT '策略编码',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态: 0-草稿 1-启用 2-暂停 3-结束',
    objective       VARCHAR(200)    DEFAULT NULL             COMMENT '投放目标',
    description     VARCHAR(500)    DEFAULT NULL             COMMENT '策略描述',
    budget          DECIMAL(18,4)   DEFAULT NULL             COMMENT '总预算',
    target_cpa      DECIMAL(18,4)   DEFAULT NULL             COMMENT '目标CPA',
    target_cvr      DECIMAL(10,4)   DEFAULT NULL             COMMENT '目标CVR',
    expected_roas   DECIMAL(10,4)   DEFAULT NULL             COMMENT '预期ROAS',
    budget_ratio    DECIMAL(10,4)   DEFAULT NULL             COMMENT '预算分配比例',
    sort_order      INT             DEFAULT 0                COMMENT '排序号',

    -- BaseEntity fields
    version         INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-正常 1-删除',
    created_by      VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    updated_by      VARCHAR(64)     DEFAULT NULL             COMMENT '更新人',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投放策略表';

-- -----------------------------------------------------------
-- 2. 策略渠道关联表 (ad_strategy_channel)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_strategy_channel (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    strategy_id     BIGINT          NOT NULL                 COMMENT '策略ID',
    channel         VARCHAR(32)     NOT NULL                 COMMENT '渠道编码',
    budget_ratio    DECIMAL(10,4)   DEFAULT NULL             COMMENT '该渠道预算占比',

    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_channel (strategy_id, channel),
    KEY idx_strategy_id (strategy_id),
    KEY idx_channel (channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略渠道关联表';

-- -----------------------------------------------------------
-- 3. 人群表 (ad_audience)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_audience (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    name            VARCHAR(100)    NOT NULL                 COMMENT '人群名称',
    code            VARCHAR(64)     NOT NULL                 COMMENT '人群编码',
    source          VARCHAR(32)     NOT NULL                 COMMENT '人群来源: dmp/lookalike/retarget',
    size_estimate   BIGINT          DEFAULT NULL             COMMENT '预估人群规模',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态: 0-待使用 1-使用中',

    -- BaseEntity fields
    version         INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-正常 1-删除',
    created_by      VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    updated_by      VARCHAR(64)     DEFAULT NULL             COMMENT '更新人',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_source (source),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人群表';

-- -----------------------------------------------------------
-- 4. 素材表 (ad_material)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_material (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    name            VARCHAR(200)    NOT NULL                 COMMENT '素材名称',
    code            VARCHAR(64)     NOT NULL                 COMMENT '素材编码',
    type            VARCHAR(16)     NOT NULL                 COMMENT '素材类型: video/image/image_text',
    duration        INT             DEFAULT NULL             COMMENT '视频时长(秒)',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态: 0-审核中 1-生效中 2-衰退中 3-已停止',
    score           INT             DEFAULT NULL             COMMENT '素材评分(0-100)',

    -- BaseEntity fields
    version         INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-正常 1-删除',
    created_by      VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    updated_by      VARCHAR(64)     DEFAULT NULL             COMMENT '更新人',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_type (type),
    KEY idx_status (status),
    KEY idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='素材表';

-- -----------------------------------------------------------
-- 5. 策略人群关联表 (ad_strategy_audience)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_strategy_audience (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    strategy_id     BIGINT          NOT NULL                 COMMENT '策略ID',
    audience_id     BIGINT          NOT NULL                 COMMENT '人群ID',
    type            VARCHAR(16)     NOT NULL DEFAULT 'main'  COMMENT '人群策略: main-主人群 extend-扩展人群 exclude-排除人群',

    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_audience (strategy_id, audience_id, type),
    KEY idx_strategy_id (strategy_id),
    KEY idx_audience_id (audience_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略人群关联表';

-- -----------------------------------------------------------
-- 6. 策略素材关联表 (ad_strategy_material)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_strategy_material (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    strategy_id     BIGINT          NOT NULL                 COMMENT '策略ID',
    material_id     BIGINT          NOT NULL                 COMMENT '素材ID',
    sort_order      INT             DEFAULT 0                COMMENT '排序号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_material (strategy_id, material_id),
    KEY idx_strategy_id (strategy_id),
    KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略素材关联表';

-- -----------------------------------------------------------
-- 7. 广告计划表 (ad_campaign)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_campaign (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    strategy_id         BIGINT          NOT NULL                 COMMENT '所属策略ID',
    name                VARCHAR(200)    NOT NULL                 COMMENT '计划名称',
    channel             VARCHAR(32)     NOT NULL                 COMMENT '投放渠道',
    platform_campaign_id VARCHAR(128)   DEFAULT NULL             COMMENT '平台侧计划ID',
    budget_daily        DECIMAL(18,4)   DEFAULT NULL             COMMENT '日预算',
    bid_price           DECIMAL(18,4)   DEFAULT NULL             COMMENT '出价',
    bid_type            VARCHAR(32)     DEFAULT NULL             COMMENT '出价方式',
    status              TINYINT         NOT NULL DEFAULT 0       COMMENT '状态: 0-搭建中 1-投放中 2-已暂停 3-已停止',
    launch_at           DATETIME        DEFAULT NULL             COMMENT '投放开始时间',
    stop_at             DATETIME        DEFAULT NULL             COMMENT '投放结束时间',

    -- BaseEntity fields
    version             INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本',
    deleted             TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-正常 1-删除',
    created_by          VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    updated_by          VARCHAR(64)     DEFAULT NULL             COMMENT '更新人',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_strategy_id (strategy_id),
    KEY idx_channel (channel),
    KEY idx_status (status),
    KEY idx_platform_campaign_id (platform_campaign_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告计划表';

-- -----------------------------------------------------------
-- 8. 规则表 (ad_rule)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_rule (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    name                VARCHAR(100)    NOT NULL                 COMMENT '规则名称',
    trigger_metric      VARCHAR(32)     NOT NULL                 COMMENT '触发指标: cpa/ctr/cvr/consume',
    trigger_operator    VARCHAR(8)      NOT NULL                 COMMENT '触发运算符: gt/lt/gte/lte',
    trigger_threshold   VARCHAR(64)     NOT NULL                 COMMENT '触发阈值',
    trigger_window_hours INT            DEFAULT NULL             COMMENT '统计窗口(小时)',
    action_type         VARCHAR(32)     NOT NULL                 COMMENT '动作类型',
    action_params       VARCHAR(500)    DEFAULT NULL             COMMENT '动作参数(JSON)',
    scope_type          VARCHAR(16)     DEFAULT NULL             COMMENT '作用域类型: strategy/channel/campaign',
    scope_value         VARCHAR(128)    DEFAULT NULL             COMMENT '作用域值',
    priority            INT             DEFAULT 0                COMMENT '优先级',
    cooldown_minutes    INT             DEFAULT 0                COMMENT '冷却时间(分钟)',
    is_system           TINYINT         NOT NULL DEFAULT 0       COMMENT '是否系统规则: 0-否 1-是',
    status              TINYINT         NOT NULL DEFAULT 0       COMMENT '状态: 0-禁用 1-启用',

    -- BaseEntity fields
    version             INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本',
    deleted             TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-正常 1-删除',
    created_by          VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    updated_by          VARCHAR(64)     DEFAULT NULL             COMMENT '更新人',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_priority (priority),
    KEY idx_trigger_metric (trigger_metric),
    KEY idx_scope_type_value (scope_type, scope_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则表';

-- -----------------------------------------------------------
-- 9. 规则执行日志表 (ad_rule_execution_log)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_rule_execution_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    rule_id         BIGINT          NOT NULL                 COMMENT '规则ID',
    campaign_id     BIGINT          DEFAULT NULL             COMMENT '计划ID',
    trigger_value   VARCHAR(128)    DEFAULT NULL             COMMENT '触发时的指标值',
    action_taken    VARCHAR(64)     NOT NULL                 COMMENT '执行的动作',
    result          VARCHAR(32)     DEFAULT NULL             COMMENT '执行结果: success/failed',
    error_message   VARCHAR(500)    DEFAULT NULL             COMMENT '错误信息',
    executed_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',

    PRIMARY KEY (id),
    KEY idx_rule_id (rule_id),
    KEY idx_campaign_id (campaign_id),
    KEY idx_executed_at (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则执行日志表';

-- -----------------------------------------------------------
-- 10. 小时级统计数据表 (ad_stats_hourly)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_stats_hourly (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    channel             VARCHAR(32)     NOT NULL                 COMMENT '渠道',
    strategy_id         BIGINT          DEFAULT NULL             COMMENT '策略ID',
    campaign_id         BIGINT          DEFAULT NULL             COMMENT '计划ID',
    stat_date           DATE            NOT NULL                 COMMENT '统计日期',
    stat_hour           TINYINT         NOT NULL                 COMMENT '统计小时(0-23)',
    impressions         BIGINT          NOT NULL DEFAULT 0       COMMENT '展示次数',
    clicks              BIGINT          NOT NULL DEFAULT 0       COMMENT '点击次数',
    micro_conversions   BIGINT          NOT NULL DEFAULT 0       COMMENT '微转化数',
    conversions         BIGINT          NOT NULL DEFAULT 0       COMMENT '转化数',
    new_users           BIGINT          NOT NULL DEFAULT 0       COMMENT '新用户数',
    cost                DECIMAL(18,4)   NOT NULL DEFAULT 0       COMMENT '消耗',
    gmv                 DECIMAL(18,4)   NOT NULL DEFAULT 0       COMMENT 'GMV',

    PRIMARY KEY (id, stat_date),
    KEY idx_channel (channel),
    KEY idx_strategy_id (strategy_id),
    KEY idx_campaign_id (campaign_id),
    KEY idx_channel_date (channel, stat_date),
    KEY idx_strategy_date (strategy_id, stat_date),
    KEY idx_campaign_date (campaign_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小时级统计数据表'
PARTITION BY RANGE (YEAR(stat_date) * 100 + MONTH(stat_date)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    PARTITION p202603 VALUES LESS THAN (202604),
    PARTITION p202604 VALUES LESS THAN (202605),
    PARTITION p202605 VALUES LESS THAN (202606),
    PARTITION p202606 VALUES LESS THAN (202607),
    PARTITION p202607 VALUES LESS THAN (202608),
    PARTITION p202608 VALUES LESS THAN (202609),
    PARTITION p202609 VALUES LESS THAN (202610),
    PARTITION p202610 VALUES LESS THAN (202611),
    PARTITION p202611 VALUES LESS THAN (202612),
    PARTITION p202612 VALUES LESS THAN (202701),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
