package io.coreplatform.storage.application.domain.enums;

/**
 * 统一存储驱动类型枚举。
 * 每一个 Driver 实现对应一个 DriverType。
 */
public enum DriverType {

    LOCAL,
    DATABASE,
    MINIO,
    S3,
    OSS,
    COS,
    AZURE,
    NAS,
    FTP,
    CUSTOM
}
