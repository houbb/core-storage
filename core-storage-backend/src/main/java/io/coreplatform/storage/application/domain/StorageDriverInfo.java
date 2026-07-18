package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.DriverHealth;
import io.coreplatform.storage.application.domain.enums.DriverStatus;
import io.coreplatform.storage.application.domain.enums.DriverType;

import java.time.LocalDateTime;

/**
 * 存储驱动运行时信息 — 对应 storage_driver 表。
 * 代表一个已注册的驱动实例及其运行时状态。
 */
public class StorageDriverInfo {

    private Long id;
    private String driverName;
    private DriverType driverType;
    private String version;
    private boolean enabled;
    private DriverStatus status;
    private DriverHealth healthStatus;
    private LocalDateTime createTime;

    public StorageDriverInfo() {
    }

    // -- static factory for initial registration --

    public static StorageDriverInfo create(String driverName, DriverType driverType, String version) {
        StorageDriverInfo info = new StorageDriverInfo();
        info.driverName = driverName;
        info.driverType = driverType;
        info.version = version;
        info.enabled = true;
        info.status = DriverStatus.RUNNING;
        info.healthStatus = DriverHealth.UNKNOWN;
        info.createTime = LocalDateTime.now();
        return info;
    }

    // -- getters & setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public DriverType getDriverType() { return driverType; }
    public void setDriverType(DriverType driverType) { this.driverType = driverType; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus status) { this.status = status; }

    public DriverHealth getHealthStatus() { return healthStatus; }
    public void setHealthStatus(DriverHealth healthStatus) { this.healthStatus = healthStatus; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
