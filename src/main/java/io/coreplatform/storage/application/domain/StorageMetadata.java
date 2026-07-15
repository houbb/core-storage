package io.coreplatform.storage.application.domain;

import java.time.LocalDateTime;

/**
 * 资源元数据领域对象 — 资源真实身份（Source of Truth）。
 */
public class StorageMetadata {

    private Long id;
    private String uuid;
    private String resourceName;
    private String originalName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String hashSha256;
    private String storageDriver;
    private String storageKey;
    private String relativePath;
    private String storageName;
    private String storageType;
    private String ownerType;
    private String ownerId;
    private String systemName;
    private String moduleName;
    private String tags;
    private String remark;
    private String status;
    private boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;

    // -- 引用数（非持久化字段，查询时填充） --
    private int referenceCount;

    public StorageMetadata() {
    }

    /** 文件是否可被正常访问 */
    public boolean isActive() {
        return !deleted && ("ACTIVE".equals(status) || "REFERENCED".equals(status));
    }

    /** 软删除标记 */
    public void markSoftDeleted() {
        this.deleted = true;
        this.status = "SOFT_DELETED";
        this.updateTime = LocalDateTime.now();
    }

    /** 状态迁移 */
    public void transitionTo(String newStatus) {
        this.status = newStatus;
        this.updateTime = LocalDateTime.now();
    }

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }

    public String getStorageDriver() { return storageDriver; }
    public void setStorageDriver(String storageDriver) { this.storageDriver = storageDriver; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public String getStorageName() { return storageName; }
    public void setStorageName(String storageName) { this.storageName = storageName; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }

    public int getReferenceCount() { return referenceCount; }
    public void setReferenceCount(int referenceCount) { this.referenceCount = referenceCount; }
}