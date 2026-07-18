package io.coreplatform.storage;

import io.coreplatform.storage.api.controller.StorageTenantController;
import io.coreplatform.storage.application.domain.StorageTenant;
import io.coreplatform.storage.application.domain.enums.TenantStatus;
import io.coreplatform.storage.application.service.StorageTenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorageTenantController.class)
class StorageTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageTenantService tenantService;

    @Test
    void listTenantsReturnsOk() throws Exception {
        when(tenantService.listTenants()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/storage/tenants"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void createTenantReturnsCreated() throws Exception {
        var tenant = new StorageTenant();
        tenant.setTenantId("t1");
        tenant.setTenantName("Tenant One");
        tenant.setStatus(TenantStatus.ACTIVE);
        when(tenantService.createTenant(anyString(), anyString())).thenReturn(tenant);

        String body = "{\"tenantId\":\"t1\",\"tenantName\":\"Tenant One\"}";

        mockMvc.perform(post("/api/v1/storage/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("t1"))
                .andExpect(jsonPath("$.tenantName").value("Tenant One"));
    }

    @Test
    void getTenantReturnsOk() throws Exception {
        var tenant = new StorageTenant();
        tenant.setTenantId("t1");
        tenant.setTenantName("Tenant One");
        tenant.setStatus(TenantStatus.ACTIVE);
        when(tenantService.getTenant("t1")).thenReturn(tenant);

        mockMvc.perform(get("/api/v1/storage/tenants/t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("t1"));
    }

    @Test
    void deleteTenantReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/storage/tenants/t1"))
                .andExpect(status().isNoContent());
    }
}