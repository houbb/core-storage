package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.ReplicaRole;
import io.coreplatform.storage.application.domain.enums.ReplicaStatus;

import java.time.LocalDateTime;

/**
 * 存储副本领域对象 — 对应 storage_replica 表。
 * <p>
 * 设计原则：
 * <ul>
 *     <li>一个 Resource 可以拥有多个 Replica</li>
 *     <li>Replica 才是真正对应具体存储位置的实体</li>
 *     <li>业务永远只访问 Resource，不直接访问某一个副本</li>
 * </ul>
 */
public class StorageReplica {

    private Long id;
    private String resourceUuid;
    private String profileName;
    private String driverName;
    private ReplicaRole replicaRole;
    private ReplicaStatus replicaStatus;
    private Long version;
    private String checksum;
    private LocalDateTime syncTime;
    private LocalDateTime createTime;

    public StorageReplica() {
    }

    public static StorageReplica create(String resourceUuid, String profileName,
                                         String driverName, ReplicaRole replicaRole) {
        StorageReplica replica = new StorageReplica();
        replica.resourceUuid = resourceUuid;
        replica.profileName = profileName;
        replica.driverName = driverName;
        replica.replicaRole = replicaRole;
        replica.replicaStatus = ReplicaStatus.CREATING;
        replica.version = 1L;
        replica.createTime = LocalDateTime.now();
        return replica;
    }

    // -- getters & setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public ReplicaRole getReplicaRole() { return replicaRole; }
    public void setReplicaRole(ReplicaRole replicaRole) { this.replicaRole = replicaRole; }

    public ReplicaStatus getReplicaStatus() { return replicaStatus; }
    public void setReplicaStatus(ReplicaStatus replicaStatus) { this.replicaStatus = replicaStatus; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}