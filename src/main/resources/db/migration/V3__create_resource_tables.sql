-- P2 Resource Runtime: 统一资源模型
-- Resource 是业务第一入口，Metadata 是内部存储对象

-- ============================================================
-- 1. storage_resource：统一资源主表
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_resource
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid  TEXT    NOT NULL UNIQUE,
    metadata_uuid  TEXT    NOT NULL,
    resource_name  TEXT,
    resource_type  TEXT,
    category       TEXT,
    description    TEXT,
    owner_type     TEXT,
    owner_id       TEXT,
    visibility     TEXT    NOT NULL DEFAULT 'PUBLIC',
    status         TEXT    NOT NULL DEFAULT 'UPLOADING',
    create_time    DATETIME,
    update_time    DATETIME,
    create_user    TEXT,
    update_user    TEXT
);

CREATE INDEX IF NOT EXISTS idx_resource_uuid ON storage_resource (resource_uuid);
CREATE INDEX IF NOT EXISTS idx_resource_metadata_uuid ON storage_resource (metadata_uuid);
CREATE INDEX IF NOT EXISTS idx_resource_type ON storage_resource (resource_type);
CREATE INDEX IF NOT EXISTS idx_resource_category ON storage_resource (category);
CREATE INDEX IF NOT EXISTS idx_resource_status ON storage_resource (status);
CREATE INDEX IF NOT EXISTS idx_resource_owner ON storage_resource (owner_type, owner_id);
CREATE INDEX IF NOT EXISTS idx_resource_visibility ON storage_resource (visibility);

-- ============================================================
-- 2. storage_resource_tag：标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_resource_tag
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid  TEXT    NOT NULL,
    tag_name       TEXT    NOT NULL,
    create_time    DATETIME,
    update_time    DATETIME,
    create_user    TEXT,
    update_user    TEXT
);

CREATE INDEX IF NOT EXISTS idx_resource_tag_uuid ON storage_resource_tag (resource_uuid);
CREATE INDEX IF NOT EXISTS idx_resource_tag_name ON storage_resource_tag (tag_name);

-- ============================================================
-- 3. storage_resource_property：属性扩展表
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_resource_property
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid  TEXT    NOT NULL,
    prop_key       TEXT    NOT NULL,
    prop_value     TEXT,
    create_time    DATETIME,
    update_time    DATETIME,
    create_user    TEXT,
    update_user    TEXT
);

CREATE INDEX IF NOT EXISTS idx_resource_prop_uuid ON storage_resource_property (resource_uuid);
CREATE INDEX IF NOT EXISTS idx_resource_prop_key ON storage_resource_property (resource_uuid, prop_key);