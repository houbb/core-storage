-- P4 Image Runtime: 图片专用运行时
-- Image Runtime 是 Resource Runtime 的垂直扩展，不替代 Storage
-- 原图永远不可修改，所有处理生成新 Variant

-- ============================================================
-- 1. storage_image：图片元数据表
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_image
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    image_uuid      TEXT    NOT NULL UNIQUE,
    metadata_uuid   TEXT    NOT NULL,
    width           INTEGER NOT NULL DEFAULT 0,
    height          INTEGER NOT NULL DEFAULT 0,
    format          TEXT,
    color_space     TEXT,
    has_alpha       INTEGER NOT NULL DEFAULT 0,
    orientation     INTEGER NOT NULL DEFAULT 1,
    dpi             INTEGER NOT NULL DEFAULT 72,
    create_time     DATETIME,
    update_time     DATETIME,
    create_user     TEXT,
    update_user     TEXT
);

CREATE INDEX IF NOT EXISTS idx_storage_image_uuid ON storage_image (image_uuid);
CREATE INDEX IF NOT EXISTS idx_storage_image_metadata ON storage_image (metadata_uuid);

-- ============================================================
-- 2. storage_image_variant：变体追踪表
-- 每个 Variant 对应一个独立的 StorageFile（通过 metadata_uuid 关联）
-- ============================================================
CREATE TABLE IF NOT EXISTS storage_image_variant
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    image_uuid      TEXT    NOT NULL,
    variant_name    TEXT    NOT NULL,
    metadata_uuid   TEXT    NOT NULL,
    width           INTEGER NOT NULL DEFAULT 0,
    height          INTEGER NOT NULL DEFAULT 0,
    format          TEXT,
    file_size       BIGINT  NOT NULL DEFAULT 0,
    create_time     DATETIME,
    update_time     DATETIME,
    create_user     TEXT,
    update_user     TEXT
);

CREATE INDEX IF NOT EXISTS idx_image_variant_image ON storage_image_variant (image_uuid);
CREATE INDEX IF NOT EXISTS idx_image_variant_name ON storage_image_variant (image_uuid, variant_name);
CREATE INDEX IF NOT EXISTS idx_image_variant_metadata ON storage_image_variant (metadata_uuid);