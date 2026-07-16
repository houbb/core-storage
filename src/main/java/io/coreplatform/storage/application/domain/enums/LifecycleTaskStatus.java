package io.coreplatform.storage.application.domain.enums;

/**
 * 生命周期任务状态。
 * <p>
 * 所有生命周期操作都通过 Task 模型执行，保证可暂停、可恢复、可重试。
 */
public enum LifecycleTaskStatus {

    /** 等待执行 */
    PENDING,

    /** 执行中 */
    RUNNING,

    /** 执行完成 */
    COMPLETED,

    /** 执行失败 */
    FAILED,

    /** 已取消 */
    CANCELLED
}