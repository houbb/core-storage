package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageReplica;
import io.coreplatform.storage.application.domain.enums.ReplicaRole;
import io.coreplatform.storage.application.domain.enums.ReplicaStatus;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageReplicaEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageReplicaConverter {

    private StorageReplicaConverter() {
    }

    public static StorageReplica toDomain(StorageReplicaEntity e) {
        if (e == null) return null;
        StorageReplica d = new StorageReplica();
        d.setId(e.getId());
        d.setResourceUuid(e.getResourceUuid());
        d.setProfileName(e.getProfileName());
        d.setDriverName(e.getDriverName());
        d.setReplicaRole(safeEnum(ReplicaRole.class, e.getReplicaRole(), ReplicaRole.SECONDARY));
        d.setReplicaStatus(safeEnum(ReplicaStatus.class, e.getReplicaStatus(), ReplicaStatus.CREATING));
        d.setVersion(e.getVersion());
        d.setChecksum(e.getChecksum());
        d.setSyncTime(e.getSyncTime());
        d.setCreateTime(e.getCreateTime());
        return d;
    }

    public static StorageReplicaEntity toEntity(StorageReplica d) {
        if (d == null) return null;
        StorageReplicaEntity e = new StorageReplicaEntity();
        e.setId(d.getId());
        e.setResourceUuid(d.getResourceUuid());
        e.setProfileName(d.getProfileName());
        e.setDriverName(d.getDriverName());
        e.setReplicaRole(d.getReplicaRole() != null ? d.getReplicaRole().name() : "SECONDARY");
        e.setReplicaStatus(d.getReplicaStatus() != null ? d.getReplicaStatus().name() : "CREATING");
        e.setVersion(d.getVersion());
        e.setChecksum(d.getChecksum());
        e.setSyncTime(d.getSyncTime());
        e.setCreateTime(d.getCreateTime());
        return e;
    }

    private static <E extends Enum<E>> E safeEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}