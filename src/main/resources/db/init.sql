-- 文件上传系统 数据库初始化脚本
-- 执行前请先创建数据库：CREATE DATABASE file_upload CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE file_upload;

-- ----------------------------
-- 系统用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    open_id       VARCHAR(64)  NOT NULL COMMENT '飞书 open_id',
    user_name     VARCHAR(64)  NOT NULL COMMENT '用户姓名',
    email         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    department_id   VARCHAR(64)  DEFAULT NULL COMMENT '飞书部门 ID',
    department_name VARCHAR(128) DEFAULT NULL COMMENT '部门名称',
    role          VARCHAR(16)  NOT NULL DEFAULT 'user' COMMENT 'admin/user',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_id (open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ----------------------------
-- 目录树
-- ----------------------------
CREATE TABLE IF NOT EXISTS directories (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id     BIGINT       DEFAULT NULL COMMENT '父目录 ID，顶级为 NULL',
    name          VARCHAR(128) NOT NULL COMMENT '目录名称',
    full_path     VARCHAR(512) NOT NULL COMMENT '完整路径，如 /集团/美妆/产品营销',
    template_id   BIGINT       DEFAULT NULL COMMENT '绑定模板 ID，仅最下级目录有值',
    upload_cycle  VARCHAR(16)  DEFAULT NULL COMMENT '上报周期 daily/weekly/monthly',
    cycle_deadline VARCHAR(32) DEFAULT NULL COMMENT '周期截止时间配置',
    created_by    BIGINT       NOT NULL COMMENT '创建人 ID',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_parent_id (parent_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目录树';

-- ----------------------------
-- 目录权限
-- ----------------------------
CREATE TABLE IF NOT EXISTS directory_permissions (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    directory_id    BIGINT      NOT NULL COMMENT '目录 ID',
    user_id         BIGINT      NOT NULL COMMENT '用户 ID',
    permission_type VARCHAR(32) NOT NULL COMMENT 'upload/view_history',
    is_deleted      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dir_user_perm (directory_id, user_id, permission_type),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目录权限';

-- ----------------------------
-- 模板主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS templates (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(128) NOT NULL COMMENT '模板名称',
    directory_id    BIGINT       NOT NULL COMMENT '所属最下级目录 ID',
    status          VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/active/disabled',
    current_version VARCHAR(16)  DEFAULT NULL COMMENT '当前最新版本号',
    form_fields     JSON         DEFAULT NULL COMMENT '上传表单字段配置',
    dirty_threshold DECIMAL(5,2) DEFAULT 10.00 COMMENT '脏数据阈值百分比',
    created_by      BIGINT       NOT NULL COMMENT '创建人 ID',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_directory_id (directory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板主表';

-- ----------------------------
-- 模板版本
-- ----------------------------
CREATE TABLE IF NOT EXISTS template_versions (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id       BIGINT       NOT NULL COMMENT '所属模板 ID',
    version           VARCHAR(16)  NOT NULL COMMENT '版本号',
    target_table_name VARCHAR(128) NOT NULL COMMENT '对应业务数据表名',
    change_log        TEXT         DEFAULT NULL COMMENT '版本变更说明',
    created_by        BIGINT       NOT NULL COMMENT '操作人 ID',
    is_deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（版本不可删）',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本';

-- ----------------------------
-- 模板字段定义
-- ----------------------------
CREATE TABLE IF NOT EXISTS template_fields (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    version_id       BIGINT       NOT NULL COMMENT '所属版本 ID',
    field_name       VARCHAR(128) NOT NULL COMMENT '字段显示名称',
    source_column    VARCHAR(128) NOT NULL COMMENT '文件列名',
    target_column    VARCHAR(128) NOT NULL COMMENT '目标表列名',
    field_type       VARCHAR(32)  NOT NULL COMMENT 'int/varchar/decimal/date/datetime',
    field_order      INT          NOT NULL DEFAULT 0 COMMENT '列顺序',
    is_required      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填',
    constant_value   VARCHAR(256) DEFAULT NULL COMMENT '常量映射值',
    is_skipped       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否跳过',
    validation_rules JSON         DEFAULT NULL COMMENT '校验规则配置',
    is_deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_version_id (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板字段定义';

-- ----------------------------
-- 上传任务记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS upload_tasks (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    directory_id BIGINT       NOT NULL COMMENT '所属目录 ID',
    template_id  BIGINT       DEFAULT NULL COMMENT '模板 ID',
    version_id   BIGINT       DEFAULT NULL COMMENT '使用的模板版本 ID',
    file_name    VARCHAR(256) NOT NULL COMMENT '原始文件名',
    file_size    BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
    file_path    VARCHAR(512) DEFAULT NULL COMMENT '服务器临时存储路径',
    oss_path     VARCHAR(512) DEFAULT NULL COMMENT 'OSS 存储路径',
    status       VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT 'pending/validating/processing/success/failed/partial_failed',
    row_count    INT          DEFAULT NULL COMMENT '成功写入行数',
    error_count  INT          DEFAULT NULL COMMENT '错误行数',
    retry_count  INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    uploaded_by  BIGINT       NOT NULL COMMENT '上传人用户 ID',
    form_data    JSON         DEFAULT NULL COMMENT '上传时填写的表单数据',
    is_no_data   TINYINT      NOT NULL DEFAULT 0 COMMENT '无数据申报',
    expire_at    DATETIME     DEFAULT NULL COMMENT '文件过期时间',
    is_deleted   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_task_dir_status_time (directory_id, status, create_time),
    INDEX idx_uploaded_by (uploaded_by),
    INDEX idx_expire_at (expire_at, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传任务记录';

-- ----------------------------
-- 校验错误明细
-- ----------------------------
CREATE TABLE IF NOT EXISTS upload_errors (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id       BIGINT       NOT NULL COMMENT '所属任务 ID',
    row_num       INT          NOT NULL COMMENT '出错行号',
    column_name   VARCHAR(128) DEFAULT NULL COMMENT '出错列名',
    error_type    VARCHAR(64)  NOT NULL COMMENT '错误类型',
    error_message VARCHAR(512) NOT NULL COMMENT '错误描述',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校验错误明细';

-- ----------------------------
-- 分片上传记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS upload_chunk_records (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id      BIGINT       NOT NULL COMMENT '所属任务 ID',
    total_chunks INT          NOT NULL COMMENT '总分片数',
    chunk_index  INT          NOT NULL COMMENT '当前分片索引（0-based）',
    chunk_path   VARCHAR(512) NOT NULL COMMENT '分片临时文件路径',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_chunk (task_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分片上传记录';

-- ----------------------------
-- 监控状态快照
-- ----------------------------
CREATE TABLE IF NOT EXISTS monitor_status (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    directory_id BIGINT      NOT NULL COMMENT '目录 ID',
    user_id      BIGINT      NOT NULL COMMENT '用户 ID',
    period_key   VARCHAR(32) NOT NULL COMMENT '周期键，如 2026-W18/2026-04',
    status       VARCHAR(16) NOT NULL COMMENT 'no_upload/failed/success/no_data',
    task_count   INT         DEFAULT 0 COMMENT '本周期任务数',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dir_user_period (directory_id, user_id, period_key),
    INDEX idx_monitor_dir_period (directory_id, period_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监控状态快照';

-- ----------------------------
-- 告警规则
-- ----------------------------
CREATE TABLE IF NOT EXISTS alert_rules (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_name   VARCHAR(128)  NOT NULL COMMENT '规则名称',
    metric_type VARCHAR(64)   NOT NULL COMMENT '指标类型',
    threshold   DECIMAL(10,2) NOT NULL COMMENT '阈值',
    alert_level VARCHAR(16)   NOT NULL COMMENT 'critical/warning/info',
    is_enabled  TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用',
    webhook_url VARCHAR(512)  DEFAULT NULL COMMENT '飞书 Webhook URL',
    is_deleted  TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';

-- ----------------------------
-- 操作审计日志
-- ----------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator_id    BIGINT      NOT NULL COMMENT '操作人 ID',
    operator_name  VARCHAR(64) NOT NULL COMMENT '操作人姓名',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    resource_type  VARCHAR(32) DEFAULT NULL COMMENT '资源类型',
    resource_id    BIGINT      DEFAULT NULL COMMENT '资源 ID',
    before_value   TEXT        DEFAULT NULL COMMENT '变更前值',
    after_value    TEXT        DEFAULT NULL COMMENT '变更后值',
    ip_address     VARCHAR(64) DEFAULT NULL COMMENT '操作 IP',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- ----------------------------
-- 初始化默认告警规则
-- ----------------------------
INSERT INTO alert_rules (rule_name, metric_type, threshold, alert_level, is_enabled) VALUES
('上传失败率告警',      'failure_rate',     10.00, 'critical', 1),
('单文件解析耗时告警',  'parse_duration',   300.0, 'warning',  1),
('队列堆积告警',        'queue_size',       200.0, 'warning',  1),
('并发用户数告警',      'concurrent_users', 60.00, 'info',     1),
('存储连续失败告警',    'storage_failure',  3.00,  'critical', 1);
