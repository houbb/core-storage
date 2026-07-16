package io.coreplatform.storage.application.domain;

import java.time.LocalDateTime;

/**
 * 配额领域对象 — 对应 storage_quota 表。
 * <p>
 * 提供 isExceeded(additionalBytes) 领域行为：判断新增字节后是否超限。
 */
public class StorageQuota {

    private Long id;
    private String tenantId;
    private String resourceType;
    private long limitSize;
    private long usedSize;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public StorageQuota() {}

    public static StorageQuota create(String tenantId, String resourceType, long limitSize) {
        StorageQuota q = new StorageQuota();
        q.setTenantId(tenantId);
        q.setResourceType(resourceType);
        q.setLimitSize(limitSize);
        q.setUsedSize(0);
        q.setCreateTime(LocalDateTime.now());
        q.setUpdateTime(LocalDateTime.now());
        return q;
    }

    /** 检查新增 bytes 后是否超出配额。limitSize=0 表示无限。 */
    public boolean isExceeded(long additionalBytes) {
        if (limitSize <= 0) return false;
        return usedSize + additionalBytes > limitSize;
    }

    /** 获取剩余可用字节。limitSize=0 表示无限，返回 Long.MAX_VALUE。 */
    public long remainingBytes() {
        if (limitSize <= 0) return Long.MAX_VALUE;
        return Math.max(0, limitSize - usedSize);
    }

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