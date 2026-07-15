package io.coreplatform.storage.application.domain.enums;

/**
 * 资源可见性。
 */
public enum Visibility {

    /** 所有人可见 */
    PUBLIC,

    /** 登录用户可见 */
    LOGIN,

    /** 仅 Owner 可见 */
    PRIVATE,

    /** 系统内部可见 */
    SYSTEM
}