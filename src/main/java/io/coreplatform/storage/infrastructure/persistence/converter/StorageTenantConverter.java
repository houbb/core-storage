package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageTenant;
import io.coreplatform.storage.application.domain.enums.TenantStatus;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageTenantEntity;

/**
 * StorageTenant Entity ↔ Domain 转换器。
 */
public final class StorageTenantConverter {

    private StorageTenantConverter() {}

    public static StorageTenant toDomain(StorageTenantEntity e) {
        if (e == null) return null;
        StorageTenant d = new StorageTenant();
        d.setTenantId(e.getTenantId());
        d.setTenantName(e.getTenantName());
        d.setStatus(safeEnum(TenantStatus.class, e.getStatus(), TenantStatus.ACTIVE));
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        return d;
    }

    public static StorageTenantEntity toEntity(StorageTenant d) {
        if (d == null) return null;
        StorageTenantEntity e = new StorageTenantEntity();
        e.setTenantId(d.getTenantId());
        e.setTenantName(d.getTenantName());
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
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