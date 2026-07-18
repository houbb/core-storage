package io.coreplatform.storage.application.domain.enums;

/**
 * 驱动健康状态。
 */
public enum DriverHealth {

    /** 健康 — 后端可达且正常响应 */
    HEALTHY,

    /** 降级 — 部分功能可用，但性能或容量接近上限 */
    DEGRADED,

    /** 不健康 — 后端不可达或无法正常操作 */
    UNHEALTHY,

    /** 未知 — 尚未执行健康检查 */
    UNKNOWN
}
