package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.application.domain.StorageReplica;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.*;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReplicaRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.SyncTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReplicationEngineTest {

    private ReplicationEngine engine;
    private StorageDriverFactory driverFactory;
    private StorageMetadataRepository metadataRepo;
    private StorageResourceRepository resourceRepo;
    private StorageReplicaRepository replicaRepo;
    private SyncTaskRepository taskRepo;

    private StorageDriver sourceDriver;
    private StorageDriver targetDriver;

    private StorageResource resource() {
        StorageResource r = new StorageResource();
        r.setResourceUuid("res-001");
        r.setMetadataUuid("md-001");
        r.setProfileName("default");
        return r;
    }

    @BeforeEach
    void setUp() {
        driverFactory = mock(StorageDriverFactory.class);
        metadataRepo = mock(StorageMetadataRepository.class);
        resourceRepo = mock(StorageResourceRepository.class);
        replicaRepo = mock(StorageReplicaRepository.class);
        taskRepo = mock(SyncTaskRepository.class);

        sourceDriver = mock(StorageDriver.class);
        targetDriver = mock(StorageDriver.class);

        when(driverFactory.getDriverForProfile("default")).thenReturn(sourceDriver);
        when(driverFactory.getDriverForProfile("database")).thenReturn(targetDriver);

        engine = new ReplicationEngine(driverFactory, metadataRepo, resourceRepo, replicaRepo, taskRepo);
    }

    @Test
    void executeSyncSuccessfully() throws Exception {
        byte[] content = "hello replication".getBytes();

        StorageMetadata metadata = new StorageMetadata();
        metadata.setRelativePath("2026/07/16");
        metadata.setStorageName("test.bin");
        metadata.setHashSha256("64465f344f77596d01830c5e6bdd4534665d407495cada0dc91e9675b66824be");

        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);

        StorageReplica replica = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        replica.setId(2L);

        when(metadataRepo.findByUuid("md-001")).thenReturn(Optional.of(metadata));
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource()));
        when(sourceDriver.download("2026/07/16", "test.bin")).thenReturn(new ByteArrayInputStream(content));
        when(replicaRepo.findByResourceUuidAndProfile("res-001", "database")).thenReturn(Optional.of(replica));

        engine.executeSync(task);

        // 验证下载和上传
        verify(sourceDriver).download("2026/07/16", "test.bin");
        verify(targetDriver).upload(eq("2026/07/16"), eq("test.bin"), any(InputStream.class));

        // 验证进度更新
        verify(taskRepo).updateProgress(1L, 10);
        verify(taskRepo).updateProgress(1L, 40);
        verify(taskRepo).updateProgress(1L, 80);
        verify(taskRepo).updateProgress(1L, 100);

        // 验证副本更新
        verify(replicaRepo).updateChecksum(eq(2L), anyString(), any());
        verify(replicaRepo).updateStatus(2L, ReplicaStatus.READY);
        verify(taskRepo).updateStatus(1L, SyncTaskStatus.COMPLETED);
    }

    @Test
    void executeSyncFailsWhenChecksumMismatch() throws Exception {
        byte[] content = "hello".getBytes();

        StorageMetadata metadata = new StorageMetadata();
        metadata.setRelativePath("2026/07/16");
        metadata.setStorageName("test.bin");
        metadata.setHashSha256("EXPECTED_DIFFERENT_HASH");  // 故意不匹配

        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);

        StorageReplica replica = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        replica.setId(2L);

        when(metadataRepo.findByUuid("md-001")).thenReturn(Optional.of(metadata));
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource()));
        when(sourceDriver.download("2026/07/16", "test.bin")).thenReturn(new ByteArrayInputStream(content));
        when(replicaRepo.findByResourceUuidAndProfile("res-001", "database")).thenReturn(Optional.of(replica));

        engine.executeSync(task);

        // 校验失败：任务应标记为 FAILED
        verify(taskRepo).updateError(eq(1L), contains("Checksum mismatch"));
        verify(replicaRepo).updateStatus(2L, ReplicaStatus.FAILED);
        // 不应该标记 COMPLETED
        verify(taskRepo, never()).updateStatus(1L, SyncTaskStatus.COMPLETED);
    }

    @Test
    void executeSyncSkipsChecksumWhenSourceChecksumMissing() throws Exception {
        byte[] content = "no checksum".getBytes();

        StorageMetadata metadata = new StorageMetadata();
        metadata.setRelativePath("2026/07/16");
        metadata.setStorageName("test.bin");
        metadata.setHashSha256(null);  // 没有 checksum

        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);

        StorageReplica replica = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        replica.setId(2L);

        when(metadataRepo.findByUuid("md-001")).thenReturn(Optional.of(metadata));
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource()));
        when(sourceDriver.download("2026/07/16", "test.bin")).thenReturn(new ByteArrayInputStream(content));
        when(replicaRepo.findByResourceUuidAndProfile("res-001", "database")).thenReturn(Optional.of(replica));

        engine.executeSync(task);

        // 跳过校验，仍标记完成
        verify(taskRepo).updateStatus(1L, SyncTaskStatus.COMPLETED);
        verify(replicaRepo).updateStatus(2L, ReplicaStatus.READY);
    }

    @Test
    void executeMigrationPromotesTargetToPrimary() throws Exception {
        byte[] content = "migration test".getBytes();

        StorageMetadata metadata = new StorageMetadata();
        metadata.setRelativePath("2026/07/16");
        metadata.setStorageName("migrate.bin");
        metadata.setHashSha256("ecc64ba5d7a198af0b4894caeeb43f7578af4caa44a69b71dc4b0f2b35a9448c");

        SyncTask task = SyncTask.create(SyncTaskType.MIGRATE, "res-001", "default", "database");
        task.setId(1L);

        StorageReplica primary = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        primary.setId(1L);
        StorageReplica target = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        target.setId(2L);

        when(metadataRepo.findByUuid("md-001")).thenReturn(Optional.of(metadata));
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource()));
        when(sourceDriver.download("2026/07/16", "migrate.bin")).thenReturn(new ByteArrayInputStream(content));
        when(replicaRepo.findByResourceUuidAndProfile("res-001", "database")).thenReturn(Optional.of(target));
        when(replicaRepo.findByResourceUuidAndRole("res-001", ReplicaRole.PRIMARY)).thenReturn(Optional.of(primary));
        // executeSync 内部会调 updateStatus 标记 COMPLETED，executeMigration 后续又调 findById 检查状态
        when(taskRepo.findById(1L)).thenAnswer(inv -> {
            task.setStatus(SyncTaskStatus.COMPLETED);
            return Optional.of(task);
        });

        engine.executeMigration(task);

        // 旧 PRIMARY 降级为 BACKUP
        verify(replicaRepo).updateRole(1L, ReplicaRole.BACKUP);
        // 目标提升为 PRIMARY
        verify(replicaRepo).updateRole(2L, ReplicaRole.PRIMARY);
    }

    @Test
    void verifyChecksumReturnsTrueWhenMatching() throws Exception {
        byte[] content = "verify me".getBytes();

        StorageMetadata metadata = new StorageMetadata();
        metadata.setRelativePath("2026/07/16");
        metadata.setStorageName("verify.bin");

        when(metadataRepo.findByUuid("md-001")).thenReturn(Optional.of(metadata));
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource()));
        when(sourceDriver.download("2026/07/16", "verify.bin")).thenReturn(new ByteArrayInputStream(content));
        when(targetDriver.download("2026/07/16", "verify.bin")).thenReturn(new ByteArrayInputStream(content));

        boolean result = engine.verifyChecksum("res-001", "default", "database");

        assertTrue(result);
    }

    @Test
    void verifyChecksumReturnsFalseWhenMismatch() throws Exception {
        StorageMetadata metadata = new StorageMetadata();
        metadata.setRelativePath("2026/07/16");
        metadata.setStorageName("verify.bin");

        when(metadataRepo.findByUuid("md-001")).thenReturn(Optional.of(metadata));
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource()));
        when(sourceDriver.download("2026/07/16", "verify.bin")).thenReturn(new ByteArrayInputStream("aaa".getBytes()));
        when(targetDriver.download("2026/07/16", "verify.bin")).thenReturn(new ByteArrayInputStream("bbb".getBytes()));

        boolean result = engine.verifyChecksum("res-001", "default", "database");

        assertFalse(result);
    }
}