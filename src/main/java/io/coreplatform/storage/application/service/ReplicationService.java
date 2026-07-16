package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageReplica;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.ReplicaRole;
import io.coreplatform.storage.application.domain.enums.ReplicaStatus;
import io.coreplatform.storage.application.domain.enums.SyncTaskStatus;
import io.coreplatform.storage.application.domain.enums.SyncTaskType;
import io.coreplatform.storage.infrastructure.driver.DriverRegistry;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReplicaRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageProfileRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.SyncTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 复制服务 — 副本管理 + 同步/迁移/恢复编排。
 * <p>
 * 对外提供 ReplicationController 调用的所有业务方法，
 * 内部委托给 ReplicationEngine 执行实际的 Driver 间数据搬运。
 */
@Service
public class ReplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    private final StorageReplicaRepository replicaRepo;
    private final SyncTaskRepository taskRepo;
    private final StorageResourceRepository resourceRepo;
    private final StorageProfileRepository profileRepo;
    private final StorageDriverFactory driverFactory;

    public ReplicationService(StorageReplicaRepository replicaRepo,
                               SyncTaskRepository taskRepo,
                               StorageResourceRepository resourceRepo,
                               StorageProfileRepository profileRepo,
                               StorageDriverFactory driverFactory) {
        this.replicaRepo = replicaRepo;
        this.taskRepo = taskRepo;
        this.resourceRepo = resourceRepo;
        this.profileRepo = profileRepo;
        this.driverFactory = driverFactory;
    }

    // ---- Replica management ----

    /**
     * 查询 Resource 的所有副本。
     */
    public List<StorageReplica> listReplicas(String resourceUuid) {
        return replicaRepo.findByResourceUuid(resourceUuid);
    }

    /**
     * 为 Resource 添加一个副本，自动创建 SYNC 任务。
     * <p>
     * 添加成功后，Scheduler 会自动执行首次同步。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageReplica addReplica(String resourceUuid, String profileName, ReplicaRole role) {
        // 1. 验证 Resource 存在
        StorageResource resource = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new ReplicaNotFoundException("Resource not found: uuid=" + resourceUuid));

        // 2. 验证 Profile 存在
        var profile = profileRepo.findByProfileName(profileName)
                .orElseThrow(() -> new ReplicaNotFoundException("Profile not found: name=" + profileName));

        // 3. 检查是否已存在同 Profile 的副本
        replicaRepo.findByResourceUuidAndProfile(resourceUuid, profileName)
                .ifPresent(r -> {
                    throw new ReplicaAlreadyExistsException(
                            "Replica already exists for resource=" + resourceUuid + ", profile=" + profileName);
                });

        // 4. 创建副本
        String driverName = profile.getDriverName();
        StorageReplica replica = StorageReplica.create(resourceUuid, profileName, driverName, role);
        StorageReplica saved = replicaRepo.save(replica);
        log.info("Replica created: id={}, resource={}, profile={}, role={}",
                saved.getId(), resourceUuid, profileName, role);

        // 5. 如果是非 PRIMARY 角色，自动创建 SYNC 任务
        if (role != ReplicaRole.PRIMARY) {
            String sourceProfile = resource.getProfileName() != null ? resource.getProfileName() : "default";
            SyncTask task = SyncTask.create(SyncTaskType.SYNC, resourceUuid, sourceProfile, profileName);
            taskRepo.save(task);
            log.info("Auto sync task created: resource={}, source={}, target={}",
                    resourceUuid, sourceProfile, profileName);
        }

        return saved;
    }

    /**
     * 删除副本。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeReplica(Long replicaId) {
        StorageReplica replica = replicaRepo.findById(replicaId)
                .orElseThrow(() -> new ReplicaNotFoundException("Replica not found: id=" + replicaId));

        // 不能删除 PRIMARY 副本
        if (replica.getReplicaRole() == ReplicaRole.PRIMARY) {
            throw new CannotDeletePrimaryReplicaException(
                    "Cannot delete PRIMARY replica: id=" + replicaId);
        }

        replicaRepo.updateStatus(replicaId, ReplicaStatus.DELETING);
        replicaRepo.delete(replicaId);
        log.info("Replica removed: id={}, resource={}, profile={}",
                replicaId, replica.getResourceUuid(), replica.getProfileName());
    }

    // ---- Sync / Migrate / Recover ----

    /**
     * 触发资源同步 — 为所有非 PRIMARY 副本创建 SYNC 任务。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SyncTask> syncResource(String resourceUuid) {
        StorageResource resource = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new ReplicaNotFoundException("Resource not found: uuid=" + resourceUuid));

        String sourceProfile = resource.getProfileName() != null ? resource.getProfileName() : "default";
        List<StorageReplica> replicas = replicaRepo.findByResourceUuid(resourceUuid);

        List<SyncTask> tasks = new java.util.ArrayList<>();
        for (StorageReplica replica : replicas) {
            if (replica.getReplicaRole() != ReplicaRole.PRIMARY) {
                // 检查是否有活跃任务
                ensureNoActiveTask(resourceUuid, replica.getProfileName());

                SyncTask task = SyncTask.create(SyncTaskType.SYNC, resourceUuid,
                        sourceProfile, replica.getProfileName());
                tasks.add(taskRepo.save(task));
            }
        }

        log.info("Sync triggered: resource={}, tasks={}", resourceUuid, tasks.size());
        return tasks;
    }

    /**
     * 触发资源迁移 — 将 PRIMARY 切换到 targetProfile。
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncTask migrateResource(String resourceUuid, String sourceProfile, String targetProfile) {
        // 验证 profile 存在
        profileRepo.findByProfileName(sourceProfile)
                .orElseThrow(() -> new ReplicaNotFoundException("Source profile not found: " + sourceProfile));
        profileRepo.findByProfileName(targetProfile)
                .orElseThrow(() -> new ReplicaNotFoundException("Target profile not found: " + targetProfile));

        if (sourceProfile.equals(targetProfile)) {
            throw new InvalidReplicationTargetException("Source and target profiles must differ");
        }

        ensureNoActiveTask(resourceUuid, targetProfile);

        SyncTask task = SyncTask.create(SyncTaskType.MIGRATE, resourceUuid, sourceProfile, targetProfile);
        SyncTask saved = taskRepo.save(task);
        log.info("Migration triggered: resource={}, source={}, target={}", resourceUuid, sourceProfile, targetProfile);
        return saved;
    }

    /**
     * 触发故障恢复 — 从健康的 SECONDARY 恢复到 PRIMARY。
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncTask recoverResource(String resourceUuid) {
        StorageResource resource = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new ReplicaNotFoundException("Resource not found: uuid=" + resourceUuid));

        // 找到一个健康的 SECONDARY 副本
        StorageReplica secondary = replicaRepo.findByResourceUuid(resourceUuid).stream()
                .filter(r -> r.getReplicaRole() == ReplicaRole.SECONDARY && r.getReplicaStatus() == ReplicaStatus.READY)
                .findFirst()
                .orElseThrow(() -> new ReplicaNotFoundException(
                        "No healthy SECONDARY replica found for resource=" + resourceUuid));

        String primaryProfile = resource.getProfileName() != null ? resource.getProfileName() : "default";
        ensureNoActiveTask(resourceUuid, primaryProfile);

        SyncTask task = SyncTask.create(SyncTaskType.RECOVER, resourceUuid,
                secondary.getProfileName(), primaryProfile);
        SyncTask saved = taskRepo.save(task);
        log.info("Recovery triggered: resource={}, from={}, to={}",
                resourceUuid, secondary.getProfileName(), primaryProfile);
        return saved;
    }

    /**
     * 上传后自动触发同步（如果 Resource 配置了多副本）。
     * 由 StorageService 在上传完成后调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replicateAfterUpload(String metadataUuid, String sourceProfile) {
        resourceRepo.findByMetadataUuid(metadataUuid).ifPresent(resource -> {
            List<StorageReplica> replicas = replicaRepo.findByResourceUuid(resource.getResourceUuid());
            for (StorageReplica replica : replicas) {
                if (replica.getReplicaRole() != ReplicaRole.PRIMARY) {
                    // 确保没有正在运行的任务
                    boolean hasActive = taskRepo.findActiveByResourceUuid(resource.getResourceUuid()).stream()
                            .anyMatch(t -> t.getTargetProfile().equals(replica.getProfileName()));
                    if (!hasActive) {
                        SyncTask task = SyncTask.create(SyncTaskType.SYNC, resource.getResourceUuid(),
                                sourceProfile, replica.getProfileName());
                        taskRepo.save(task);
                        log.info("Auto sync task created after upload: resource={}, target={}",
                                resource.getResourceUuid(), replica.getProfileName());
                    }
                }
            }
        });
    }

    // ---- Task queries ----

    /**
     * 查询所有任务（支持过滤）。
     */
    public List<SyncTask> listTasks(String resourceUuid, String taskType, String status,
                                     String sort, String order, int page, int size) {
        int offset = Math.max(0, page - 1) * size;
        return taskRepo.search(resourceUuid, taskType, status, sort, order, offset, size);
    }

    /**
     * 查询任务总数。
     */
    public int countTasks(String resourceUuid, String taskType, String status) {
        return taskRepo.countSearch(resourceUuid, taskType, status);
    }

    /**
     * 查询任务详情。
     */
    public SyncTask getTask(Long id) {
        return taskRepo.findById(id)
                .orElseThrow(() -> new SyncTaskNotFoundException("Task not found: id=" + id));
    }

    /**
     * 暂停任务（将 PENDING 标记为 CANCELLED）。
     */
    public void pauseTask(Long id) {
        SyncTask task = getTask(id);
        if (task.getStatus() != SyncTaskStatus.PENDING) {
            throw new SyncTaskAlreadyRunningException(
                    "Only PENDING tasks can be paused, current status=" + task.getStatus());
        }
        taskRepo.updateStatus(id, SyncTaskStatus.CANCELLED);
        log.info("Task paused: id={}", id);
    }

    /**
     * 恢复任务（将 CANCELLED 重置为 PENDING）。
     */
    public void resumeTask(Long id) {
        SyncTask task = getTask(id);
        if (task.getStatus() != SyncTaskStatus.CANCELLED) {
            throw new SyncTaskAlreadyRunningException(
                    "Only CANCELLED tasks can be resumed, current status=" + task.getStatus());
        }
        taskRepo.updateStatus(id, SyncTaskStatus.PENDING);
        log.info("Task resumed: id={}", id);
    }

    // ---- private helpers ----

    /**
     * 确保同一 resource+target 没有活跃任务。
     */
    private void ensureNoActiveTask(String resourceUuid, String targetProfile) {
        List<SyncTask> active = taskRepo.findActiveByResourceUuid(resourceUuid);
        boolean conflict = active.stream()
                .anyMatch(t -> t.getTargetProfile().equals(targetProfile));
        if (conflict) {
            throw new SyncTaskAlreadyRunningException(
                    "An active task already exists for resource=" + resourceUuid + ", target=" + targetProfile);
        }
    }

    // ---- inner exceptions ----

    public static class ReplicaNotFoundException extends RuntimeException {
        public ReplicaNotFoundException(String message) { super(message); }
    }

    public static class ReplicaAlreadyExistsException extends RuntimeException {
        public ReplicaAlreadyExistsException(String message) { super(message); }
    }

    public static class CannotDeletePrimaryReplicaException extends RuntimeException {
        public CannotDeletePrimaryReplicaException(String message) { super(message); }
    }

    public static class SyncTaskNotFoundException extends RuntimeException {
        public SyncTaskNotFoundException(String message) { super(message); }
    }

    public static class SyncTaskAlreadyRunningException extends RuntimeException {
        public SyncTaskAlreadyRunningException(String message) { super(message); }
    }

    public static class InvalidReplicationTargetException extends RuntimeException {
        public InvalidReplicationTargetException(String message) { super(message); }
    }
}