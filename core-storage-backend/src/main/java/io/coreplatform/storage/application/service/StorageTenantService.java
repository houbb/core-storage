package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageTenant;
import io.coreplatform.storage.application.domain.enums.TenantStatus;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageTenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户管理服务 — 负责租户的 CRUD、激活/暂停等治理操作。
 */
@Service
public class StorageTenantService {

    private static final Logger log = LoggerFactory.getLogger(StorageTenantService.class);

    private final StorageTenantRepository tenantRepo;

    public StorageTenantService(StorageTenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageTenant createTenant(String tenantId, String tenantName) {
        if (tenantRepo.existsByTenantId(tenantId)) {
            throw new TenantAlreadyExistsException("Tenant already exists: " + tenantId);
        }
        StorageTenant tenant = StorageTenant.create(tenantId, tenantName);
        tenant = tenantRepo.save(tenant);
        log.info("Tenant created: tenantId={}, tenantName={}", tenantId, tenantName);
        return tenant;
    }

    public StorageTenant getTenant(String tenantId) {
        return tenantRepo.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));
    }

    public List<StorageTenant> listTenants() {
        return tenantRepo.listAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageTenant updateTenant(String tenantId, String tenantName, TenantStatus status) {
        StorageTenant tenant = getTenant(tenantId);
        if (tenantName != null) tenant.setTenantName(tenantName);
        if (status != null) tenant.setStatus(status);
        tenantRepo.update(tenant);
        log.info("Tenant updated: tenantId={}", tenantId);
        return tenant;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(String tenantId) {
        getTenant(tenantId); // validate exists
        tenantRepo.deleteByTenantId(tenantId);
        log.info("Tenant soft-deleted: tenantId={}", tenantId);
    }

    /** 验证租户存在且为活跃状态，否则抛出异常。 */
    public void validateActive(String tenantId) {
        StorageTenant tenant = getTenant(tenantId);
        if (!tenant.isActive()) {
            throw new TenantSuspendedException("Tenant is suspended or deleted: " + tenantId);
        }
    }

    // ─── inner exceptions ───

    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String msg) { super(msg); }
    }

    public static class TenantAlreadyExistsException extends RuntimeException {
        public TenantAlreadyExistsException(String msg) { super(msg); }
    }

    public static class TenantSuspendedException extends RuntimeException {
        public TenantSuspendedException(String msg) { super(msg); }
    }
}