package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageAudit;
import io.coreplatform.storage.application.domain.enums.AuditAction;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 统一审计服务 — fire-and-forget 审计日志写入，异步搜索查询。
 */
@Service
public class StorageAuditService {

    private static final Logger log = LoggerFactory.getLogger(StorageAuditService.class);

    private final StorageAuditRepository auditRepo;

    public StorageAuditService(StorageAuditRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    /**
     * 记录一条审计日志（fire-and-forget，不抛异常阻塞主流程）。
     */
    public void log(String tenantId, String resourceUuid, String operatorId,
                    AuditAction action, String target, String clientIp) {
        try {
            StorageAudit audit = StorageAudit.create(tenantId, resourceUuid, operatorId, action, target, clientIp);
            auditRepo.save(audit);
        } catch (Exception e) {
            log.warn("Failed to write audit log: action={}, resourceUuid={}, error={}",
                    action, resourceUuid, e.getMessage());
        }
    }

    /**
     * 记录一条带详情的审计日志。
     */
    public void logDetail(String tenantId, String resourceUuid, String operatorId,
                           AuditAction action, String target, String result, String detail, String clientIp) {
        try {
            StorageAudit audit = new StorageAudit();
            audit.setTenantId(tenantId);
            audit.setResourceUuid(resourceUuid);
            audit.setOperatorId(operatorId);
            audit.setAction(action);
            audit.setTarget(target);
            audit.setResult(result);
            audit.setDetail(detail);
            audit.setClientIp(clientIp);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            audit.setCreateTime(now);
            auditRepo.save(audit);
        } catch (Exception e) {
            log.warn("Failed to write audit log: action={}, resourceUuid={}, error={}",
                    action, resourceUuid, e.getMessage());
        }
    }

    public StorageAudit getAudit(Long id) {
        return auditRepo.findById(id)
                .orElseThrow(() -> new AuditNotFoundException("Audit not found: id=" + id));
    }

    /** 多条件搜索审计日志。 */
    public List<StorageAudit> search(String tenantId, String resourceUuid, String action,
                                      String operatorId, int page, int size) {
        int offset = Math.max(0, page - 1) * size;
        return auditRepo.search(tenantId, resourceUuid, action, operatorId, offset, size);
    }

    /** 搜索总数。 */
    public int countSearch(String tenantId, String resourceUuid, String action, String operatorId) {
        return auditRepo.countSearch(tenantId, resourceUuid, action, operatorId);
    }

    // ─── inner exceptions ───

    public static class AuditNotFoundException extends RuntimeException {
        public AuditNotFoundException(String msg) { super(msg); }
    }
}