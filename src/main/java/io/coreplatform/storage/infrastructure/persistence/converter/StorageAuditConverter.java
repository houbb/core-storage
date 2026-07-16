package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageAudit;
import io.coreplatform.storage.application.domain.enums.AuditAction;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageAuditEntity;

/**
 * StorageAudit Entity ↔ Domain 转换器。
 */
public final class StorageAuditConverter {

    private StorageAuditConverter() {}

    public static StorageAudit toDomain(StorageAuditEntity e) {
        if (e == null) return null;
        StorageAudit d = new StorageAudit();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setResourceUuid(e.getResourceUuid());
        d.setOperatorId(e.getOperatorId());
        d.setAction(safeEnum(AuditAction.class, e.getAction(), AuditAction.DOWNLOAD));
        d.setTarget(e.getTarget());
        d.setResult(e.getResult());
        d.setDetail(e.getDetail());
        d.setClientIp(e.getClientIp());
        d.setCreateTime(e.getCreateTime());
        return d;
    }

    public static StorageAuditEntity toEntity(StorageAudit d) {
        if (d == null) return null;
        StorageAuditEntity e = new StorageAuditEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setResourceUuid(d.getResourceUuid());
        e.setOperatorId(d.getOperatorId());
        e.setAction(d.getAction() != null ? d.getAction().name() : null);
        e.setTarget(d.getTarget());
        e.setResult(d.getResult());
        e.setDetail(d.getDetail());
        e.setClientIp(d.getClientIp());
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