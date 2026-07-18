package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.TenantStatus;

import java.time.LocalDateTime;

/**
 * 租户领域对象 — 对应 storage_tenant 表。
 */
public class StorageTenant {

    private String tenantId;
    private String tenantName;
    private TenantStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public StorageTenant() {}

    public static StorageTenant create(String tenantId, String tenantName) {
        StorageTenant t = new StorageTenant();
        t.setTenantId(tenantId);
        t.setTenantName(tenantName);
        t.setStatus(TenantStatus.ACTIVE);
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        return t;
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}