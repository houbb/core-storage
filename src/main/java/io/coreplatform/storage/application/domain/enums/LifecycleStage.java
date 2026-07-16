package io.coreplatform.storage.application.domain.enums;

/**
 * 资源生命周期阶段。
 * <p>
 * 资源不是永久存在的——它从 ACTIVE 开始，随着时间推移经历 WARM、COLD、ARCHIVED，
 * 最终进入 DELETED。这整个过程由 LifecyclePolicy 驱动，而不是业务代码驱动。
 */
public enum LifecycleStage {

    /** 活跃使用中 */
    ACTIVE,

    /** 较少访问（温数据） */
    WARM,

    /** 几乎不用（冷数据） */
    COLD,

    /** 已归档（不可直接修改） */
    ARCHIVED,

    /** 已删除（等待物理删除或 grace period 恢复） */
    DELETED
}