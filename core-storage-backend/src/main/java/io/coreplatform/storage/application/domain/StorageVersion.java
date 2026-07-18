package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.VersionStatus;

import java.time.LocalDateTime;

/**
 * 版本领域对象 — Resource 演化历史的载体。
 * <p>
 * 每个 Resource 可以有多个 Version，每个 Version 拥有独立的 Metadata，
 * 因此每个 Version 对应独立的物理文件。
 */
public class StorageVersion {

    private Long id;
    private String versionUuid;
    private String resourceUuid;
    private String metadataUuid;
    private String versionName;
    private int versionCode;
    private VersionStatus status;
    private boolean published;
    private boolean latest;
    private String checksum;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;

    public StorageVersion() {
    }

    public static StorageVersion create(String resourceUuid, String metadataUuid,
                                        int versionCode, String checksum) {
        StorageVersion v = new StorageVersion();
        v.versionUuid = java.util.UUID.randomUUID().toString().replace("-", "");
        v.resourceUuid = resourceUuid;
        v.metadataUuid = metadataUuid;
        v.versionCode = versionCode;
        v.versionName = "v" + versionCode;
        v.status = VersionStatus.DRAFT;
        v.published = false;
        v.latest = false;
        v.checksum = checksum;
        v.createTime = LocalDateTime.now();
        return v;
    }

    /** 标记为已发布 */
    public void markPublished() {
        this.status = VersionStatus.PUBLISHED;
        this.published = true;
        this.latest = true;
        this.publishTime = LocalDateTime.now();
    }

    /** 清除 latest 标记（发布新版本或回滚时） */
    public void clearLatest() {
        this.latest = false;
    }

    /** 是否可被发布 */
    public boolean isPublishable() {
        return status == VersionStatus.DRAFT
                || status == VersionStatus.UPLOADED
                || status == VersionStatus.VALIDATED;
    }

    /** 是否为活跃版本（已发布且未删除） */
    public boolean isActive() {
        return published && status != VersionStatus.DELETED;
    }

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionUuid() { return versionUuid; }
    public void setVersionUuid(String versionUuid) { this.versionUuid = versionUuid; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getMetadataUuid() { return metadataUuid; }
    public void setMetadataUuid(String metadataUuid) { this.metadataUuid = metadataUuid; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public int getVersionCode() { return versionCode; }
    public void setVersionCode(int versionCode) { this.versionCode = versionCode; }

    public VersionStatus getStatus() { return status; }
    public void setStatus(VersionStatus status) { this.status = status; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public boolean isLatest() { return latest; }
    public void setLatest(boolean latest) { this.latest = latest; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
}
