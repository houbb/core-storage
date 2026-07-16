package io.coreplatform.storage.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * 配额实体 — 直接映射 storage_quota 表。
 */
public class StorageQuotaEntity {

    private Long id;
    private String tenantId;
    private String resourceType;
    private long limitSize;
    private long usedSize;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public long getLimitSize() { return limitSize; }
    public void setLimitSize(long limitSize) { this.limitSize = limitSize; }

    public long getUsedSize() { return usedSize; }
    public void setUsedSize(long usedSize) { this.usedSize = usedSize; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}