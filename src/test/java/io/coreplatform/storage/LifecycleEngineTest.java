package io.coreplatform.storage;

import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.application.domain.LifecycleTask;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.enums.*;
import io.coreplatform.storage.application.service.LifecycleEngine;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LifecycleEngineTest {

    private LifecycleEngine engine;
    private LifecyclePolicyRepository policyRepo;
    private LifecycleTaskRepository taskRepo;
    private StorageResourceRepository resourceRepo;
    private StorageReferenceRepository referenceRepo;
    private ResourceHoldRepository holdRepo;

    @BeforeEach
    void setUp() {
        policyRepo = mock(LifecyclePolicyRepository.class);
        taskRepo = mock(LifecycleTaskRepository.class);
        resourceRepo = mock(StorageResourceRepository.class);
        referenceRepo = mock(StorageReferenceRepository.class);
        holdRepo = mock(ResourceHoldRepository.class);
        engine = new LifecycleEngine(policyRepo, taskRepo, resourceRepo, referenceRepo, holdRepo);
    }

    // ---- 评估资源 ----

    @Test
    void evaluateResourceNoPolicyReturnsNull() {
        StorageResource resource = buildResource("res-001", ResourceType.IMAGE, ResourceCategory.AVATAR, LifecycleStage.ACTIVE);
        when(policyRepo.findByTypeAndCategory("IMAGE", "AVATAR")).thenReturn(Optional.empty());

        LifecycleTask task = engine.evaluateResource(resource);
        assertNull(task);
    }

    @Test
    void evaluateResourceAlreadyInCorrectStageReturnsNull() {
        StorageResource resource = buildResource("res-001", ResourceType.DOCUMENT, ResourceCategory.OTHER, LifecycleStage.ACTIVE);
        LifecyclePolicy policy = buildPolicy(1L, "Doc-30Days", "DOCUMENT", "OTHER", 0, 0, 0, 0, 30);
        when(policyRepo.findByTypeAndCategory("DOCUMENT", "OTHER")).thenReturn(Optional.of(policy));

        LifecycleTask task = engine.evaluateResource(resource);
        assertNull(task); // 刚创建，未到 30 天
    }

    @Test
    void evaluateResourceNeedsTransitionCreatesTask() {
        StorageResource resource = buildResource("res-002", ResourceType.DOCUMENT, ResourceCategory.OTHER, LifecycleStage.ACTIVE);
        // 创建时间设为 40 天前
        resource.setCreateTime(LocalDateTime.now().minusDays(40));

        LifecyclePolicy policy = buildPolicy(1L, "Doc-30Days", "DOCUMENT", "OTHER", 0, 0, 0, 0, 30);
        when(policyRepo.findByTypeAndCategory("DOCUMENT", "OTHER")).thenReturn(Optional.of(policy));
        when(taskRepo.findActiveByResourceUuid("res-002")).thenReturn(Optional.empty());
        when(holdRepo.hasActiveHold("res-002")).thenReturn(false);
        when(taskRepo.save(any())).thenAnswer(inv -> {
            LifecycleTask t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        LifecycleTask task = engine.evaluateResource(resource);
        assertNotNull(task);
        assertEquals(LifecycleAction.DELETE, task.getAction());
        assertEquals("DELETED", task.getTargetStage());
    }

    @Test
    void evaluateResourceUnderHoldReturnsNull() {
        StorageResource resource = buildResource("res-003", ResourceType.DOCUMENT, ResourceCategory.OTHER, LifecycleStage.ACTIVE);
        resource.setCreateTime(LocalDateTime.now().minusDays(40));

        LifecyclePolicy policy = buildPolicy(1L, "Doc-30Days", "DOCUMENT", "OTHER", 0, 0, 0, 0, 30);
        when(policyRepo.findByTypeAndCategory("DOCUMENT", "OTHER")).thenReturn(Optional.of(policy));
        when(holdRepo.hasActiveHold("res-003")).thenReturn(true);

        LifecycleTask task = engine.evaluateResource(resource);
        assertNull(task);
    }

    // ---- 执行任务 ----

    @Test
    void executeDeleteActionChecksReferences() {
        LifecycleTask task = LifecycleTask.create("res-004", 1L, LifecycleAction.DELETE, "DELETED");
        task.setId(1L);

        when(holdRepo.hasActiveHold("res-004")).thenReturn(false);
        StorageResource resource = buildResource("res-004", ResourceType.OTHER, ResourceCategory.OTHER, LifecycleStage.ACTIVE);
        when(resourceRepo.findByResourceUuid("res-004")).thenReturn(Optional.of(resource));
        when(referenceRepo.countByMetadataUuid(resource.getMetadataUuid())).thenReturn(0);

        engine.executeTask(task);

        verify(resourceRepo).updateLifecycleStage("res-004", "DELETED");
        verify(resourceRepo).updateStatus("res-004", "DELETED");
        verify(taskRepo).markCompleted(1L);
    }

    // ---- 手动操作 ----

    @Test
    void triggerLifecycleNotFoundThrows() {
        when(resourceRepo.findByResourceUuid("not-exist")).thenReturn(Optional.empty());

        assertThrows(StorageResourceService.ResourceNotFoundException.class,
                () -> engine.triggerLifecycle("not-exist"));
    }

    // ---- helpers ----

    private StorageResource buildResource(String uuid, ResourceType type, ResourceCategory category, LifecycleStage stage) {
        StorageResource r = new StorageResource();
        r.setId(1L);
        r.setResourceUuid(uuid);
        r.setMetadataUuid("md-" + uuid);
        r.setResourceName("test");
        r.setResourceType(type);
        r.setCategory(category);
        r.setLifecycleStage(stage);
        r.setStatus(ResourceStatus.READY);
        r.setCreateTime(LocalDateTime.now().minusDays(1));
        return r;
    }

    private LifecyclePolicy buildPolicy(Long id, String name, String type, String category,
                                          int activeDays, int warmDays, int coldDays,
                                          int archiveDays, int deleteDays) {
        LifecyclePolicy p = new LifecyclePolicy();
        p.setId(id);
        p.setPolicyName(name);
        p.setResourceType(type);
        p.setCategory(category);
        p.setActiveDays(activeDays);
        p.setWarmDays(warmDays);
        p.setColdDays(coldDays);
        p.setArchiveDays(archiveDays);
        p.setDeleteDays(deleteDays);
        p.setEnabled(true);
        return p;
    }
}