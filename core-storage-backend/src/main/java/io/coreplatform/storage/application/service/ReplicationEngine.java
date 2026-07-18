package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.ReplicaRole;
import io.coreplatform.storage.application.domain.enums.ReplicaStatus;
import io.coreplatform.storage.application.domain.enums.SyncTaskStatus;
import io.coreplatform.storage.application.domain.enums.SyncTaskType;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReplicaRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.SyncTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 复制引擎 — 核心同步执行逻辑。
 * <p>
 * 负责在 Driver 层之间搬运数据 + SHA-256 校验，
 * 不参与业务逻辑、不参与事务管理。
 */
@Component
public class ReplicationEngine {

    private static final Logger log = LoggerFactory.getLogger(ReplicationEngine.class);

    private final StorageDriverFactory driverFactory;
    private final StorageMetadataRepository metadataRepo;
    private final StorageResourceRepository resourceRepo;
    private final StorageReplicaRepository replicaRepo;
    private final SyncTaskRepository taskRepo;

    public ReplicationEngine(StorageDriverFactory driverFactory,
                              StorageMetadataRepository metadataRepo,
                              StorageResourceRepository resourceRepo,
                              StorageReplicaRepository replicaRepo,
                              SyncTaskRepository taskRepo) {
        this.driverFactory = driverFactory;
        this.metadataRepo = metadataRepo;
        this.resourceRepo = resourceRepo;
        this.replicaRepo = replicaRepo;
        this.taskRepo = taskRepo;
    }

    /**
     * 执行同步任务。
     * <p>
     * 流程：source driver download → target driver upload → checksum verify → update replica
     */
    public void executeSync(SyncTask task) throws Exception {
        log.info("Executing sync task: id={}, resource={}, source={}, target={}",
                task.getId(), task.getResourceUuid(), task.getSourceProfile(), task.getTargetProfile());

        StorageDriver sourceDriver = driverFactory.getDriverForProfile(task.getSourceProfile());
        StorageDriver targetDriver = driverFactory.getDriverForProfile(task.getTargetProfile());

        // 获取源文件元数据 — resourceUuid → metadataUuid → metadata
        String metadataUuid = resourceRepo.findByResourceUuid(task.getResourceUuid())
                .map(r -> r.getMetadataUuid())
                .orElseThrow(() -> new RuntimeException("Resource not found: uuid=" + task.getResourceUuid()));
        StorageMetadata metadata = metadataRepo.findByUuid(metadataUuid)
                .orElseThrow(() -> new RuntimeException("Metadata not found: uuid=" + metadataUuid));
        String relativePath = metadata.getRelativePath();
        String storageName = metadata.getStorageName();
        String sourceChecksum = metadata.getHashSha256();

        log.info("Sync metadata: relativePath={}, storageName={}, sourceChecksum={}",
                relativePath, storageName, sourceChecksum);

        // 更新任务进度：10%
        taskRepo.updateProgress(task.getId(), 10);

        // 1. 从源 Driver 下载
        log.info("Downloading from source: profile={}", task.getSourceProfile());
        InputStream downloadedStream = sourceDriver.download(relativePath, storageName);

        // 更新任务进度：40%
        taskRepo.updateProgress(task.getId(), 40);

        // 2. 上传到目标 Driver（流式传输，使用 DigestInputStream 同时计算 SHA-256）
        log.info("Uploading to target: profile={}", task.getTargetProfile());
        String targetChecksum;
        try (InputStream in = downloadedStream) {
            targetChecksum = uploadAndComputeChecksum(targetDriver, relativePath, storageName, in);
        }

        // 更新任务进度：80%
        taskRepo.updateProgress(task.getId(), 80);

        // 3. 校验 — 比较源和目标 SHA-256
        if (sourceChecksum != null && !sourceChecksum.isEmpty()) {
            if (!sourceChecksum.equals(targetChecksum)) {
                String errorMsg = String.format("Checksum mismatch: source=%s, target=%s", sourceChecksum, targetChecksum);
                log.error(errorMsg);
                taskRepo.updateError(task.getId(), errorMsg);
                // 把对应 replica 标记为 FAILED
                updateReplicaOnFailure(task);
                return;
            }
            log.info("Checksum verified: {}", targetChecksum);
        } else {
            log.warn("Source checksum not available, skipping verification. resourceUuid={}", task.getResourceUuid());
        }

        // 4. 更新副本信息
        updateReplicaOnSuccess(task, targetChecksum);

        // 5. 标记任务完成
        taskRepo.updateProgress(task.getId(), 100);
        taskRepo.updateStatus(task.getId(), SyncTaskStatus.COMPLETED);
        log.info("Sync task completed: id={}, resource={}", task.getId(), task.getResourceUuid());
    }

