package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.ResourceHold;
import io.coreplatform.storage.application.domain.enums.HoldType;
import io.coreplatform.storage.infrastructure.persistence.entity.ResourceHoldEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class ResourceHoldConverter {

    private ResourceHoldConverter() {
    }

    public static ResourceHold toDomain(ResourceHoldEntity e) {
        if (e == null) return null;
        ResourceHold d = new ResourceHold();
        d.setId(e.getId());
        d.setResourceUuid(e.getResourceUuid());
        d.setHoldType(safeEnum(HoldType.class, e.getHoldType(), HoldType.LEGAL));
        d.setReason(e.getReason());
        d.setOperatorId(e.getOperatorId());
        d.setExpireTime(e.getExpireTime());
        d.setReleased(e.getReleased() != null && e.getReleased() == 1);
        d.setReleasedTime(e.getReleasedTime());
        d.setReleaseOperatorId(e.getReleaseOperatorId());
        d.setCreateTime(e.getCreateTime());
        return d;
    }

    public static ResourceHoldEntity toEntity(ResourceHold d) {
        if (d == null) return null;
        ResourceHoldEntity e = new ResourceHoldEntity();
        e.setId(d.getId());
        e.setResourceUuid(d.getResourceUuid());
        e.setHoldType(d.getHoldType() != null ? d.getHoldType().name() : "LEGAL");
        e.setReason(d.getReason());
        e.setOperatorId(d.getOperatorId());
        e.setExpireTime(d.getExpireTime());
        e.setReleased(d.isReleased() ? 1 : 0);
        e.setReleasedTime(d.getReleasedTime());
        e.setReleaseOperatorId(d.getReleaseOperatorId());
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