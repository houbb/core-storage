package io.coreplatform.storage.application.domain.enums;

/**
 * Version 生命周期状态。
 */
public enum VersionStatus {

    /** 草稿，尚未上传文件 */
    DRAFT,

    /** 已上传文件 */
    UPLOADED,

    /** 校验通过 */
    VALIDATED,

    /** 已发布 */
    PUBLISHED,

    /** 不推荐使用 */
    DEPRECATED,

    /** 已归档 */
    ARCHIVED,

    /** 已删除 */
    DELETED
}
