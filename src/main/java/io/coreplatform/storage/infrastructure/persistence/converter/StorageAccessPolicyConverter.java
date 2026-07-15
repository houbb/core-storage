package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.*;
import io.coreplatform.storage.application.domain.enums.AccessMode;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageAccessPolicyEntity;

/**
 * StorageAccessPolicy Entity ↔ Domain 双向转换。
 */
public final class StorageAccessPolicyConverter {

    private StorageAccessPolicyConverter() {}

    public static StorageAccessPolicy toDomain(StorageAccessPolicyEntity e) {
        if (e == null) return null;
        StorageAccessPolicy d = new StorageAccessPolicy();
        d.setId(e.getId());
        d.setResourceUuid(e.getResourceUuid());
        d.setAccessMode(safeEnum(AccessMode.class, e.getAccessMode(), AccessMode.PUBLIC));
        d.setRoleName(e.getRoleName());
        d.setAllowDownload(e.getAllowDownload() != null && e.getAllowDownload() == 1);
        d.setAllowPreview(e.getAllowPreview() != null && e.getAllowPreview() == 1);
        d.setAllowUpdate(e.getAllowUpdate() != null && e.getAllowUpdate() == 1);
        d.setAllowDelete(e.getAllowDelete() != null && e.getAllowDelete() == 1);
        d.setAllowShare(e.getAllowShare() != null && e.getAllowShare() == 1);
        d.setExpireTime(e.getExpireTime());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }

    public static StorageAccessPolicyEntity toEntity(StorageAccessPolicy d) {
        if (d == null) return null;
        StorageAccessPolicyEntity e = new StorageAccessPolicyEntity();
        e.setId(d.getId());
        e.setResourceUuid(d.getResourceUuid());
        e.setAccessMode(d.getAccessMode() != null ? d.getAccessMode().name() : "PUBLIC");
        e.setRoleName(d.getRoleName());
        e.setAllowDownload(d.isAllowDownload() ? 1 : 0);
        e.setAllowPreview(d.isAllowPreview() ? 1 : 0);
        e.setAllowUpdate(d.isAllowUpdate() ? 1 : 0);
        e.setAllowDelete(d.isAllowDelete() ? 1 : 0);
        e.setAllowShare(d.isAllowShare() ? 1 : 0);
        e.setExpireTime(d.getExpireTime());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        e.setCreateUser(d.getCreateUser());
        e.setUpdateUser(d.getUpdateUser());
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