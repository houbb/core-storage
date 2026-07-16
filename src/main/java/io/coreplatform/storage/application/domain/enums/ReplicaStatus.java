package io.coreplatform.storage.application.domain.enums;

/**
 * 副本状态 — 描述 Replica 的同步健康状态。
 */
public enum ReplicaStatus {

    /** 副本已创建，等待首次同步 */
    CREATING,

    /** 同步中 */
    SYNCING,

    /** 就绪 — 数据与 PRIMARY 一致 */
    READY,

    /** 同步失败 — 上次同步出错 */
    FAILED,

    /** 离线 — 目标存储不可达 */
    OFFLINE,

    /** 删除中 — 等待清理 */
    DELETING
}