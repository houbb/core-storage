package io.coreplatform.storage.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * 持久化实体 — 直接映射 storage_version 表。
 */
public class StorageVersionEntity {

    private Long id;
    private String versionUuid;
    private String resourceUuid;
    private String metadataUuid;
    private String versionName;
    private Integer versionCode;
    private String status;
    private Integer published;
    private Integer latest;
    private String checksum;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;

    public StorageVersionEntity() {
    }

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

    public Integer getVersionCode() { return versionCode; }
    public void setVersionCode(Integer versionCode) { this.versionCode = versionCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPublished() { return published; }
    public void setPublished(Integer published) { this.published = published; }

    public Integer getLatest() { return latest; }
    public void setLatest(Integer latest) { this.latest = latest; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
}
