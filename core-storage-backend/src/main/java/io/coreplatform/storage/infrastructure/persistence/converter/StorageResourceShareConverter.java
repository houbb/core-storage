package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageResourceShare;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageResourceShareEntity;

/**
 * StorageResourceShare Entity ↔ Domain 双向转换。
 */
public final class StorageResourceShareConverter {

    private StorageResourceShareConverter() {}

    public static StorageResourceShare toDomain(StorageResourceShareEntity e) {
        if (e == null) return null;
        StorageResourceShare d = new StorageResourceShare();
        d.setId(e.getId());
        d.setShareToken(e.getShareToken());
        d.setResourceUuid(e.getResourceUuid());
        d.setExpireSeconds(e.getExpireSeconds() != null ? e.getExpireSeconds() : 86400);
        d.setExpireTime(e.getExpireTime());
        d.setCreatorId(e.getCreatorId());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }

    public static StorageResourceShareEntity toEntity(StorageResourceShare d) {
        if (d == null) return null;
        StorageResourceShareEntity e = new StorageResourceShareEntity();
        e.setId(d.getId());
        e.setShareToken(d.getShareToken());
        e.setResourceUuid(d.getResourceUuid());
        e.setExpireSeconds(d.getExpireSeconds());
        e.setExpireTime(d.getExpireTime());
        e.setCreatorId(d.getCreatorId());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        e.setCreateUser(d.getCreateUser());
        e.setUpdateUser(d.getUpdateUser());
        return e;
    }
}