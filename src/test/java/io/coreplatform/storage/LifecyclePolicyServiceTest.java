package io.coreplatform.storage;

import io.coreplatform.storage.application.service.LifecyclePolicyService;
import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.infrastructure.persistence.repository.LifecyclePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LifecyclePolicyServiceTest {

    private LifecyclePolicyService policyService;
    private LifecyclePolicyRepository policyRepo;

    @BeforeEach
    void setUp() {
        policyRepo = mock(LifecyclePolicyRepository.class);
        policyService = new LifecyclePolicyService(policyRepo);
    }

    // ---- 创建策略 ----

    @Test
    void createPolicySavesAndReturns() {
        when(policyRepo.existsByTypeAndCategory("DOCUMENT", "EXPORT")).thenReturn(false);
        when(policyRepo.save(any())).thenAnswer(inv -> {
            LifecyclePolicy p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        LifecyclePolicy result = policyService.createPolicy(
                "Export-7Days", "DOCUMENT", "EXPORT",
                0, 0, 0, 0, 7, "Export cleanup");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Export-7Days", result.getPolicyName());
        assertEquals("DOCUMENT", result.getResourceType());
        assertEquals("EXPORT", result.getCategory());
        assertEquals(7, result.getDeleteDays());
        assertTrue(result.isEnabled());
    }

    @Test
    void createDuplicatePolicyThrows() {
        when(policyRepo.existsByTypeAndCategory("IMAGE", "AVATAR")).thenReturn(true);

        assertThrows(LifecyclePolicyService.PolicyAlreadyExistsException.class,
                () -> policyService.createPolicy("Avatar-Permanent", "IMAGE", "AVATAR",
                        0, 0, 0, 0, 0, "Permanent"));
    }

    // ---- 查询 ----

    @Test
    void listPoliciesReturnsAll() {
        when(policyRepo.findAll()).thenReturn(List.of(
                buildPolicy(1L, "P1", "DOCUMENT", "EXPORT"),
                buildPolicy(2L, "P2", "IMAGE", "AVATAR")
        ));

        List<LifecyclePolicy> result = policyService.listPolicies();
        assertEquals(2, result.size());
    }

    @Test
    void getByIdReturnsPolicy() {
        when(policyRepo.findById(1L)).thenReturn(Optional.of(
                buildPolicy(1L, "P1", "DOCUMENT", "EXPORT")));

        LifecyclePolicy result = policyService.getById(1L);
        assertEquals("P1", result.getPolicyName());
    }

    @Test
    void getByIdNotFoundThrows() {
        when(policyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LifecyclePolicyService.PolicyNotFoundException.class,
                () -> policyService.getById(99L));
    }

    // ---- 更新 ----

    @Test
    void updatePolicyUpdatesFields() {
        LifecyclePolicy existing = buildPolicy(1L, "Old", "DOCUMENT", "EXPORT");
        when(policyRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(policyRepo.findById(1L)).thenReturn(Optional.of(existing));

        policyService.updatePolicy(1L, "New", null, null, null, null, 30, null, "Updated");
        verify(policyRepo).update(any());
    }

    @Test
    void updatePolicyNotFoundThrows() {
        when(policyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LifecyclePolicyService.PolicyNotFoundException.class,
                () -> policyService.updatePolicy(99L, "N", null, null, null, null, null, null, null));
    }

    // ---- 删除 ----

    @Test
    void deletePolicyRemoves() {
        when(policyRepo.findById(1L)).thenReturn(Optional.of(
                buildPolicy(1L, "P1", "OTHER", "OTHER")));

        policyService.deletePolicy(1L);
        verify(policyRepo).deleteById(1L);
    }

    // ---- helper ----

    private LifecyclePolicy buildPolicy(Long id, String name, String type, String category) {
        LifecyclePolicy p = new LifecyclePolicy();
        p.setId(id);
        p.setPolicyName(name);
        p.setResourceType(type);
        p.setCategory(category);
        p.setActiveDays(0);
        p.setWarmDays(0);
        p.setColdDays(0);
        p.setArchiveDays(0);
        p.setDeleteDays(0);
        p.setEnabled(true);
        return p;
    }
}