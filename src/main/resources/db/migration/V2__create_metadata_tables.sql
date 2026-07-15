-- P1 Metadata Runtime: 元数据中心 + 引用管理 + 轻量索引
-- storage_file 保留不变（P0 兼容），P1 新增三张表

-- ============================================================
-- 1. storage_metadata：资源元数据主表
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_metadata
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid           TEXT    NOT NULL UNIQUE,
    resource_name  TEXT,
    original_name  TEXT,
    extension      TEXT,
    mime_type      TEXT,
    file_size      BIGINT       DEFAULT 0,
    hash_sha256    TEXT,
    storage_driver TEXT         DEFAULT 'local',
    storage_key    TEXT,
    relative_path  TEXT         DEFAULT '',
    storage_name   TEXT,
    storage_type   TEXT         DEFAULT 'local',
    owner_type     TEXT,
    owner_id       TEXT,
    system_name    TEXT,
    module_name    TEXT,
    tags           TEXT,
    remark         TEXT,
    status         TEXT    NOT NULL DEFAULT 'UPLOADING',
    deleted        INTEGER NOT NULL DEFAULT 0,
    create_time    DATETIME,
    update_time    DATETIME,
    create_user    TEXT,
    update_user    TEXT
);

CREATE INDEX IF NOT EXISTS idx_metadata_uuid ON storage_metadata (uuid);
CREATE INDEX IF NOT EXISTS idx_metadata_status ON storage_metadata (status);
CREATE INDEX IF NOT EXISTS idx_metadata_deleted ON storage_metadata (deleted);
CREATE INDEX IF NOT EXISTS idx_metadata_hash ON storage_metadata (hash_sha256);
CREATE INDEX IF NOT EXISTS idx_metadata_owner ON storage_metadata (owner_type, owner_id);
CREATE INDEX IF NOT EXISTS idx_metadata_system_module ON storage_metadata (system_name, module_name);
CREATE INDEX IF NOT EXISTS idx_metadata_mime ON storage_metadata (mime_type);

-- ============================================================
-- 2. storage_reference：业务引用表
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_reference
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    metadata_uuid  TEXT    NOT NULL,
    system_name    TEXT,
    module_name    TEXT,
    business_type  TEXT,
    business_id    TEXT,
    create_time    DATETIME,
    update_time    DATETIME,
    create_user    TEXT,
    update_user    TEXT
);

CREATE INDEX IF NOT EXISTS idx_reference_metadata_uuid ON storage_reference (metadata_uuid);
CREATE INDEX IF NOT EXISTS idx_reference_business ON storage_reference (business_type, business_id);

-- ============================================================
-- 3. storage_metadata_index：轻量索引表（为 P2/P3/P8 预留）
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_metadata_index
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid  TEXT    NOT NULL,
    owner_type     TEXT,
    owner_id       TEXT,
    resource_type  TEXT,
    module_name    TEXT,
    tag            TEXT,
    status         TEXT,
    create_time    DATETIME,
    update_time    DATETIME,
    create_user    TEXT,
    update_user    TEXT
);

CREATE INDEX IF NOT EXISTS idx_meta_index_resource_uuid ON storage_metadata_index (resource_uuid);
CREATE INDEX IF NOT EXISTS idx_meta_index_owner ON storage_metadata_index (owner_type, owner_id);
CREATE INDEX IF NOT EXISTS idx_meta_index_module ON storage_metadata_index (module_name);
CREATE INDEX IF NOT EXISTS idx_meta_index_tag ON storage_metadata_index (tag);
CREATE INDEX IF NOT EXISTS idx_meta_index_status ON storage_metadata_index (status);
