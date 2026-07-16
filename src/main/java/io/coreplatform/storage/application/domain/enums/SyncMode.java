package io.coreplatform.storage.application.domain.enums;

/**
 * 同步模式 — 控制上传后同步行为。
 */
public enum SyncMode {

    /** 同步 — 等待所有副本完成后才返回 */
    SYNC,

    /** 异步 — 立即返回，后台同步 */
    ASYNC,

    /** 手动 — 仅通过 API 触发 */
    MANUAL
}