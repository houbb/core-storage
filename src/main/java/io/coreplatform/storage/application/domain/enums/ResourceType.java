package io.coreplatform.storage.application.domain.enums;

/**
 * 统一资源类型 — 告诉"是什么"。
 * 业务系统不通过 MIME 判断，直接通过 ResourceType 判断。
 */
public enum ResourceType {

    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    PLUGIN,
    TEMPLATE,
    MODEL,
    MODEL_3D,
    BACKUP,
    EXPORT,
    ICON,
    FONT,
    DATASET,
    OTHER;

    /**
     * 根据 MIME 类型自动推断 ResourceType。
     * 如果无法匹配，返回 OTHER。
     */
    public static ResourceType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return OTHER;
        }
        String lower = mimeType.toLowerCase();
        if (lower.startsWith("image/")) return IMAGE;
        if (lower.startsWith("video/")) return VIDEO;
        if (lower.startsWith("audio/")) return AUDIO;
        if (lower.equals("application/pdf")
                || lower.contains("document")
                || lower.contains("spreadsheet")
                || lower.contains("presentation")
                || lower.contains("msword")
                || lower.contains("officedocument")) return DOCUMENT;
        if (lower.equals("application/zip")
                || lower.equals("application/x-tar")
                || lower.equals("application/gzip")
                || lower.equals("application/x-7z-compressed")
                || lower.equals("application/x-rar-compressed")) return ARCHIVE;
        if (lower.startsWith("font/")
                || lower.equals("application/vnd.ms-fontobject")
                || lower.contains("woff")) return FONT;
        if (lower.startsWith("text/") || lower.equals("application/json") || lower.equals("application/xml")) return DOCUMENT;
        return OTHER;
    }
}