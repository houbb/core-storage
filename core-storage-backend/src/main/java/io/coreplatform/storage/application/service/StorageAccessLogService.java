package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageAccessLog;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageAccessLogEntity;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageAccessLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 访问日志服务 — 异步写入，不阻塞主流程。
 */
@Service
public class StorageAccessLogService {

    private static final Logger log = LoggerFactory.getLogger(StorageAccessLogService.class);

    private final StorageAccessLogRepository accessLogRepo;

    public StorageAccessLogService(StorageAccessLogRepository accessLogRepo) {
        this.accessLogRepo = accessLogRepo;
    }

    /**
     * 异步记录一次访问。
     */
    @Async
    public void log(StorageAccessLog domain) {
        if (domain == null) return;
        try {
            StorageAccessLogEntity entity = new StorageAccessLogEntity();
            entity.setResourceUuid(domain.getResourceUuid());
            entity.setAccessType(domain.getAccessType());
            entity.setAccessDetail(domain.getAccessDetail());
            entity.setOperatorId(domain.getOperatorId());
            entity.setOperatorRoles(domain.getOperatorRoles());
            entity.setClientIp(domain.getClientIp());
            entity.setUserAgent(domain.getUserAgent());
            entity.setResult(domain.getResult() != null ? domain.getResult() : "SUCCESS");
            entity.setReason(domain.getReason());
            entity.setDurationMs(domain.getDurationMs() != null ? domain.getDurationMs() : 0);
            entity.setCreateUser(domain.getOperatorId());

            accessLogRepo.save(entity);
        } catch (Exception e) {
            // 审计日志失败不能影响主流程
            log.warn("Failed to write access log: {}", e.getMessage());
        }
    }
}