CREATE TABLE IF NOT EXISTS storage_file
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid          TEXT    NOT NULL UNIQUE,
    original_name TEXT    NOT NULL,
    storage_name  TEXT    NOT NULL,
    extension     TEXT,
    mime_type     TEXT,
    size          BIGINT  NOT NULL DEFAULT 0,
    storage_type  TEXT    NOT NULL DEFAULT 'local',
    relative_path TEXT    NOT NULL DEFAULT '',
    hash          TEXT,
    status        TEXT    NOT NULL DEFAULT 'ACTIVE',
    deleted       INTEGER NOT NULL DEFAULT 0,
    create_time   DATETIME,
    update_time   DATETIME,
    create_user   TEXT,
    update_user   TEXT
);

CREATE INDEX IF NOT EXISTS idx_storage_file_uuid ON storage_file (uuid);
CREATE INDEX IF NOT EXISTS idx_storage_file_deleted ON storage_file (deleted);
CREATE INDEX IF NOT EXISTS idx_storage_file_status ON storage_file (status);
