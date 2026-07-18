package io.coreplatform.storage.application.domain.enums;

/**
 * 内容扫描状态。
 */
public enum ScanStatus {

    PENDING,
    SCANNING,
    CLEAN,
    INFECTED,
    BLOCKED
}