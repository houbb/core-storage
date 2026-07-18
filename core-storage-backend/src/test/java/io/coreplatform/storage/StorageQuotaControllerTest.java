package io.coreplatform.storage;

import io.coreplatform.storage.api.controller.StorageQuotaController;
import io.coreplatform.storage.application.domain.StorageQuota;
import io.coreplatform.storage.application.service.StorageQuotaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorageQuotaController.class)
class StorageQuotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageQuotaService quotaService;

    @Test
    void listAllQuotasReturnsOk() throws Exception {
        when(quotaService.listAllQuotas()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/storage/quotas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getQuotaReturnsOk() throws Exception {
        var quota = new StorageQuota();
        quota.setId(1L);
        quota.setTenantId("t1");
        quota.setResourceType("*");
        quota.setLimitSize(1024);
        quota.setUsedSize(100);
        when(quotaService.getQuota("t1", "*")).thenReturn(quota);

        mockMvc.perform(get("/api/v1/storage/quota?tenantId=t1&resourceType=*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("t1"))
                .andExpect(jsonPath("$.limitSize").value(1024))
                .andExpect(jsonPath("$.usedSize").value(100));
    }
}