package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * 区域响应 DTO。
 */
public class StorageRegionResponse {

    private String regionCode;
    private String regionName;
    private String endpoint;
    private String driverName;
    private LocalDateTime createTime;

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}