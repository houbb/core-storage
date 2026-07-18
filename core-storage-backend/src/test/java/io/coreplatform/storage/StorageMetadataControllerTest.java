package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageMetadataResponse;
import io.coreplatform.storage.api.response.StorageReferenceResponse;
import io.coreplatform.storage.application.service.StorageMetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorageMetadataController.class)
class StorageMetadataControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StorageMetadataService metadataService;

    @Test
    void getMetadataReturnsDetail() throws Exception {
        StorageMetadataResponse resp = new StorageMetadataResponse();
        resp.setUuid("uuid-001");
        resp.setOriginalName("test.png");
        resp.setMimeType("image/png");
        resp.setFileSize(2048L);
        resp.setStatus("ACTIVE");
        resp.setReferenceCount(2);
        resp.setCreateTime(LocalDateTime.now());

        when(metadataService.getByUuid("uuid-001")).thenReturn(resp);

        mvc.perform(get("/api/v1/storage/metadata/uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-001"))
                .andExpect(jsonPath("$.originalName").value("test.png"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.referenceCount").value(2));
    }

    @Test
    void getMetadataReturns404WhenNotFound() throws Exception {
        when(metadataService.getByUuid("not-exist"))
                .thenThrow(new StorageMetadataService.MetadataNotFoundException("Metadata not found: uuid=not-exist"));

        mvc.perform(get("/api/v1/storage/metadata/not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Metadata not found"));
    }

    @Test
    void searchReturnsPaginatedResults() throws Exception {
        StorageMetadataResponse item1 = new StorageMetadataResponse();
        item1.setUuid("uuid-001");
        item1.setOriginalName("file1.png");
        item1.setStatus("ACTIVE");

        SearchResultResponse<StorageMetadataResponse> result =
                new SearchResultResponse<>(List.of(item1), 1, 20, 1);

        when(metadataService.search(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(result);

        mvc.perform(get("/api/v1/storage/metadata/search")
                        .param("keyword", "file1")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].uuid").value("uuid-001"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void createReferenceReturnsJson() throws Exception {
        StorageReferenceResponse resp = new StorageReferenceResponse();
        resp.setId(100L);
        resp.setMetadataUuid("uuid-001");
        resp.setSystemName("core-user");
        resp.setBusinessType("user");
        resp.setBusinessId("1001");

        when(metadataService.createReference(eq("uuid-001"), eq("core-user"), eq("avatar"), eq("user"), eq("1001")))
                .thenReturn(resp);

        String body = """
                {
                    "metadataUuid": "uuid-001",
                    "system": "core-user",
                    "module": "avatar",
                    "businessType": "user",
                    "businessId": "1001"
                }
                """;

        mvc.perform(post("/api/v1/storage/reference")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.metadataUuid").value("uuid-001"))
                .andExpect(jsonPath("$.businessType").value("user"));
    }

    @Test
    void deleteReferenceReturns204() throws Exception {
        mvc.perform(delete("/api/v1/storage/reference/1")
                        .param("metadataUuid", "uuid-001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getReferencesReturnsList() throws Exception {
        StorageReferenceResponse ref1 = new StorageReferenceResponse();
        ref1.setId(1L);
        ref1.setMetadataUuid("uuid-001");
        ref1.setSystemName("core-user");
        ref1.setBusinessType("user");
        ref1.setBusinessId("1001");

        when(metadataService.getReferences("uuid-001")).thenReturn(List.of(ref1));

        mvc.perform(get("/api/v1/storage/metadata/uuid-001/references"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].systemName").value("core-user"))
                .andExpect(jsonPath("$[0].businessType").value("user"));
    }
}