package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageVersion;
import io.coreplatform.storage.application.domain.StorageVersionAlias;
import io.coreplatform.storage.application.domain.StorageVersionHistory;
import io.coreplatform.storage.application.domain.VersionNumber;
import io.coreplatform.storage.application.domain.enums.VersionAction;
import io.coreplatform.storage.application.domain.enums.VersionStatus;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageVersionAliasRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageVersionHistoryRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 版本管理桥接服务 — Resource ↔ Version ↔ Metadata 的核心枢纽。
 * <p>
 * 所有 Version 操作（创建/发布/回滚/别名/历史/比较）都经由此服务，
 * 承担 Resource 演化历史的全部读写职责。
 */
@Service
public class StorageVersionService {

    private static final Logger log = LoggerFactory.getLogger(StorageVersionService.class);

    private final StorageVersionRepository versionRepo;
    private final StorageVersionAliasRepository aliasRepo;
    private final StorageVersionHistoryRepository historyRepo;

    public StorageVersionService(StorageVersionRepository versionRepo,
                                  StorageVersionAliasRepository aliasRepo,
                                  StorageVersionHistoryRepository historyRepo) {
        this.versionRepo = versionRepo;
        this.aliasRepo = aliasRepo;
        this.historyRepo = historyRepo;
    }

    // ============================================================
    // 创建
    // ============================================================

