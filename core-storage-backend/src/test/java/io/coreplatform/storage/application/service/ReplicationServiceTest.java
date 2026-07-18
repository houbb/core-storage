package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageReplica;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.*;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageProfileRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReplicaRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.SyncTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReplicationServiceTest {

    private ReplicationService replicationService;
    private StorageReplicaRepository replicaRepo;
    private SyncTaskRepository taskRepo;
    private StorageResourceRepository resourceRepo;
    private StorageProfileRepository profileRepo;
    private StorageDriverFactory driverFactory;

    @BeforeEach
    void setUp() {
        replicaRepo = mock(StorageReplicaRepository.class);
        taskRepo = mock(SyncTaskRepository.class);
        resourceRepo = mock(StorageResourceRepository.class);
        profileRepo = mock(StorageProfileRepository.class);
        driverFactory = mock(StorageDriverFactory.class);
        replicationService = new ReplicationService(replicaRepo, taskRepo, resourceRepo, profileRepo, driverFactory);
    }

    // ---- 副本管理 ----

    @Test
    void listReplicasReturnsAllForResource() {
        StorageReplica r1 = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        r1.setId(1L);
        StorageReplica r2 = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        r2.setId(2L);

        when(replicaRepo.findByResourceUuid("res-001")).thenReturn(List.of(r1, r2));

        List<StorageReplica> result = replicationService.listReplicas("res-001");
        assertEquals(2, result.size());
        assertEquals(ReplicaRole.PRIMARY, result.get(0).getReplicaRole());
        assertEquals(ReplicaRole.BACKUP, result.get(1).getReplicaRole());
    }

    @Test
    void addReplicaCreatesReplicaAndSyncTask() {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid("res-001");
        resource.setProfileName("default");

        var profile = io.coreplatform.storage.application.domain.StorageProfile.create("database", "database", false);

        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource));
        when(profileRepo.findByProfileName("database")).thenReturn(Optional.of(profile));
        when(replicaRepo.findByResourceUuidAndProfile("res-001", "database")).thenReturn(Optional.empty());
        when(replicaRepo.save(any())).thenAnswer(inv -> {
            StorageReplica r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StorageReplica result = replicationService.addReplica("res-001", "database", ReplicaRole.BACKUP);

        assertNotNull(result);
        assertEquals("res-001", result.getResourceUuid());
        assertEquals("database", result.getProfileName());
        assertEquals(ReplicaRole.BACKUP, result.getReplicaRole());
        assertEquals(ReplicaStatus.CREATING, result.getReplicaStatus());

        // 应该自动创建了一个 SYNC 任务
        verify(taskRepo).save(argThat(task ->
                task.getTaskType() == SyncTaskType.SYNC
                && "res-001".equals(task.getResourceUuid())
                && "database".equals(task.getTargetProfile())
        ));
    }

    @Test
    void addReplicaThrowsWhenProfileNotFound() {
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(new StorageResource()));
        when(profileRepo.findByProfileName("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ReplicationService.ReplicaNotFoundException.class, () ->
                replicationService.addReplica("res-001", "nonexistent", ReplicaRole.BACKUP));
    }

    @Test
    void addReplicaThrowsWhenAlreadyExists() {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid("res-001");
        resource.setProfileName("default");

        var profile = io.coreplatform.storage.application.domain.StorageProfile.create("database", "database", false);

        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource));
        when(profileRepo.findByProfileName("database")).thenReturn(Optional.of(profile));
        when(replicaRepo.findByResourceUuidAndProfile("res-001", "database"))
                .thenReturn(Optional.of(new StorageReplica()));

        assertThrows(ReplicationService.ReplicaAlreadyExistsException.class, () ->
                replicationService.addReplica("res-001", "database", ReplicaRole.BACKUP));
    }

    @Test
    void removeReplicaDeletesNonPrimary() {
        StorageReplica replica = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        replica.setId(1L);
        when(replicaRepo.findById(1L)).thenReturn(Optional.of(replica));

        replicationService.removeReplica(1L);

        verify(replicaRepo).updateStatus(1L, ReplicaStatus.DELETING);
        verify(replicaRepo).delete(1L);
    }

    @Test
    void removeReplicaThrowsWhenPrimary() {
        StorageReplica primary = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        primary.setId(1L);
        when(replicaRepo.findById(1L)).thenReturn(Optional.of(primary));

        assertThrows(ReplicationService.CannotDeletePrimaryReplicaException.class, () ->
                replicationService.removeReplica(1L));
    }

    // ---- 同步 / 迁移 / 恢复 ----

    @Test
    void syncResourceCreatesTasksForNonPrimaryReplicas() {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid("res-001");
        resource.setProfileName("default");

        StorageReplica primary = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        primary.setId(1L);
        StorageReplica backup = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        backup.setId(2L);

        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource));
        when(replicaRepo.findByResourceUuid("res-001")).thenReturn(List.of(primary, backup));
        when(taskRepo.findActiveByResourceUuid("res-001")).thenReturn(List.of());
        when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<SyncTask> tasks = replicationService.syncResource("res-001");

        assertEquals(1, tasks.size());
        assertEquals("database", tasks.get(0).getTargetProfile());
        assertEquals("default", tasks.get(0).getSourceProfile());
    }

    @Test
    void migrateResourceCreatesMigrationTask() {
        var sourceProfile = io.coreplatform.storage.application.domain.StorageProfile.create("default", "local", true);
        var targetProfile = io.coreplatform.storage.application.domain.StorageProfile.create("database", "database", false);

        when(profileRepo.findByProfileName("default")).thenReturn(Optional.of(sourceProfile));
        when(profileRepo.findByProfileName("database")).thenReturn(Optional.of(targetProfile));
        when(taskRepo.findActiveByResourceUuid("res-001")).thenReturn(List.of());
        when(taskRepo.save(any())).thenAnswer(inv -> {
            SyncTask t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        SyncTask task = replicationService.migrateResource("res-001", "default", "database");

        assertEquals(SyncTaskType.MIGRATE, task.getTaskType());
        assertEquals("default", task.getSourceProfile());
        assertEquals("database", task.getTargetProfile());
    }

    @Test
    void migrateResourceThrowsWhenSameProfile() {
        var profile = io.coreplatform.storage.application.domain.StorageProfile.create("default", "local", true);
        when(profileRepo.findByProfileName("default")).thenReturn(Optional.of(profile));

        assertThrows(ReplicationService.InvalidReplicationTargetException.class, () ->
                replicationService.migrateResource("res-001", "default", "default"));
    }

    @Test
    void recoverResourceUsesHealthySecondary() {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid("res-001");
        resource.setProfileName("default");

        StorageReplica primary = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        primary.setId(1L);
        primary.setReplicaStatus(ReplicaStatus.FAILED);
        StorageReplica secondary = StorageReplica.create("res-001", "database", "database", ReplicaRole.SECONDARY);
        secondary.setId(2L);
        secondary.setReplicaStatus(ReplicaStatus.READY);

        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource));
        when(replicaRepo.findByResourceUuid("res-001")).thenReturn(List.of(primary, secondary));
        when(taskRepo.findActiveByResourceUuid("res-001")).thenReturn(List.of());
        when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncTask task = replicationService.recoverResource("res-001");

        assertEquals(SyncTaskType.RECOVER, task.getTaskType());
        assertEquals("database", task.getSourceProfile()); // 从 healthy secondary 恢复
    }

    @Test
    void recoverResourceThrowsWhenNoHealthySecondary() {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid("res-001");
        resource.setProfileName("default");

        StorageReplica primary = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        primary.setReplicaStatus(ReplicaStatus.FAILED);

        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(resource));
        when(replicaRepo.findByResourceUuid("res-001")).thenReturn(List.of(primary));

        assertThrows(ReplicationService.ReplicaNotFoundException.class, () ->
                replicationService.recoverResource("res-001"));
    }

    // ---- 任务管理 ----

    @Test
    void getTaskReturnsTaskById() {
        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        SyncTask result = replicationService.getTask(1L);
        assertEquals(SyncTaskType.SYNC, result.getTaskType());
    }

    @Test
    void getTaskThrowsWhenNotFound() {
        when(taskRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ReplicationService.SyncTaskNotFoundException.class, () ->
                replicationService.getTask(999L));
    }

    @Test
    void pauseTaskCancelsPendingTask() {
        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        replicationService.pauseTask(1L);

        verify(taskRepo).updateStatus(1L, SyncTaskStatus.CANCELLED);
    }

    @Test
    void pauseTaskThrowsWhenNotPending() {
        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);
        task.setStatus(SyncTaskStatus.RUNNING);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(ReplicationService.SyncTaskAlreadyRunningException.class, () ->
                replicationService.pauseTask(1L));
    }

    @Test
    void resumeTaskResetsToPending() {
        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);
        task.setStatus(SyncTaskStatus.CANCELLED);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        replicationService.resumeTask(1L);

        verify(taskRepo).updateStatus(1L, SyncTaskStatus.PENDING);
    }

    // ---- 上传后自动同步 ----

    @Test
    void replicateAfterUploadCreatesSyncForNonPrimaryReplicas() {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid("res-001");
        resource.setMetadataUuid("md-001");
        resource.setProfileName("default");

        StorageReplica primary = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        StorageReplica backup = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);

        when(resourceRepo.findByMetadataUuid("md-001")).thenReturn(Optional.of(resource));
        when(replicaRepo.findByResourceUuid("res-001")).thenReturn(List.of(primary, backup));
        when(taskRepo.findActiveByResourceUuid("res-001")).thenReturn(List.of());
        when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        replicationService.replicateAfterUpload("md-001", "default");

        verify(taskRepo).save(argThat(t ->
                t.getTaskType() == SyncTaskType.SYNC
                && "res-001".equals(t.getResourceUuid())
                && "database".equals(t.getTargetProfile())
        ));
    }

    @Test
    void replicateAfterUploadSkipsWhenNoNonPrimaryReplicas() {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid("res-001");
        resource.setMetadataUuid("md-001");

        StorageReplica primary = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);

        when(resourceRepo.findByMetadataUuid("md-001")).thenReturn(Optional.of(resource));
        when(replicaRepo.findByResourceUuid("res-001")).thenReturn(List.of(primary));

        replicationService.replicateAfterUpload("md-001", "default");

        verify(taskRepo, never()).save(any());
    }
}