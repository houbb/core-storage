package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.SyncTaskStatus;
import io.coreplatform.storage.application.domain.enums.SyncTaskType;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
import io.coreplatform.storage.infrastructure.persistence.repository.SyncTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 同步任务调度器 — 定时扫描 PENDING 任务并执行。
 * <p>
 * Task + Scheduler 模型，不依赖 MQ/RabbitMQ/Kafka。
 * 通过配置项 {@code core.storage.replication.scheduler-interval-ms} 控制扫描间隔，
 * 通过 {@code core.storage.replication.max-batch-size} 控制每批处理上限。
 */
@Component
public class SyncTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncTaskScheduler.class);

    private final SyncTaskRepository taskRepo;
    private final ReplicationEngine engine;
    private final StorageProperties properties;

    public SyncTaskScheduler(SyncTaskRepository taskRepo,
                              ReplicationEngine engine,
                              StorageProperties properties) {
        this.taskRepo = taskRepo;
        this.engine = engine;
        this.properties = properties;
    }

    /**
     * 定时扫描 PENDING 任务并执行。
     * 默认 5 秒扫描一次，单线程顺序处理。失败任务记录错误，不阻塞后续任务。
     */
    @Scheduled(fixedDelayString = "${core.storage.replication.scheduler-interval-ms:5000}")
    public void processPendingTasks() {
        try {
            int maxBatchSize = properties.getReplication().getMaxBatchSize();
            List<SyncTask> pendingTasks = taskRepo.findPending(maxBatchSize);

            if (!pendingTasks.isEmpty()) {
                log.info("Scheduler found {} PENDING task(s)", pendingTasks.size());
            }

            for (SyncTask task : pendingTasks) {
                processTask(task);
            }
        } catch (Exception e) {
            log.error("Scheduler error in processPendingTasks", e);
        }
    }

    /**
     * 处理单个任务。
     */
    private void processTask(SyncTask task) {
        try {
            // 标记为 RUNNING
            taskRepo.updateStatus(task.getId(), SyncTaskStatus.RUNNING);
            log.info("Processing task: id={}, type={}, resource={}", task.getId(), task.getTaskType(), task.getResourceUuid());

            // 按任务类型分发到不同引擎方法
            switch (task.getTaskType()) {
                case SYNC -> engine.executeSync(task);
                case MIGRATE -> engine.executeMigration(task);
                case RECOVER -> engine.executeRecovery(task);
                case VERIFY -> {
                    boolean ok = engine.verifyChecksum(task.getResourceUuid(),
                            task.getSourceProfile(), task.getTargetProfile());
                    if (ok) {
                        taskRepo.updateProgress(task.getId(), 100);
                        taskRepo.updateStatus(task.getId(), SyncTaskStatus.COMPLETED);
                        log.info("Verify task completed: id={}, match=true", task.getId());
                    } else {
                        taskRepo.updateError(task.getId(), "Checksum mismatch");
                        log.warn("Verify task failed: id={}, match=false", task.getId());
                    }
                }
                default -> {
                    taskRepo.updateError(task.getId(), "Unknown task type: " + task.getTaskType());
                }
            }
        } catch (Exception e) {
            log.error("Task execution failed: id={}, resource={}", task.getId(), task.getResourceUuid(), e);
            taskRepo.updateError(task.getId(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}