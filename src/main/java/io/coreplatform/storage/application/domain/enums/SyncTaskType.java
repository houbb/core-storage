package io.coreplatform.storage.application.domain.enums;

/**
 * 同步任务类型 — 描述 SyncTask 的操作类别。
 */
public enum SyncTaskType {

    /** 数据同步 — 从源副本同步到目标副本 */
    SYNC,

    /** 数据迁移 — 切换 PRIMARY 指向 */
    MIGRATE,

    /** 故障恢复 — 从 SECONDARY 恢复 PRIMARY */
    RECOVER,

    /** 校验 — 仅验证两个副本的一致性 */
    VERIFY
}