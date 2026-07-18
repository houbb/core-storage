package io.coreplatform.storage.application.domain.enums;

/**
 * 审计操作类型 — 统一审计所有资源操作。
 */
public enum AuditAction {

    UPLOAD,
    DOWNLOAD,
    PREVIEW,
    DELETE,
    UPDATE,
    PUBLISH,
    SHARE,
    MOVE,
    ARCHIVE,
    RESTORE
}