package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.enums.AccessMode;
import io.coreplatform.storage.application.domain.enums.ResourceCategory;
import io.coreplatform.storage.application.domain.enums.ResourceStatus;
import io.coreplatform.storage.application.domain.enums.ResourceType;
import io.coreplatform.storage.application.domain.enums.Visibility;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageResourceEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageResourceConverter {

    private StorageResourceConverter() {
    }

    public static StorageResource toDomain(StorageResourceEntity e) {
        if (e == null) return null;
        StorageResource d = new StorageResource();
        d.setId(e.getId());
        d.setResourceUuid(e.getResourceUuid());
        d.setMetadataUuid(e.getMetadataUuid());
        d.setResourceName(e.getResourceName());
        d.setResourceType(safeEnum(ResourceType.class, e.getResourceType(), ResourceType.OTHER));
        d.setCategory(safeEnum(ResourceCategory.class, e.getCategory(), ResourceCategory.OTHER));
        d.setDescription(e.getDescription());
        d.setOwnerType(e.getOwnerType());
        d.setOwnerId(e.getOwnerId());
        d.setVisibility(safeEnum(Visibility.class, e.getVisibility(), Visibility.PUBLIC));
        d.setAccessMode(safeEnum(AccessMode.class, e.getAccessMode(), AccessMode.PUBLIC));
        d.setStatus(safeEnum(ResourceStatus.class, e.getStatus(), ResourceStatus.UPLOADING));
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }

    public static StorageResourceEntity toEntity(StorageResource d) {
        if (d == null) return null;
        StorageResourceEntity e = new StorageResourceEntity();
        e.setId(d.getId());
        e.setResourceUuid(d.getResourceUuid());
        e.setMetadataUuid(d.getMetadataUuid());
        e.setResourceName(d.getResourceName());
        e.setResourceType(d.getResourceType() != null ? d.getResourceType().name() : null);
        e.setCategory(d.getCategory() != null ? d.getCategory().name() : null);
        e.setDescription(d.getDescription());
        e.setOwnerType(d.getOwnerType());
        e.setOwnerId(d.getOwnerId());
        e.setVisibility(d.getVisibility() != null ? d.getVisibility().name() : "PUBLIC");
        e.setAccessMode(d.getAccessMode() != null ? d.getAccessMode().name() : "PUBLIC");
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : "UPLOADING");
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