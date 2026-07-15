package io.coreplatform.storage.application.domain.enums;

/**
 * 资源分类 — 告诉"干什么"。
 * 同一个 ResourceType 可能有不同的 Category（如 IMAGE 可以是 Avatar 或 Banner）。
 */
public enum ResourceCategory {

    AVATAR,
    ATTACHMENT,
    PLUGIN,
    TEMPLATE,
    LOGO,
    BANNER,
    BACKUP,
    DATASET,
    PROMPT,
    MODEL,
    OTHER
}