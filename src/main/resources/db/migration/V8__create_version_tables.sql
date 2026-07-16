-- ============================================================
-- P7 Version Runtime: 资源版本管理
-- ============================================================

-- 1. storage_version：版本表
-- Resource → Version → Metadata → Driver
CREATE TABLE IF NOT EXISTS storage_version
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    version_uuid    TEXT    NOT NULL UNIQUE,
    resource_uuid   TEXT    NOT NULL,
    metadata_uuid   TEXT    NOT NULL,
    version_name    TEXT,
    version_code    INTEGER NOT NULL DEFAULT 1,
    status          TEXT    NOT NULL DEFAULT 'DRAFT',
    published       INTEGER NOT NULL DEFAULT 0,
    latest          INTEGER NOT NULL DEFAULT 0,
    checksum        TEXT,
    create_time     DATETIME,
    publish_time    DATETIME
);

CREATE INDEX IF NOT EXISTS idx_version_uuid ON storage_version(version_uuid);
CREATE INDEX IF NOT EXISTS idx_version_resource ON storage_version(resource_uuid);
CREATE INDEX IF NOT EXISTS idx_version_metadata ON storage_version(metadata_uuid);
CREATE INDEX IF NOT EXISTS idx_version_latest ON storage_version(resource_uuid, latest);
CREATE INDEX IF NOT EXISTS idx_version_code ON storage_version(resource_uuid, version_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_version_resource_code ON storage_version(resource_uuid, version_code);

-- 2. storage_version_alias：版本别名表
CREATE TABLE IF NOT EXISTS storage_version_alias
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    version_uuid    TEXT    NOT NULL,
    resource_uuid   TEXT    NOT NULL,
    alias_name      TEXT    NOT NULL,
    create_time     DATETIME
);

CREATE INDEX IF NOT EXISTS idx_version_alias ON storage_version_alias(version_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS idx_alias_resource_name ON storage_version_alias(resource_uuid, alias_name);

-- 3. storage_version_history：版本操作历史表
CREATE TABLE IF NOT EXISTS storage_version_history
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    version_uuid    TEXT    NOT NULL,
    resource_uuid   TEXT    NOT NULL,
    action          TEXT    NOT NULL,
    previous_status TEXT,
    new_status      TEXT,
    operator_id     TEXT,
    remark          TEXT,
    create_time     DATETIME
);

CREATE INDEX IF NOT EXISTS idx_version_history ON storage_version_history(version_uuid);
CREATE INDEX IF NOT EXISTS idx_version_history_resource ON storage_version_history(resource_uuid);

-- 4. 回填已有数据：每个已有 Resource 生成 Version v1（PUBLISHED + latest）
INSERT OR IGNORE INTO storage_version (version_uuid, resource_uuid, metadata_uuid, version_name, version_code, status, published, latest, checksum, create_time, publish_time)
SELECT
    lower(hex(randomblob(16))),
    resource_uuid,
    metadata_uuid,
    'v1',
    1,
    'PUBLISHED',
    1,
    1,
    NULL,
    COALESCE(create_time, datetime('now')),
    COALESCE(create_time, datetime('now'))
FROM storage_resource
WHERE metadata_uuid IS NOT NULL AND metadata_uuid != '';
