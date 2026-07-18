package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageQuota;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageQuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 配额管理服务 — 负责配额的设置、查询和上传前校验。
 */
@Service
public class StorageQuotaService {

    private static final Logger log = LoggerFactory.getLogger(StorageQuotaService.class);

    private final StorageQuotaRepository quotaRepo;

    public StorageQuotaService(StorageQuotaRepository quotaRepo) {
        this.quotaRepo = quotaRepo;
    }

    /**
     * 设置或更新租户的配额。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageQuota setQuota(String tenantId, String resourceType, long limitSize) {
        StorageQuota quota = quotaRepo.findByTenantIdAndType(tenantId, resourceType != null ? resourceType : "*")
                .orElse(StorageQuota.create(tenantId, resourceType != null ? resourceType : "*", limitSize));
        quota.setLimitSize(limitSize);
        quota = quotaRepo.save(quota);
        log.info("Quota updated: tenantId={}, resourceType={}, limitSize={}", tenantId, resourceType, limitSize);
        return quota;
    }

    /**
     * 设置或更新租户的全局配额（所有资源类型）。
     */
    public StorageQuota setGlobalQuota(String tenantId, long limitSize) {
        return setQuota(tenantId, "*", limitSize);
    }

    /**
     * 获取租户的配额（按资源类型），fallback 到全局配额。
     */
    public StorageQuota getQuota(String tenantId, String resourceType) {
        return quotaRepo.findByTenantIdAndType(tenantId, resourceType != null ? resourceType : "*")
                .orElseGet(() -> quotaRepo.findByTenantIdAndType(tenantId, "*")
                        .orElse(StorageQuota.create(tenantId, "*", 0)));
    }

    /** 获取租户所有配额。 */
    public List<StorageQuota> getQuotas(String tenantId) {
        return quotaRepo.findByTenantId(tenantId);
    }

    /** 获取全局所有配额。 */
    public List<StorageQuota> listAllQuotas() {
        return quotaRepo.listAll();
    }

    /**
     * 上传前校验：检查当前配额是否允许新增指定字节数。
     * 如果超限，抛出 QuotaExceededException。
     * 检查通过后，预占配额（增加 usedSize）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkAndReserve(String tenantId, long fileSize) {
        // 使用通配 resourceType 检查全局配额
        StorageQuota quota = quotaRepo.findByTenantIdAndType(tenantId, "*")
                .orElse(null);

        if (quota != null && quota.isExceeded(fileSize)) {
            throw new QuotaExceededException(
                    String.format("Quota exceeded for tenant %s: used=%d, limit=%d, additional=%d",
                            tenantId, quota.getUsedSize(), quota.getLimitSize(), fileSize));
        }

        // 预占配额
        quotaRepo.incrementUsed(tenantId, "*", fileSize);
        log.debug("Quota reserved: tenantId={}, delta={}", tenantId, fileSize);
    }

    /**
     * 释放已预占的配额（例如上传失败时回滚）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseQuota(String tenantId, long fileSize) {
        quotaRepo.incrementUsed(tenantId, "*", -fileSize);
        log.debug("Quota released: tenantId={}, delta={}", tenantId, fileSize);
    }

    // ─── inner exceptions ───

    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String msg) { super(msg); }
    }

    public static class QuotaNotFoundException extends RuntimeException {
        public QuotaNotFoundException(String msg) { super(msg); }
    }
}