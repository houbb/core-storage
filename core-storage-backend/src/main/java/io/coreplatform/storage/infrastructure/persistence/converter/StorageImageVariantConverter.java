package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.ImageVariant;
import io.coreplatform.storage.application.domain.enums.Variant;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageImageVariantEntity;

/**
 * Entity ↔ Domain 双向转换（Variant 枚举 ↔ 字符串）。
 */
public final class StorageImageVariantConverter {

    private StorageImageVariantConverter() {
    }

    public static ImageVariant toDomain(StorageImageVariantEntity e) {
        if (e == null) return null;
        ImageVariant d = new ImageVariant();
        d.setId(e.getId());
        d.setImageUuid(e.getImageUuid());
        d.setVariantName(safeEnum(Variant.class, e.getVariantName(), Variant.ORIGINAL));
        d.setMetadataUuid(e.getMetadataUuid());
        d.setWidth(e.getWidth() != null ? e.getWidth() : 0);
        d.setHeight(e.getHeight() != null ? e.getHeight() : 0);
        d.setFormat(e.getFormat());
        d.setFileSize(e.getFileSize() != null ? e.getFileSize() : 0);
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }

    public static StorageImageVariantEntity toEntity(ImageVariant d) {
        if (d == null) return null;
        StorageImageVariantEntity e = new StorageImageVariantEntity();
        e.setId(d.getId());
        e.setImageUuid(d.getImageUuid());
        e.setVariantName(d.getVariantName() != null ? d.getVariantName().name() : null);
        e.setMetadataUuid(d.getMetadataUuid());
        e.setWidth(d.getWidth());
        e.setHeight(d.getHeight());
        e.setFormat(d.getFormat());
        e.setFileSize(d.getFileSize());
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
