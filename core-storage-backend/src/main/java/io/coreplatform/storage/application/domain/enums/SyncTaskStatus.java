package io.coreplatform.storage.application.domain.enums;

/**
 * 同步任务状态 — 描述 SyncTask 的执行生命周期。
 */
public enum SyncTaskStatus {

    /** 等待执行 */
    PENDING,

    /** 执行中 */
    RUNNING,

    /** 已完成 */
    COMPLETED,

    /** 失败 */
    FAILED,

    /** 已取消 */
    CANCELLED
}