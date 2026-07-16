package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * 配额响应 DTO。
 */
public class StorageQuotaResponse {

    private Long id;
    private String tenantId;
    private String resourceType;
    private long limitSize;
    private long usedSize;
    private long remainingSize;
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

    public long getRemainingSize() { return remainingSize; }
    public void setRemainingSize(long remainingSize) { this.remainingSize = remainingSize; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}