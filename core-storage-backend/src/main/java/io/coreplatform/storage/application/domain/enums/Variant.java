package io.coreplatform.storage.application.domain.enums;

/**
 * Image Variant — 图片变体类型。
 * ORIGINAL 永远不可修改；所有处理都生成新 Variant。
 */
public enum Variant {

    /** 原始图片（不可修改） */
    ORIGINAL,

    /** 缩略图 200×200 */
    THUMBNAIL,

    /** 小图 480×? */
    SMALL,

    /** 中图 1024×? */
    MEDIUM,

    /** 大图 2048×? */
    LARGE,

    /** 优化过的 Web 版本 */
    WEB,

    /** WebP 格式 */
    WEBP,

    /** AVIF 格式（当前为 best-effort，编码器不可用时回退 WebP） */
    AVIF
}
