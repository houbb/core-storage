package io.coreplatform.storage.application.domain;

/**
 * 版本号工具类 — 将语义版本号转为可排序整数。
 * <p>
 * 支持格式：1, 1.0, 1.0.1, 2.0.0, 2026.07
 * <p>
 * 内部编码规则：major * 1,000,000 + minor * 1,000 + patch。
 * 日期格式（yyyy.MM）直接使用年份 * 100 + 月份。
 */
public final class VersionNumber {

    private VersionNumber() {
    }

    /** 将版本号字符串转为可排序整数 */
    public static int parse(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }

        String trimmed = version.trim().toLowerCase();

        // latest / stable 等别名没有数值，返回 max
        if ("latest".equals(trimmed) || "stable".equals(trimmed)
                || "beta".equals(trimmed) || "preview".equals(trimmed)
                || "lts".equals(trimmed)) {
            return Integer.MAX_VALUE;
        }

        // 日期格式：yyyy.MM → yyyy * 100 + MM
        if (trimmed.contains(".") && trimmed.length() == 7) {
            String[] parts = trimmed.split("\\.");
            if (parts.length == 2) {
                try {
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    if (y >= 2000 && y <= 2099 && m >= 1 && m <= 12) {
                        return y * 100 + m;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // semver 风格：M, M.m, M.m.p
        String[] parts = trimmed.split("\\.");
        int[] nums = new int[]{0, 0, 0};
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                nums[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return nums[0] * 1_000_000 + nums[1] * 1_000 + nums[2];
    }

    /** 将 versionCode 格式化为显示字符串 */
    public static String format(int versionCode) {
        if (versionCode <= 0) return "v0";
        int major = versionCode / 1_000_000;
        int minor = (versionCode % 1_000_000) / 1_000;
        int patch = versionCode % 1_000;
        if (patch > 0) return "v" + major + "." + minor + "." + patch;
        if (minor > 0) return "v" + major + "." + minor;
        return "v" + major;
    }

    /** 计算下一个 versionCode（增量 1） */
    public static int nextVersion(int currentCode) {
        return currentCode + 1;
    }
}