    /**
     * 首次上传 → 创建 v1（自动发布为 latest）。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageVersion createInitialVersion(String resourceUuid, String metadataUuid,
                                                String checksum, String operatorId) {
        int nextCode = versionRepo.countByResourceUuid(resourceUuid) + 1;
        StorageVersion version = StorageVersion.create(resourceUuid, metadataUuid, nextCode, checksum);
        version.setStatus(VersionStatus.PUBLISHED);
        version.setPublished(true);
        version.setLatest(true);
        StorageVersion saved = versionRepo.save(version);
        recordHistory(saved.getVersionUuid(), resourceUuid, VersionAction.CREATED,
                null, saved.getStatus().name(), operatorId, "Initial version v" + nextCode);
        recordHistory(saved.getVersionUuid(), resourceUuid, VersionAction.PUBLISHED,
                null, saved.getStatus().name(), operatorId, "Auto-published on creation");
        log.info("Initial version created: versionUuid={}, resource={}, versionCode={}",
                saved.getVersionUuid(), resourceUuid, nextCode);
        return saved;
    }

    /**
     * 后续上传 → 创建新版本（不自动发布）。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageVersion createNewVersion(String resourceUuid, String metadataUuid,
                                            String checksum, String versionName, String operatorId) {
        int nextCode = versionRepo.countByResourceUuid(resourceUuid) + 1;
        StorageVersion version = StorageVersion.create(resourceUuid, metadataUuid, nextCode, checksum);
        if (versionName != null && !versionName.isBlank()) {
            version.setVersionName(versionName);
        }
        version.setStatus(VersionStatus.UPLOADED);
        StorageVersion saved = versionRepo.save(version);
        recordHistory(saved.getVersionUuid(), resourceUuid, VersionAction.CREATED,
                null, saved.getStatus().name(), operatorId, "New version " + saved.getVersionName());
        log.info("New version created: versionUuid={}, resource={}, versionCode={}",
                saved.getVersionUuid(), resourceUuid, nextCode);
        return saved;
    }

    // ============================================================
    // 发布 / 回滚
    // ============================================================

    /**
     * 发布版本 → 切换 Latest Pointer。
     * 旧 latest 标记为 false，新版本标记为 latest=true + published=true。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageVersion publish(String versionUuid, String operatorId) {
        StorageVersion version = versionRepo.findByVersionUuid(versionUuid)
                .orElseThrow(() -> new VersionNotFoundException("Version not found: uuid=" + versionUuid));

        if (!version.isPublishable()) {
            throw new InvalidVersionStateException(
                    "Version cannot be published: current status=" + version.getStatus());
        }

        String prevStatus = version.getStatus().name();

        // 清除当前 latest
        versionRepo.clearLatestForResource(version.getResourceUuid());

        // 设置新 latest + published
        versionRepo.setLatest(versionUuid, true);
        versionRepo.setPublished(versionUuid, true);
        versionRepo.updateStatus(versionUuid, VersionStatus.PUBLISHED.name());
        versionRepo.updatePublishTime(versionUuid);

        recordHistory(versionUuid, version.getResourceUuid(), VersionAction.PUBLISHED,
                prevStatus, VersionStatus.PUBLISHED.name(), operatorId, "Published " + version.getVersionName());

        log.info("Version published: versionUuid={}, resource={}, versionName={}",
                versionUuid, version.getResourceUuid(), version.getVersionName());
        return versionRepo.findByVersionUuid(versionUuid).orElse(version);
    }

    /**
     * 回滚 → 切换 Latest Pointer 到指定旧版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageVersion rollback(String resourceUuid, String targetVersionUuid, String operatorId) {
        StorageVersion target = versionRepo.findByVersionUuid(targetVersionUuid)
                .orElseThrow(() -> new VersionNotFoundException("Version not found: uuid=" + targetVersionUuid));

        if (!target.getResourceUuid().equals(resourceUuid)) {
            throw new InvalidVersionStateException(
                    "Version does not belong to resource: version=" + targetVersionUuid + ", resource=" + resourceUuid);
        }

        StorageVersion oldLatest = versionRepo.findLatestByResourceUuid(resourceUuid).orElse(null);

        // 清除所有 latest
        versionRepo.clearLatestForResource(resourceUuid);

        // 设置目标版本为 latest + published
        versionRepo.setLatest(targetVersionUuid, true);
        versionRepo.setPublished(targetVersionUuid, true);
        if (!target.isPublished()) {
            versionRepo.updateStatus(targetVersionUuid, VersionStatus.PUBLISHED.name());
        }

        recordHistory(targetVersionUuid, resourceUuid, VersionAction.ROLLBACK,
                oldLatest != null ? oldLatest.getStatus().name() : null,
                VersionStatus.PUBLISHED.name(), operatorId,
                "Rollback to " + target.getVersionName()
                        + (oldLatest != null ? " from " + oldLatest.getVersionName() : ""));

        log.info("Version rolled back: resource={}, oldLatest={}, newLatest={}",
                resourceUuid,
                oldLatest != null ? oldLatest.getVersionName() : "none",
                target.getVersionName());
        return versionRepo.findByVersionUuid(targetVersionUuid).orElse(target);
    }

    // ============================================================
    // 状态变更
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public StorageVersion deprecate(String versionUuid, String operatorId) {
        StorageVersion version = getVersion(versionUuid);
        String prevStatus = version.getStatus().name();
        versionRepo.updateStatus(versionUuid, VersionStatus.DEPRECATED.name());
        recordHistory(versionUuid, version.getResourceUuid(), VersionAction.DEPRECATED,
                prevStatus, VersionStatus.DEPRECATED.name(), operatorId, "Deprecated");
        log.info("Version deprecated: versionUuid={}", versionUuid);
        return versionRepo.findByVersionUuid(versionUuid).orElse(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageVersion archive(String versionUuid, String operatorId) {
        StorageVersion version = getVersion(versionUuid);
        String prevStatus = version.getStatus().name();
        versionRepo.updateStatus(versionUuid, VersionStatus.ARCHIVED.name());
        recordHistory(versionUuid, version.getResourceUuid(), VersionAction.ARCHIVED,
                prevStatus, VersionStatus.ARCHIVED.name(), operatorId, "Archived");
        log.info("Version archived: versionUuid={}", versionUuid);
        return versionRepo.findByVersionUuid(versionUuid).orElse(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(String versionUuid, String operatorId) {
        StorageVersion version = getVersion(versionUuid);
        if (version.isLatest()) {
            throw new InvalidVersionStateException("Cannot delete the latest version: uuid=" + versionUuid);
        }
        String prevStatus = version.getStatus().name();
        versionRepo.updateStatus(versionUuid, VersionStatus.DELETED.name());
        recordHistory(versionUuid, version.getResourceUuid(), VersionAction.DELETED,
                prevStatus, VersionStatus.DELETED.name(), operatorId, "Deleted");
        aliasRepo.deleteByVersionUuid(versionUuid);
        log.info("Version deleted: versionUuid={}", versionUuid);
    }

    // ============================================================
    // 别名管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public StorageVersionAlias setAlias(String versionUuid, String aliasName) {
        StorageVersion version = getVersion(versionUuid);
        // 同一 Resource 下别名唯一: 先删除旧别名
        aliasRepo.findByResourceUuidAndAlias(version.getResourceUuid(), aliasName)
                .ifPresent(existing -> aliasRepo.deleteByVersionUuid(existing.getVersionUuid()));
        StorageVersionAlias alias = StorageVersionAlias.create(versionUuid, version.getResourceUuid(), aliasName);
        StorageVersionAlias saved = aliasRepo.save(alias);
        log.info("Alias set: versionUuid={}, alias={}", versionUuid, aliasName);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeAlias(String resourceUuid, String aliasName) {
        aliasRepo.findByResourceUuidAndAlias(resourceUuid, aliasName)
                .orElseThrow(() -> new AliasNotFoundException(
                        "Alias not found: resource=" + resourceUuid + ", alias=" + aliasName));
        aliasRepo.deleteByResourceUuidAndAlias(resourceUuid, aliasName);
        log.info("Alias removed: resource={}, alias={}", resourceUuid, aliasName);
    }

    /** 根据别名解析版本（如 latest/stable/beta/lts → StorageVersion） */
    public StorageVersion resolveAlias(String resourceUuid, String aliasName) {
        // latest 别名直接查 latest 标记
        if ("latest".equalsIgnoreCase(aliasName)) {
            return versionRepo.findLatestByResourceUuid(resourceUuid)
                    .orElseThrow(() -> new VersionNotFoundException(
                            "No latest version found for resource: " + resourceUuid));
        }
        // 其他别名查 alias 表
        StorageVersionAlias alias = aliasRepo.findByResourceUuidAndAlias(resourceUuid, aliasName)
                .orElseThrow(() -> new AliasNotFoundException(
                        "Alias not found: resource=" + resourceUuid + ", alias=" + aliasName));
        return versionRepo.findByVersionUuid(alias.getVersionUuid())
                .orElseThrow(() -> new VersionNotFoundException(
                        "Version not found for alias: " + aliasName));
    }

