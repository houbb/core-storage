package io.coreplatform.storage.application.domain.enums;

/**
 * 资源保留类型（Legal Hold）。
 * <p>
 * 当资源处于 Legal Hold 状态时，所有生命周期操作（删除、归档、移动）都会被禁止，
 * 直到 Hold 被解除。对标 S3 Object Lock、金融档案系统。
 */
public enum HoldType {

    /** 法律保留（如诉讼保全） */
    LEGAL,

    /** 审计保留（如财务审计期间） */
    AUDIT,

    /** 调查保留（如安全事件调查） */
    INVESTIGATION
}