    /**
     * 执行迁移任务 — 与同步相同，但完成后更新 PRIMARY 指向。
     */
    public void executeMigration(SyncTask task) throws Exception {
        log.info("Executing migration task: id={}, resource={}, source={}, target={}",
                task.getId(), task.getResourceUuid(), task.getSourceProfile(), task.getTargetProfile());

        // 先执行同步
        executeSync(task);

        if (taskRepo.findById(task.getId())
                .map(t -> t.getStatus() == SyncTaskStatus.COMPLETED).orElse(false)) {

            // 迁移完成：更新 PRIMARY 副本指向
            replicaRepo.findByResourceUuidAndRole(task.getResourceUuid(), ReplicaRole.PRIMARY)
                    .ifPresent(primary -> {
                        replicaRepo.updateRole(primary.getId(), ReplicaRole.BACKUP);
                        log.info("Old PRIMARY demoted to BACKUP: id={}", primary.getId());
                    });

            replicaRepo.findByResourceUuidAndProfile(task.getResourceUuid(), task.getTargetProfile())
                    .ifPresent(target -> {
                        replicaRepo.updateRole(target.getId(), ReplicaRole.PRIMARY);
                        log.info("Target replica promoted to PRIMARY: id={}", target.getId());
                    });

            log.info("Migration completed: resource={}, newProfile={}",
                    task.getResourceUuid(), task.getTargetProfile());
        }
    }

    /**
     * 执行故障恢复 — 从 SECONDARY 副本恢复数据到 PRIMARY。
     */
    public void executeRecovery(SyncTask task) throws Exception {
        log.info("Executing recovery task: id={}, resource={}", task.getId(), task.getResourceUuid());

        // 先执行同步（从 SECONDARY 恢复到 PRIMARY）
        executeSync(task);

        if (taskRepo.findById(task.getId())
                .map(t -> t.getStatus() == SyncTaskStatus.COMPLETED).orElse(false)) {

            // 恢复完成：提升 SECONDARY 为 PRIMARY
            replicaRepo.findByResourceUuidAndProfile(task.getResourceUuid(), task.getSourceProfile())
                    .ifPresent(secondary -> {
                        replicaRepo.updateRole(secondary.getId(), ReplicaRole.PRIMARY);
                        replicaRepo.updateStatus(secondary.getId(), ReplicaStatus.READY);
                        log.info("SECONDARY promoted to PRIMARY: id={}", secondary.getId());
                    });

            log.info("Recovery completed: resource={}", task.getResourceUuid());
        }
    }

    /**
     * 校验两个副本的一致性（仅校验，不同步数据）。
     */
    public boolean verifyChecksum(String resourceUuid, String sourceProfile, String targetProfile) throws IOException {
        String metadataUuid = resourceRepo.findByResourceUuid(resourceUuid)
                .map(r -> r.getMetadataUuid())
                .orElseThrow(() -> new RuntimeException("Resource not found: uuid=" + resourceUuid));
        StorageMetadata metadata = metadataRepo.findByUuid(metadataUuid)
                .orElseThrow(() -> new RuntimeException("Metadata not found: uuid=" + metadataUuid));
        String relativePath = metadata.getRelativePath();
        String storageName = metadata.getStorageName();

        StorageDriver sourceDriver = driverFactory.getDriverForProfile(sourceProfile);
        StorageDriver targetDriver = driverFactory.getDriverForProfile(targetProfile);

        String sourceHash = computeChecksum(sourceDriver, relativePath, storageName);
        String targetHash = computeChecksum(targetDriver, relativePath, storageName);

        return sourceHash != null && sourceHash.equals(targetHash);
    }

    // ---- private helpers ----

    /**
     * 上传到目标 Driver 同时计算 SHA-256。
     */
    private String uploadAndComputeChecksum(StorageDriver driver, String relativePath,
                                             String storageName, InputStream in) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }

        // 先将流读入临时缓冲区以计算 checksum，然后上传
        byte[] buf = new byte[8192];
        int n;
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        while ((n = in.read(buf)) > 0) {
            digest.update(buf, 0, n);
            buffer.write(buf, 0, n);
        }

        byte[] data = buffer.toByteArray();
        String checksum = HexFormat.of().formatHex(digest.digest());

        // 上传
        try (InputStream uploadStream = new java.io.ByteArrayInputStream(data)) {
            driver.upload(relativePath, storageName, uploadStream);
        }

        return checksum;
    }

    /**
     * 计算指定 Driver 上文件的 SHA-256。
     */
    private String computeChecksum(StorageDriver driver, String relativePath, String storageName) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }

        try (InputStream in = driver.download(relativePath, storageName)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * 同步成功后更新副本信息。
     */
    private void updateReplicaOnSuccess(SyncTask task, String checksum) {
        replicaRepo.findByResourceUuidAndProfile(task.getResourceUuid(), task.getTargetProfile())
                .ifPresent(replica -> {
                    replicaRepo.updateChecksum(replica.getId(), checksum, LocalDateTime.now());
                    replicaRepo.updateStatus(replica.getId(), ReplicaStatus.READY);
                    log.info("Replica updated: id={}, status=READY, checksum={}", replica.getId(), checksum);
                });
    }

    /**
     * 同步失败后标记副本为 FAILED。
     */
    private void updateReplicaOnFailure(SyncTask task) {
        replicaRepo.findByResourceUuidAndProfile(task.getResourceUuid(), task.getTargetProfile())
                .ifPresent(replica -> {
                    replicaRepo.updateStatus(replica.getId(), ReplicaStatus.FAILED);
                    log.warn("Replica marked FAILED: id={}", replica.getId());
                });
    }
}