    // ============================================================
    // 查询
    // ============================================================

    public List<StorageVersion> listVersions(String resourceUuid) {
        return versionRepo.findByResourceUuid(resourceUuid);
    }

    public StorageVersion getVersion(String versionUuid) {
        return versionRepo.findByVersionUuid(versionUuid)
                .orElseThrow(() -> new VersionNotFoundException("Version not found: uuid=" + versionUuid));
    }

    public StorageVersion getLatestVersion(String resourceUuid) {
        return versionRepo.findLatestByResourceUuid(resourceUuid)
                .orElseThrow(() -> new VersionNotFoundException(
                        "No latest version found for resource: " + resourceUuid));
    }

    /** 根据 metadata_uuid 反向查找版本（给 replication/access 等服务的桥接方法） */
    public StorageVersion getVersionByMetadataUuid(String metadataUuid) {
        return versionRepo.findByMetadataUuid(metadataUuid)
                .orElseThrow(() -> new VersionNotFoundException(
                        "Version not found for metadata: " + metadataUuid));
    }

    public List<StorageVersionAlias> listAliases(String resourceUuid) {
        return aliasRepo.findAllByResourceUuid(resourceUuid);
    }

    public List<StorageVersionHistory> getHistory(String versionUuid) {
        return historyRepo.findByVersionUuid(versionUuid);
    }

    public List<StorageVersionHistory> getResourceHistory(String resourceUuid, int page, int size) {
        int offset = Math.max(0, page - 1) * size;
        return historyRepo.findByResourceUuid(resourceUuid, offset, size);
    }

    public int countHistory(String resourceUuid) {
        return historyRepo.countByResourceUuid(resourceUuid);
    }

