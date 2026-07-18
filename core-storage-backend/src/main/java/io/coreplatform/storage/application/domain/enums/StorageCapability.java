package io.coreplatform.storage.application.domain.enums;

/**
 * 驱动能力声明枚举。
 * 不同 Driver 支持的能力不同，Runtime 根据此声明自动启用或隐藏相关功能。
 * 替代业务代码中的 if(driver instanceof S3Driver) 判断。
 */
public enum StorageCapability {

    /** 支持分片上传 */
    MULTIPART_UPLOAD,

    /** 支持对象版本管理 */
    VERSIONING,

    /** 支持预签名 URL */
    SIGNED_URL,

    /** 支持流式读写 */
    STREAMING,

    /** 支持事务性操作 */
    TRANSACTION,

    /** 支持生命周期策略 */
    LIFECYCLE
}
