package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.LifecycleTask;
import io.coreplatform.storage.application.domain.enums.LifecycleAction;
import io.coreplatform.storage.application.domain.enums.LifecycleTaskStatus;
import io.coreplatform.storage.infrastructure.persistence.entity.LifecycleTaskEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class LifecycleTaskConverter {

    private LifecycleTaskConverter() {
    }

    public static LifecycleTask toDomain(LifecycleTaskEntity e) {
        if (e == null) return null;
        LifecycleTask d = new LifecycleTask();
        d.setId(e.getId());
        d.setResourceUuid(e.getResourceUuid());
        d.setPolicyId(e.getPolicyId());
        d.setAction(safeEnum(LifecycleAction.class, e.getAction(), LifecycleAction.NOTHING));
        d.setTargetStage(e.getTargetStage());
        d.setStatus(safeEnum(LifecycleTaskStatus.class, e.getStatus(), LifecycleTaskStatus.PENDING));
        d.setExecuteTime(e.getExecuteTime());
        d.setFinishTime(e.getFinishTime());
        d.setErrorMessage(e.getErrorMessage());
        d.setRetryCount(e.getRetryCount() != null ? e.getRetryCount() : 0);
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        return d;
    }

    public static LifecycleTaskEntity toEntity(LifecycleTask d) {
        if (d == null) return null;
        LifecycleTaskEntity e = new LifecycleTaskEntity();
        e.setId(d.getId());
        e.setResourceUuid(d.getResourceUuid());
        e.setPolicyId(d.getPolicyId());
        e.setAction(d.getAction() != null ? d.getAction().name() : null);
        e.setTargetStage(d.getTargetStage());
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : "PENDING");
        e.setExecuteTime(d.getExecuteTime());
        e.setFinishTime(d.getFinishTime());
        e.setErrorMessage(d.getErrorMessage());
        e.setRetryCount(d.getRetryCount());
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