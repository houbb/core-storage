package io.coreplatform.storage;

import io.coreplatform.storage.api.controller.StorageAuditController;
import io.coreplatform.storage.application.service.StorageAuditService;
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

@WebMvcTest(StorageAuditController.class)
class StorageAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageAuditService auditService;

    @Test
    void searchAuditsReturnsOk() throws Exception {
        when(auditService.search(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(auditService.countSearch(anyString(), anyString(), anyString(), anyString())).thenReturn(0);

        mockMvc.perform(get("/api/v1/storage/audit"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}