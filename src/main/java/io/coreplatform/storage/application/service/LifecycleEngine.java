package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.application.domain.LifecycleTask;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.enums.LifecycleAction;
import io.coreplatform.storage.application.domain.enums.LifecycleStage;
import io.coreplatform.storage.application.domain.enums.LifecycleTaskStatus;
import io.coreplatform.storage.application.domain.enums.ResourceStatus;
import io.coreplatform.storage.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 生命周期引擎 — 核心状态机。
 * <p>
 * 负责：
 * 1. 评估单个资源是否需要生命周期阶段转换
 * 2. 执行生命周期任务（状态转换、归档、删除）
 * 3. 引用保护检查
 * 4. Hold 保护检查
 */
@Component
public class LifecycleEngine {

    private static final Logger log = LoggerFactory.getLogger(LifecycleEngine.class);

    private final LifecyclePolicyRepository policyRepo;
    private final LifecycleTaskRepository taskRepo;
    private final StorageResourceRepository resourceRepo;
    private final StorageReferenceRepository referenceRepo;
    private final ResourceHoldRepository holdRepo;

    public LifecycleEngine(LifecyclePolicyRepository policyRepo,
                            LifecycleTaskRepository taskRepo,
                            StorageResourceRepository resourceRepo,
                            StorageReferenceRepository referenceRepo,
                            ResourceHoldRepository holdRepo) {
        this.policyRepo = policyRepo;
        this.taskRepo = taskRepo;
        this.resourceRepo = resourceRepo;
        this.referenceRepo = referenceRepo;
        this.holdRepo = holdRepo;
    }

    /**
     * 评估单个资源是否需要生命周期转换。
     * 如果需要，创建 PENDING 任务。
     *
     * @return 创建的任务，如果不需要转换则返回 null
     */
    public LifecycleTask evaluateResource(StorageResource resource) {
        String resourceUuid = resource.getResourceUuid();

        // 1. 查找匹配策略
        LifecyclePolicy policy = findPolicy(resource);
        if (policy == null) {
            return null; // 无策略，永久保留
        }

        // 2. 检查是否已有 PENDING/RUNNING 任务
        if (taskRepo.findActiveByResourceUuid(resourceUuid).isPresent()) {
            return null; // 已有任务在执行中
        }

        // 3. 检查 Hold 状态
        if (holdRepo.hasActiveHold(resourceUuid)) {
            return null; // 被 Hold 保护
        }

        // 4. 计算目标阶段
        long daysSinceCreation = ChronoUnit.DAYS.between(resource.getCreateTime(), LocalDateTime.now());
        String targetStageName = policy.targetStage(daysSinceCreation);
        LifecycleStage targetStage = LifecycleStage.valueOf(targetStageName);

        LifecycleStage currentStage = resource.getLifecycleStage();
        if (currentStage == null) {
            currentStage = LifecycleStage.ACTIVE;
        }

        // 5. 判断是否需要转换
        if (!needsTransition(currentStage, targetStage)) {
            return null;
        }

        // 6. 确定 Action
        LifecycleAction action = determineAction(currentStage, targetStage);

        // 7. 创建任务
        LifecycleTask task = LifecycleTask.create(resourceUuid, policy.getId(), action, targetStage.name());
        LifecycleTask saved = taskRepo.save(task);

        log.info("Lifecycle task created: resourceUuid={}, current={}, target={}, action={}",
                resourceUuid, currentStage, targetStage, action);
        return saved;
    }

