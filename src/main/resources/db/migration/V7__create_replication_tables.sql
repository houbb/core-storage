-- ============================================================
-- P6 Replication Runtime: 数据复制与同步
-- ============================================================

-- 1. storage_replica：资源副本表
-- Resource 可以通过多个 Replica 在不同存储后端保存多份数据
CREATE TABLE IF NOT EXISTS storage_replica
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid  TEXT    NOT NULL,
    profile_name   TEXT    NOT NULL,
    driver_name    TEXT    NOT NULL,
    replica_role   TEXT    NOT NULL DEFAULT 'SECONDARY',
    replica_status TEXT    NOT NULL DEFAULT 'CREATING',
    version        INTEGER DEFAULT 1,
    checksum       TEXT,
    sync_time      DATETIME,
    create_time    DATETIME
);

CREATE INDEX IF NOT EXISTS idx_replica_resource ON storage_replica (resource_uuid);
CREATE INDEX IF NOT EXISTS idx_replica_profile ON storage_replica (profile_name);
CREATE INDEX IF NOT EXISTS idx_replica_status ON storage_replica (replica_status);

-- 2. storage_sync_task：同步任务表
-- Task + Scheduler 模型，避免引入 MQ，保持 MVP 到企业版的一致架构
CREATE TABLE IF NOT EXISTS storage_sync_task
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    task_type       TEXT    NOT NULL,
    resource_uuid   TEXT    NOT NULL,
    source_profile  TEXT    NOT NULL,
    target_profile  TEXT    NOT NULL,
    status          TEXT    NOT NULL DEFAULT 'PENDING',
    progress        INTEGER DEFAULT 0,
    error_message   TEXT,
    create_time     DATETIME,
    update_time     DATETIME
);

CREATE INDEX IF NOT EXISTS idx_sync_task_resource ON storage_sync_task (resource_uuid);
CREATE INDEX IF NOT EXISTS idx_sync_task_status ON storage_sync_task (status);
