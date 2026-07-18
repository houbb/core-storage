package io.coreplatform.storage.application.domain.enums;

/**
 * 驱动运行时状态 — 对应设计文档中的 Driver 生命周期。
 */
public enum DriverStatus {

    /** 已加载，尚未初始化 */
    LOADED,

    /** 正在初始化 */
    INITIALIZING,

    /** 健康检查中 */
    HEALTH_CHECK,

    /** 就绪，正常运行 */
    RUNNING,

    /** 正在停止 */
    STOPPING,

    /** 已停止 */
    STOPPED,

    /** 已禁用 */
    DISABLED,

    /** 异常状态 */
    ERROR
}