    /**
     * 执行一个生命周期任务。
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeTask(LifecycleTask task) {
        try {
            // 标记 RUNNING
            taskRepo.updateStatus(task.getId(), LifecycleTaskStatus.RUNNING.name());

            String resourceUuid = task.getResourceUuid();

            // 执行前再次检查 Hold
            if (holdRepo.hasActiveHold(resourceUuid)) {
                throw new IllegalStateException("Resource is under active hold: uuid=" + resourceUuid);
            }

            LifecycleAction action = task.getAction();

            switch (action) {
                case NOTHING -> {
                    // 无需操作
                    taskRepo.markCompleted(task.getId());
                }
                case MOVE, ARCHIVE -> {
                    // 引用保护检查（归档和删除前）
                    checkReferences(resourceUuid);
                    resourceRepo.updateLifecycleStage(resourceUuid, task.getTargetStage());
                    taskRepo.markCompleted(task.getId());
                    log.info("Lifecycle stage transitioned: resourceUuid={}, stage={}", resourceUuid, task.getTargetStage());
                }
                case DELETE -> {
                    // 引用保护 + 两阶段删除
                    checkReferences(resourceUuid);
                    // Phase 1: 软删除
                    resourceRepo.updateLifecycleStage(resourceUuid, LifecycleStage.DELETED.name());
                    resourceRepo.updateStatus(resourceUuid, ResourceStatus.DELETED.name());
                    taskRepo.markCompleted(task.getId());
                    log.info("Resource entered deletion lifecycle: resourceUuid={}", resourceUuid);
                }
                case VERIFY -> {
                    // 校验（本期仅标记完成，未来实现校验逻辑）
                    taskRepo.markCompleted(task.getId());
                    log.info("Lifecycle verify completed: resourceUuid={}", resourceUuid);
                }
                case FREEZE -> {
                    // 冻结（临时阻止操作）
                    taskRepo.markCompleted(task.getId());
                    log.info("Lifecycle freeze applied: resourceUuid={}", resourceUuid);
                }
            }
        } catch (Exception e) {
            log.error("Lifecycle task execution failed: taskId={}, resourceUuid={}",
                    task.getId(), task.getResourceUuid(), e);
            taskRepo.updateError(task.getId(),
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    /**
     * 手动触发资源生命周期评估。
     */
    public LifecycleTask triggerLifecycle(String resourceUuid) {
        StorageResource resource = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new StorageResourceService.ResourceNotFoundException(
                        "Resource not found: uuid=" + resourceUuid));
        return evaluateResource(resource);
    }

    /**
     * 手动归档资源。
     */
    @Transactional(rollbackFor = Exception.class)
    public LifecycleTask archiveResource(String resourceUuid) {
        StorageResource resource = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new StorageResourceService.ResourceNotFoundException(
                        "Resource not found: uuid=" + resourceUuid));

        if (resource.getLifecycleStage() == LifecycleStage.ARCHIVED) {
            throw new InvalidLifecycleStateException("Resource is already archived: uuid=" + resourceUuid);
        }

        if (holdRepo.hasActiveHold(resourceUuid)) {
            throw new InvalidLifecycleStateException("Resource is under active hold: uuid=" + resourceUuid);
        }

        LifecycleTask task = LifecycleTask.create(resourceUuid, null,
                LifecycleAction.ARCHIVE, LifecycleStage.ARCHIVED.name());
        LifecycleTask saved = taskRepo.save(task);
        executeTask(saved);
        return saved;
    }

    /**
     * 从归档恢复资源。
     */
    @Transactional(rollbackFor = Exception.class)
    public LifecycleTask restoreResource(String resourceUuid) {
        StorageResource resource = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new StorageResourceService.ResourceNotFoundException(
                        "Resource not found: uuid=" + resourceUuid));

        if (resource.getLifecycleStage() != LifecycleStage.ARCHIVED) {
            throw new InvalidLifecycleStateException("Resource is not archived: uuid=" + resourceUuid);
        }

        LifecycleTask task = LifecycleTask.create(resourceUuid, null,
                LifecycleAction.MOVE, LifecycleStage.ACTIVE.name());
        LifecycleTask saved = taskRepo.save(task);
        resourceRepo.updateLifecycleStage(resourceUuid, LifecycleStage.ACTIVE.name());
        taskRepo.markCompleted(saved.getId());
        log.info("Resource restored from archive: resourceUuid={}", resourceUuid);
        return saved;
    }

    // ---- helpers ----

    private LifecyclePolicy findPolicy(StorageResource resource) {
        String type = resource.getResourceType() != null ? resource.getResourceType().name() : "OTHER";
        String category = resource.getCategory() != null ? resource.getCategory().name() : "OTHER";
        return policyRepo.findByTypeAndCategory(type, category).orElse(null);
    }

    private boolean needsTransition(LifecycleStage current, LifecycleStage target) {
        if (current == target) return false;
        // DELETED 阶段不再向前推进
        if (current == LifecycleStage.DELETED) return false;
        // 不能回退（除非手动恢复）
        return stageOrdinal(target) > stageOrdinal(current);
    }

    private int stageOrdinal(LifecycleStage stage) {
        return switch (stage) {
            case ACTIVE -> 0;
            case WARM -> 1;
            case COLD -> 2;
            case ARCHIVED -> 3;
            case DELETED -> 4;
        };
    }

    private LifecycleAction determineAction(LifecycleStage current, LifecycleStage target) {
        return switch (target) {
            case ACTIVE -> LifecycleAction.NOTHING;
            case WARM, COLD -> LifecycleAction.MOVE;
            case ARCHIVED -> LifecycleAction.ARCHIVE;
            case DELETED -> LifecycleAction.DELETE;
        };
    }

    private void checkReferences(String resourceUuid) {
        var resource = resourceRepo.findByResourceUuid(resourceUuid);
        if (resource.isPresent() && resource.get().getMetadataUuid() != null) {
            int refCount = referenceRepo.countByMetadataUuid(resource.get().getMetadataUuid());
            if (refCount > 0) {
                throw new ReferenceProtectionException(
                        "Cannot proceed: resource has " + refCount + " active reference(s): uuid=" + resourceUuid);
            }
        }
    }

    // ---- inner exceptions ----

    public static class InvalidLifecycleStateException extends RuntimeException {
        public InvalidLifecycleStateException(String message) {
            super(message);
        }
    }

    public static class ReferenceProtectionException extends RuntimeException {
        public ReferenceProtectionException(String message) {
            super(message);
        }
    }
}