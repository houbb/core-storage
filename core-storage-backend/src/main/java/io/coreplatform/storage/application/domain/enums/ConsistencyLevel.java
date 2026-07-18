package io.coreplatform.storage.application.domain.enums;

/**
 * 一致性级别 — 控制同步完成后的数据一致性保证。
 */
public enum ConsistencyLevel {

    /** 强一致 — 所有副本写入确认后才认为完成 */
    STRONG,

    /** 最终一致 — 允许短暂不一致，最终收敛 */
    EVENTUAL
}