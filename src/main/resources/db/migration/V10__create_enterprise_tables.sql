-- ============================================================
-- P9 Enterprise Resource Platform: 企业级治理能力
-- ============================================================

-- 1. storage_tenant：多租户表
CREATE TABLE IF NOT EXISTS storage_tenant
(
    tenant_id   TEXT PRIMARY KEY,
    tenant_name TEXT NOT NULL,
    status      TEXT NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME,
    update_time DATETIME
);

-- 2. storage_region：区域表
CREATE TABLE IF NOT EXISTS storage_region
(
    region_code TEXT PRIMARY KEY,
    region_name TEXT    NOT NULL,
    endpoint    TEXT,
    driver_name TEXT,
    create_time DATETIME
);

-- 3. storage_quota：配额表
CREATE TABLE IF NOT EXISTS storage_quota
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id     TEXT    NOT NULL,
    resource_type TEXT    NOT NULL DEFAULT '*',
    limit_size    BIGINT  NOT NULL DEFAULT 0,
    used_size     BIGINT  NOT NULL DEFAULT 0,
    create_time   DATETIME,
    update_time   DATETIME
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_quota_tenant_type ON storage_quota(tenant_id, resource_type);

-- 4. storage_audit：统一审计日志表
CREATE TABLE IF NOT EXISTS storage_audit
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id     TEXT,
    resource_uuid TEXT,
    operator_id   TEXT,
    action        TEXT    NOT NULL,
    target        TEXT,
    result        TEXT    NOT NULL DEFAULT 'SUCCESS',
    detail        TEXT,
    client_ip     TEXT,
    create_time   DATETIME
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant ON storage_audit(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON storage_audit(resource_uuid);
CREATE INDEX IF NOT EXISTS idx_audit_action ON storage_audit(action);
CREATE INDEX IF NOT EXISTS idx_audit_time ON storage_audit(create_time);

-- 5. storage_scan：内容扫描表
CREATE TABLE IF NOT EXISTS storage_scan
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_uuid  TEXT    NOT NULL,
    scan_type      TEXT    NOT NULL,
    status         TEXT    NOT NULL DEFAULT 'PENDING',
    result_message TEXT,
    scan_time      DATETIME,
    create_time    DATETIME
);

CREATE INDEX IF NOT EXISTS idx_scan_resource ON storage_scan(resource_uuid);
CREATE INDEX IF NOT EXISTS idx_scan_status ON storage_scan(status);

-- 6. 为已有表增加 tenant_id 列（全链路租户隔离）
ALTER TABLE storage_resource ADD COLUMN tenant_id TEXT NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_resource_tenant ON storage_resource(tenant_id);

ALTER TABLE storage_metadata ADD COLUMN tenant_id TEXT NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_metadata_tenant ON storage_metadata(tenant_id);

ALTER TABLE storage_access_log ADD COLUMN tenant_id TEXT NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_access_log_tenant ON storage_access_log(tenant_id);

-- 7. 插入默认租户和默认配额
INSERT OR IGNORE INTO storage_tenant (tenant_id, tenant_name, status, create_time, update_time)
VALUES ('default', 'Default Tenant', 'ACTIVE', datetime('now'), datetime('now'));

INSERT OR IGNORE INTO storage_quota (tenant_id, resource_type, limit_size, used_size, create_time, update_time)
VALUES ('default', '*', 107374182400, 0, datetime('now'), datetime('now'));