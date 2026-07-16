package io.coreplatform.storage.application.domain;

import java.time.LocalDateTime;

/**
 * 区域领域对象 — 对应 storage_region 表。
 */
public class StorageRegion {

    private String regionCode;
    private String regionName;
    private String endpoint;
    private String driverName;
    private LocalDateTime createTime;

    public StorageRegion() {}

    public static StorageRegion create(String regionCode, String regionName, String endpoint, String driverName) {
        StorageRegion r = new StorageRegion();
        r.setRegionCode(regionCode);
        r.setRegionName(regionName);
        r.setEndpoint(endpoint);
        r.setDriverName(driverName);
        r.setCreateTime(LocalDateTime.now());
        return r;
    }

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