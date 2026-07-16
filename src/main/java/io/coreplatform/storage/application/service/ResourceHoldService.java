package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.ResourceHold;
import io.coreplatform.storage.application.domain.enums.HoldType;
import io.coreplatform.storage.infrastructure.persistence.repository.ResourceHoldRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资源法律保留（Legal Hold）管理服务。
 * <p>
 * 当资源处于 Hold 状态时，所有生命周期操作（删除、归档、移动）都会被禁止。
 * 对标 S3 Object Lock、企业 ECM、金融档案系统。
 */
@Service
public class ResourceHoldService {

    private static final Logger log = LoggerFactory.getLogger(ResourceHoldService.class);

    private final ResourceHoldRepository holdRepo;
    private final StorageResourceRepository resourceRepo;

    public ResourceHoldService(ResourceHoldRepository holdRepo,
                                StorageResourceRepository resourceRepo) {
        this.holdRepo = holdRepo;
        this.resourceRepo = resourceRepo;
    }

    /**
     * 设置 Legal Hold。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResourceHold placeHold(String resourceUuid, String holdType, String reason,
                                   String operatorId, LocalDateTime expireTime) {
        // 验证资源存在
        resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new StorageResourceService.ResourceNotFoundException(
                        "Resource not found: uuid=" + resourceUuid));

        // 检查是否已有活跃 Hold
        if (holdRepo.hasActiveHold(resourceUuid)) {
            throw new HoldAlreadyExistsException("Resource already has an active hold: uuid=" + resourceUuid);
        }

        HoldType ht = safeEnum(holdType);

        ResourceHold hold = ResourceHold.create(resourceUuid, ht, reason, operatorId, expireTime);
        ResourceHold saved = holdRepo.save(hold);
        log.info("Hold placed: resourceUuid={}, type={}, operator={}", resourceUuid, holdType, operatorId);
        return saved;
    }

    /**
     * 解除 Legal Hold。
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseHold(String resourceUuid, String operatorId) {
        List<ResourceHold> activeHolds = holdRepo.findActiveByResourceUuid(resourceUuid);
        if (activeHolds.isEmpty()) {
            throw new HoldNotFoundException("No active hold found for resource: uuid=" + resourceUuid);
        }

        for (ResourceHold hold : activeHolds) {
            holdRepo.release(hold.getId(), operatorId);
            log.info("Hold released: id={}, resourceUuid={}, operator={}", hold.getId(), resourceUuid, operatorId);
        }
    }

    /**
     * 检查资源是否有活跃的 Hold（阻止生命周期操作）。
     */
    public boolean hasActiveHold(String resourceUuid) {
        return holdRepo.hasActiveHold(resourceUuid);
    }

    /**
     * 查询资源的 Hold 列表。
     */
    public List<ResourceHold> listHolds(String resourceUuid) {
        return holdRepo.findByResourceUuid(resourceUuid);
    }

    /**
     * 获取活跃 Hold 数量。
     */
    public int countActiveHolds() {
        return holdRepo.countActiveHolds();
    }

    private HoldType safeEnum(String value) {
        try {
            return HoldType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return HoldType.LEGAL;
        }
    }

    // ---- inner exceptions ----

    public static class HoldAlreadyExistsException extends RuntimeException {
        public HoldAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class HoldNotFoundException extends RuntimeException {
        public HoldNotFoundException(String message) {
            super(message);
        }
    }
}