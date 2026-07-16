package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.infrastructure.persistence.entity.LifecyclePolicyEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class LifecyclePolicyConverter {

    private LifecyclePolicyConverter() {
    }

    public static LifecyclePolicy toDomain(LifecyclePolicyEntity e) {
        if (e == null) return null;
        LifecyclePolicy d = new LifecyclePolicy();
        d.setId(e.getId());
        d.setPolicyName(e.getPolicyName());
        d.setResourceType(e.getResourceType());
        d.setCategory(e.getCategory());
        d.setActiveDays(e.getActiveDays() != null ? e.getActiveDays() : 0);
        d.setWarmDays(e.getWarmDays() != null ? e.getWarmDays() : 0);
        d.setColdDays(e.getColdDays() != null ? e.getColdDays() : 0);
        d.setArchiveDays(e.getArchiveDays() != null ? e.getArchiveDays() : 0);
        d.setDeleteDays(e.getDeleteDays() != null ? e.getDeleteDays() : 0);
        d.setEnabled(e.getEnabled() != null && e.getEnabled() == 1);
        d.setDescription(e.getDescription());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        return d;
    }

    public static LifecyclePolicyEntity toEntity(LifecyclePolicy d) {
        if (d == null) return null;
        LifecyclePolicyEntity e = new LifecyclePolicyEntity();
        e.setId(d.getId());
        e.setPolicyName(d.getPolicyName());
        e.setResourceType(d.getResourceType());
        e.setCategory(d.getCategory());
        e.setActiveDays(d.getActiveDays());
        e.setWarmDays(d.getWarmDays());
        e.setColdDays(d.getColdDays());
        e.setArchiveDays(d.getArchiveDays());
        e.setDeleteDays(d.getDeleteDays());
        e.setEnabled(d.isEnabled() ? 1 : 0);
        e.setDescription(d.getDescription());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        return e;
    }
}