    // ============================================================
    // 比较
    // ============================================================

    /**
     * 比较两个版本 → 返回通用差异（hash、versionCode、status、时间）。
     */
    public VersionCompareResult compare(String versionUuid1, String versionUuid2) {
        StorageVersion v1 = getVersion(versionUuid1);
        StorageVersion v2 = getVersion(versionUuid2);

        Map<String, String[]> diffs = new LinkedHashMap<>();
        if (!Objects.equals(v1.getChecksum(), v2.getChecksum())) {
            diffs.put("checksum", new String[]{v1.getChecksum(), v2.getChecksum()});
        }
        if (v1.getVersionCode() != v2.getVersionCode()) {
            diffs.put("versionCode", new String[]{String.valueOf(v1.getVersionCode()), String.valueOf(v2.getVersionCode())});
        }
        if (!Objects.equals(v1.getStatus(), v2.getStatus())) {
            diffs.put("status", new String[]{v1.getStatus() != null ? v1.getStatus().name() : null,
                    v2.getStatus() != null ? v2.getStatus().name() : null});
        }
        if (!Objects.equals(v1.getPublishTime(), v2.getPublishTime())) {
            diffs.put("publishTime", new String[]{
                    v1.getPublishTime() != null ? v1.getPublishTime().toString() : null,
                    v2.getPublishTime() != null ? v2.getPublishTime().toString() : null});
        }

        return new VersionCompareResult(v1.getVersionUuid(), v1.getVersionName(), v1.getVersionCode(),
                v2.getVersionUuid(), v2.getVersionName(), v2.getVersionCode(),
                diffs, diffs.isEmpty());
    }

    // ============================================================
    // 私有
    // ============================================================

    private void recordHistory(String versionUuid, String resourceUuid,
                                VersionAction action, String prevStatus, String newStatus,
                                String operatorId, String remark) {
        StorageVersionHistory h = StorageVersionHistory.record(
                versionUuid, resourceUuid, action, prevStatus, newStatus, operatorId, remark);
        historyRepo.save(h);
    }

    // ============================================================
    // 内部异常
    // ============================================================

    public static class VersionNotFoundException extends RuntimeException {
        public VersionNotFoundException(String message) { super(message); }
    }

    public static class VersionAlreadyPublishedException extends RuntimeException {
        public VersionAlreadyPublishedException(String message) { super(message); }
    }

    public static class InvalidVersionStateException extends RuntimeException {
        public InvalidVersionStateException(String message) { super(message); }
    }

    public static class AliasNotFoundException extends RuntimeException {
        public AliasNotFoundException(String message) { super(message); }
    }

    public static class AliasAlreadyExistsException extends RuntimeException {
        public AliasAlreadyExistsException(String message) { super(message); }
    }

    // ============================================================
    // 返回值 DTO
    // ============================================================

    public static class VersionCompareResult {
        private final String versionUuid1;
        private final String versionName1;
        private final int versionCode1;
        private final String versionUuid2;
        private final String versionName2;
        private final int versionCode2;
        private final Map<String, String[]> differences;
        private final boolean identical;

        public VersionCompareResult(String versionUuid1, String versionName1, int versionCode1,
                                     String versionUuid2, String versionName2, int versionCode2,
                                     Map<String, String[]> differences, boolean identical) {
            this.versionUuid1 = versionUuid1;
            this.versionName1 = versionName1;
            this.versionCode1 = versionCode1;
            this.versionUuid2 = versionUuid2;
            this.versionName2 = versionName2;
            this.versionCode2 = versionCode2;
            this.differences = differences;
            this.identical = identical;
        }

        public String getVersionUuid1() { return versionUuid1; }
        public String getVersionName1() { return versionName1; }
        public int getVersionCode1() { return versionCode1; }
        public String getVersionUuid2() { return versionUuid2; }
        public String getVersionName2() { return versionName2; }
        public int getVersionCode2() { return versionCode2; }
        public Map<String, String[]> getDifferences() { return differences; }
        public boolean isIdentical() { return identical; }
    }
}
