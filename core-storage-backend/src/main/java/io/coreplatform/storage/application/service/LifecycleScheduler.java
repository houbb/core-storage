package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.LifecycleTask;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.enums.LifecycleTaskStatus;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
import io.coreplatform.storage.infrastructure.persistence.repository.LifecycleTaskRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 生命周期调度器 — 定时扫描资源并执行生命周期任务。
 * <p>
 * Task + Scheduler 模型，与 SyncTaskScheduler 保持一致架构。
 * 两个独立的扫描周期：
 * 1. 评估扫描：扫描所有活跃资源，匹配策略，创建任务
 * 2. 执行扫描：扫描 PENDING 任务并执行
 * <p>
 * 通过配置项 {@code core.storage.lifecycle.scheduler-interval-ms} 控制扫描间隔，
 * 通过 {@code core.storage.lifecycle.max-batch-size} 控制每批处理上限。
 */
@Component
public class LifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(LifecycleScheduler.class);

    private final StorageResourceRepository resourceRepo;
    private final LifecycleTaskRepository taskRepo;
    private final LifecycleEngine engine;
    private final StorageProperties properties;

    public LifecycleScheduler(StorageResourceRepository resourceRepo,
                               LifecycleTaskRepository taskRepo,
                               LifecycleEngine engine,
                               StorageProperties properties) {
        this.resourceRepo = resourceRepo;
        this.taskRepo = taskRepo;
        this.engine = engine;
        this.properties = properties;
    }

    /**
     * Phase 1: 评估扫描 — 检查资源是否需要生命周期转换。
     * 默认每 60 秒执行一次。
     */
    @Scheduled(fixedDelayString = "${core.storage.lifecycle.scheduler-interval-ms:60000}")
    public void evaluateResources() {
        try {
            int maxBatchSize = properties.getLifecycle().getMaxBatchSize();
            List<StorageResource> resources = resourceRepo.findActiveForLifecycle();

            int processed = 0;
            for (StorageResource resource : resources) {
                if (processed >= maxBatchSize) break;
                try {
                    LifecycleTask task = engine.evaluateResource(resource);
                    if (task != null) processed++;
                } catch (Exception e) {
                    log.warn("Evaluation error for resource={}", resource.getResourceUuid(), e);
                }
            }

            if (processed > 0) {
                log.debug("Lifecycle evaluation: created {} task(s) from {} active resource(s)",
                        processed, resources.size());
            }
        } catch (Exception e) {
            log.error("Scheduler error in evaluateResources", e);
        }
    }

    /**
     * Phase 2: 执行扫描 — 执行 PENDING 任务。
     * 默认每 60 秒执行一次（与评估共享间隔）。
     */
    @Scheduled(fixedDelayString = "${core.storage.lifecycle.scheduler-interval-ms:60000}")
    public void processPendingTasks() {
        try {
            int maxBatchSize = properties.getLifecycle().getMaxBatchSize();
            List<LifecycleTask> pendingTasks = taskRepo.findPending(maxBatchSize);

            if (!pendingTasks.isEmpty()) {
                log.info("Lifecycle scheduler found {} PENDING task(s)", pendingTasks.size());
            }

            for (LifecycleTask task : pendingTasks) {
                try {
                    engine.executeTask(task);
                } catch (Exception e) {
                    log.error("Task execution failed: taskId={}, resourceUuid={}",
                            task.getId(), task.getResourceUuid(), e);
                }
            }
        } catch (Exception e) {
            log.error("Scheduler error in processPendingTasks", e);
        }
    }
}