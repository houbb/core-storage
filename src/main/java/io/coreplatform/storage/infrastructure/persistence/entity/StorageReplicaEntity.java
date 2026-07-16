package io.coreplatform.storage.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * 持久化实体 — 直接映射 storage_replica 表。
 */
public class StorageReplicaEntity {

    private Long id;
    private String resourceUuid;
    private String profileName;
    private String driverName;
    private String replicaRole;
    private String replicaStatus;
    private Long version;
    private String checksum;
    private LocalDateTime syncTime;
    private LocalDateTime createTime;

    public StorageReplicaEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getReplicaRole() { return replicaRole; }
    public void setReplicaRole(String replicaRole) { this.replicaRole = replicaRole; }

    public String getReplicaStatus() { return replicaStatus; }
    public void setReplicaStatus(String replicaStatus) { this.replicaStatus = replicaStatus; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}