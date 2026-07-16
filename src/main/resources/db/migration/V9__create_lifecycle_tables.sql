-- ============================================================
-- P8 Lifecycle Runtime: 资源生命周期治理
-- ============================================================

-- 1. 扩展 storage_resource：增加 lifecycle_stage 列
-- 已有资源默认 ACTIVE
ALTER TABLE storage_resource ADD COLUMN lifecycle_stage TEXT NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_resource_type_cat_stage ON storage_resource(resource_type, category, lifecycle_stage);

-- 2. storage_lifecycle_policy：生命周期策略表
-- 策略绑定 resource_type + category，每个组合唯一
-- delete_days=0 表示永久保留（永不过期）
CREATE TABLE IF NOT EXISTS storage_lifecycle_policy
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    policy_name     TEXT    NOT NULL,
    resource_type   TEXT    NOT NULL,
    category        TEXT    NOT NULL,
    active_days     INTEGER NOT NULL DEFAULT 0,
    warm_days       INTEGER NOT NULL DEFAULT 0,
    cold_days       INTEGER NOT NULL DEFAULT 0,
    archive_days    INTEGER NOT NULL DEFAULT 0,
    delete_days     INTEGER NOT NULL DEFAULT 0,
    enabled         INTEGER NOT NULL DEFAULT 1,
    description     TEXT,
    create_time     DATETIME,
    update_time     DATETIME
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_policy_type_cat ON storage_lifecycle_policy(resource_type, category);

-- 3. storage_lifecycle_task：生命周期任务表
-- Task + Scheduler 模型，与 SyncTask 保持一致架构
CREATE TABLE IF NOT EXISTS storage_lifecycle_task
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid   TEXT    NOT NULL,
    policy_id       INTEGER,
    action          TEXT    NOT NULL,
    target_stage    TEXT,
    status          TEXT    NOT NULL DEFAULT 'PENDING',
    execute_time    DATETIME,
    finish_time     DATETIME,
    error_message   TEXT,
    retry_count     INTEGER DEFAULT 0,
    create_time     DATETIME,
    update_time     DATETIME
);

CREATE INDEX IF NOT EXISTS idx_lifecycle_task_resource ON storage_lifecycle_task(resource_uuid);
CREATE INDEX IF NOT EXISTS idx_lifecycle_task_status ON storage_lifecycle_task(status);

-- 4. storage_resource_hold：资源法律保留表
-- 对标 S3 Object Lock、企业 ECM、金融档案系统
CREATE TABLE IF NOT EXISTS storage_resource_hold
(
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid        TEXT    NOT NULL,
    hold_type            TEXT    NOT NULL,
    reason               TEXT,
    operator_id          TEXT,
    expire_time          DATETIME,
    released             INTEGER NOT NULL DEFAULT 0,
    released_time        DATETIME,
    release_operator_id  TEXT,
    create_time          DATETIME
);

CREATE INDEX IF NOT EXISTS idx_hold_resource ON storage_resource_hold(resource_uuid);
CREATE INDEX IF NOT EXISTS idx_hold_active ON storage_resource_hold(resource_uuid, released);

-- 5. 插入默认策略
-- 管理员可在运行时通过 API 修改
INSERT OR IGNORE INTO storage_lifecycle_policy (policy_name, resource_type, category, active_days, warm_days, cold_days, archive_days, delete_days, enabled, description, create_time, update_time)
VALUES
    ('Export-7Days',      'DOCUMENT', 'OTHER',   0, 0, 0, 0, 7,    1, '导出文件 7 天后自动删除', datetime('now'), datetime('now')),
    ('Backup-180Days',    'ARCHIVE',  'BACKUP',  0, 0, 0, 180, 365, 1, '备份 180 天后归档，365 天后删除', datetime('now'), datetime('now')),
    ('Temp-1Day',         'OTHER',    'OTHER',   0, 0, 0, 0, 1,    1, '临时文件 1 天后自动删除', datetime('now'), datetime('now')),
    ('Model3D-90Days',    'MODEL_3D', 'MODEL',   0, 0, 0, 0, 90,   1, '3D 模型 90 天后自动删除', datetime('now'), datetime('now'));
