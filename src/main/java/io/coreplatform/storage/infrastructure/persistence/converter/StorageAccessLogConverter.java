package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.infrastructure.persistence.entity.StorageAccessLogEntity;
import io.coreplatform.storage.application.domain.*;

/**
 * StorageAccessLog Entity → Domain 转换。
 * AccessLog 是只写不读的审计数据，无需 toEntity。
 */
public final class StorageAccessLogConverter {

    private StorageAccessLogConverter() {}

    public static StorageAccessLog toDomain(StorageAccessLogEntity e) {
        if (e == null) return null;
        StorageAccessLog d = new StorageAccessLog();
        d.setId(e.getId());
        d.setResourceUuid(e.getResourceUuid());
        d.setAccessType(e.getAccessType());
        d.setAccessDetail(e.getAccessDetail());
        d.setOperatorId(e.getOperatorId());
        d.setOperatorRoles(e.getOperatorRoles());
        d.setClientIp(e.getClientIp());
        d.setUserAgent(e.getUserAgent());
        d.setResult(e.getResult());
        d.setReason(e.getReason());
        d.setDurationMs(e.getDurationMs());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }
}