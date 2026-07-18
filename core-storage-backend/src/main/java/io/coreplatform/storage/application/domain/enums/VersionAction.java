package io.coreplatform.storage.application.domain.enums;

/**
 * Version 操作类型，用于审计历史记录。
 */
public enum VersionAction {

    /** 版本创建 */
    CREATED,

    /** 版本发布 */
    PUBLISHED,

    /** 标记为不推荐 */
    DEPRECATED,

    /** 回滚到该版本 */
    ROLLBACK,

    /** 归档 */
    ARCHIVED,

    /** 删除 */
    DELETED
}
