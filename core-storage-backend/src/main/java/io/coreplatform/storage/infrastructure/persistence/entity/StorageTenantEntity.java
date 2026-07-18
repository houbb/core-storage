package io.coreplatform.storage.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * 租户实体 — 直接映射 storage_tenant 表。
 */
public class StorageTenantEntity {

    private String tenantId;
    private String tenantName;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}