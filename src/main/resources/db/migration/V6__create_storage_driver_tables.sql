-- ============================================================
-- P5 Driver Runtime: 存储驱动运行时
-- ============================================================

-- 1. storage_driver：驱动程序注册表
CREATE TABLE IF NOT EXISTS storage_driver
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    driver_name   TEXT    NOT NULL UNIQUE,
    driver_type   TEXT    NOT NULL,
    version       TEXT,
    enabled       INTEGER NOT NULL DEFAULT 1,
    status        TEXT    NOT NULL DEFAULT 'RUNNING',
    health_status TEXT    NOT NULL DEFAULT 'UNKNOWN',
    create_time   DATETIME
);

CREATE INDEX IF NOT EXISTS idx_storage_driver_name ON storage_driver (driver_name);

-- 2. storage_profile：存储配置档案
CREATE TABLE IF NOT EXISTS storage_profile
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_name TEXT    NOT NULL UNIQUE,
    driver_name  TEXT    NOT NULL,
    is_default   INTEGER NOT NULL DEFAULT 0,
    create_time  DATETIME
);

CREATE INDEX IF NOT EXISTS idx_storage_profile_name ON storage_profile (profile_name);
CREATE INDEX IF NOT EXISTS idx_storage_profile_default ON storage_profile (is_default);

-- 3. storage_driver_blob：数据库驱动 BLOB 存储表
CREATE TABLE IF NOT EXISTS storage_driver_blob
(
    blob_id      TEXT PRIMARY KEY,
    storage_key  TEXT    NOT NULL UNIQUE,
    content      BLOB,
    size         INTEGER NOT NULL DEFAULT 0,
    content_type TEXT,
    create_time  DATETIME,
    update_time  DATETIME
);

CREATE INDEX IF NOT EXISTS idx_storage_driver_blob_key ON storage_driver_blob (storage_key);

-- 4. storage_resource 新增 profile_name（可为空，兼容已有数据）
ALTER TABLE storage_resource ADD COLUMN profile_name TEXT;

CREATE INDEX IF NOT EXISTS idx_resource_profile ON storage_resource (profile_name);
