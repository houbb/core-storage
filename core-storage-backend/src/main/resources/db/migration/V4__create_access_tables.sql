-- ============================================================
-- V4: Access Runtime — 权限策略 / 分享 / 访问日志
-- ============================================================

-- 资源表新增 access_mode 字段（默认 PUBLIC，向前兼容）
ALTER TABLE storage_resource ADD COLUMN access_mode TEXT DEFAULT 'PUBLIC';

-- 访问策略表 — 多行策略：一个资源可有多行，不同 role 不同权限
CREATE TABLE IF NOT EXISTS storage_access_policy (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid   TEXT NOT NULL,
    access_mode     TEXT NOT NULL DEFAULT 'PUBLIC',
    role_name       TEXT,          -- 当 access_mode=ROLE 时必填
    allow_download  INTEGER NOT NULL DEFAULT 1,
    allow_preview   INTEGER NOT NULL DEFAULT 1,
    allow_update    INTEGER NOT NULL DEFAULT 0,
    allow_delete    INTEGER NOT NULL DEFAULT 0,
    allow_share     INTEGER NOT NULL DEFAULT 0,
    expire_time     DATETIME,
    create_time     DATETIME,
    update_time     DATETIME,
    create_user     TEXT,
    update_user     TEXT
);
CREATE INDEX IF NOT EXISTS idx_policy_resource ON storage_access_policy(resource_uuid);
CREATE INDEX IF NOT EXISTS idx_policy_role ON storage_access_policy(resource_uuid, role_name);

-- 分享链接表
CREATE TABLE IF NOT EXISTS storage_resource_share (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    share_token     TEXT NOT NULL UNIQUE,
    resource_uuid   TEXT NOT NULL,
    expire_seconds  INTEGER NOT NULL DEFAULT 86400,
    expire_time     DATETIME NOT NULL,
    creator_id      TEXT,
    create_time     DATETIME,
    update_time     DATETIME,
    create_user     TEXT,
    update_user     TEXT
);
CREATE INDEX IF NOT EXISTS idx_share_token ON storage_resource_share(share_token);
CREATE INDEX IF NOT EXISTS idx_share_resource ON storage_resource_share(resource_uuid);

-- 访问日志表
CREATE TABLE IF NOT EXISTS storage_access_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid   TEXT,
    access_type     TEXT NOT NULL,
    access_detail   TEXT,
    operator_id     TEXT,
    operator_roles  TEXT,
    client_ip       TEXT,
    user_agent      TEXT,
    result          TEXT NOT NULL DEFAULT 'SUCCESS',
    reason          TEXT,
    duration_ms     INTEGER DEFAULT 0,
    create_time     DATETIME,
    update_time     DATETIME,
    create_user     TEXT,
    update_user     TEXT
);
CREATE INDEX IF NOT EXISTS idx_access_log_resource ON storage_access_log(resource_uuid);
CREATE INDEX IF NOT EXISTS idx_access_log_time ON storage_access_log(create_time);