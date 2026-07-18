package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageImage;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageImageEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageImageConverter {

    private StorageImageConverter() {
    }

    public static StorageImage toDomain(StorageImageEntity e) {
        if (e == null) return null;
        StorageImage d = new StorageImage();
        d.setId(e.getId());
        d.setImageUuid(e.getImageUuid());
        d.setMetadataUuid(e.getMetadataUuid());
        d.setWidth(e.getWidth() != null ? e.getWidth() : 0);
        d.setHeight(e.getHeight() != null ? e.getHeight() : 0);
        d.setFormat(e.getFormat());
        d.setColorSpace(e.getColorSpace());
        d.setHasAlpha(e.getHasAlpha() != null && e.getHasAlpha() != 0);
        d.setOrientation(e.getOrientation() != null ? e.getOrientation() : 1);
        d.setDpi(e.getDpi() != null ? e.getDpi() : 72);
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }

    public static StorageImageEntity toEntity(StorageImage d) {
        if (d == null) return null;
        StorageImageEntity e = new StorageImageEntity();
        e.setId(d.getId());
        e.setImageUuid(d.getImageUuid());
        e.setMetadataUuid(d.getMetadataUuid());
        e.setWidth(d.getWidth());
        e.setHeight(d.getHeight());
        e.setFormat(d.getFormat());
        e.setColorSpace(d.getColorSpace());
        e.setHasAlpha(d.isHasAlpha() ? 1 : 0);
        e.setOrientation(d.getOrientation());
        e.setDpi(d.getDpi());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        e.setCreateUser(d.getCreateUser());
        e.setUpdateUser(d.getUpdateUser());
        return e;
    }
}
