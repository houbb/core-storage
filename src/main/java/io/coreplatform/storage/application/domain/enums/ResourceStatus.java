package io.coreplatform.storage.application.domain.enums;

/**
 * Resource 生命周期状态（5 态版本）。
 */
public enum ResourceStatus {

    /** 资源已创建，等待上传（本期预留，Wizard 模式使用） */
    CREATED,

    /** 上传中 */
    UPLOADING,

    /** 就绪（对应 Metadata ACTIVE） */
    READY,

    /** 被引用（对应 Metadata REFERENCED） */
    REFERENCED,

    /** 已删除（软删除，对应 Metadata SOFT_DELETED） */
    DELETED
}