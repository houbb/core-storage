package io.coreplatform.storage.application.domain.enums;

/**
 * 访问模式 — 决定资源的实际访问控制策略。
 * 与 Visibility（展示标签）分离，Visibility 仅做 UI 展示用。
 */
public enum AccessMode {

    /** 公开访问，任何人无需登录 */
    PUBLIC,

    /** 需要登录（有 user id 即可） */
    LOGIN,

    /** 仅资源所有者可访问 */
    OWNER,

    /** 根据角色（role_name）匹配策略 */
    ROLE,

    /** 通过分享 token 访问 */
    TOKEN,

    /** 通过 HMAC 签名 URL 访问 */
    SIGNED_URL,

    /** 系统内部访问，绕过所有检查 */
    SYSTEM
}