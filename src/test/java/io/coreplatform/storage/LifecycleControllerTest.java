package io.coreplatform.storage;

import io.coreplatform.storage.api.controller.LifecycleController;
import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.application.service.LifecycleEngine;
import io.coreplatform.storage.application.service.LifecyclePolicyService;
import io.coreplatform.storage.application.service.ResourceHoldService;
import io.coreplatform.storage.infrastructure.persistence.repository.LifecycleTaskRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LifecycleController.class)
class LifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LifecyclePolicyService policyService;

    @MockBean
    private LifecycleEngine engine;

    @MockBean
    private ResourceHoldService holdService;

    @MockBean
    private LifecycleTaskRepository taskRepo;

    @MockBean
    private StorageResourceRepository resourceRepo;

    @Test
    void listPoliciesReturnsOk() throws Exception {
        when(policyService.listPolicies()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/storage/lifecycle/policies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void createPolicyReturnsCreated() throws Exception {
        var policy = new LifecyclePolicy();
        policy.setId(1L);
        policy.setPolicyName("Export-7D");
        policy.setResourceType("DOCUMENT");
        policy.setCategory("EXPORT");
        when(policyService.createPolicy(anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any())).thenReturn(policy);

        String body = """
                {
                    "policyName": "Export-7D",
                    "resourceType": "DOCUMENT",
                    "category": "EXPORT",
                    "deleteDays": 7
                }""";

        mockMvc.perform(post("/api/v1/storage/lifecycle/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyName").value("Export-7D"));
    }

    @Test
    void deletePolicyReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/storage/lifecycle/policies/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getDashboardReturnsOk() throws Exception {
        when(resourceRepo.countByLifecycleStage(anyString())).thenReturn(0);
        when(holdService.countActiveHolds()).thenReturn(0);
        when(taskRepo.countSearch(anyString(), any())).thenReturn(0);
        when(policyService.listPolicies()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/storage/lifecycle/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeHolds").value(0));
    }
}