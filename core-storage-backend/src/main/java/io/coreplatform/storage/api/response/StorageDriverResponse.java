package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 驱动运行时信息 API 响应。
 */
public class StorageDriverResponse {

    private Long id;
    private String driverName;
    private String driverType;
    private String version;
    private boolean enabled;
    private String status;
    private String healthStatus;
    private List<String> capabilities;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverType() { return driverType; }
    public void setDriverType(String driverType) { this.driverType = driverType; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
