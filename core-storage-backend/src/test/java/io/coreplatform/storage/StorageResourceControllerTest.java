package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageResourceResponse;
import io.coreplatform.storage.application.service.StorageResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorageResourceController.class)
class StorageResourceControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StorageResourceService resourceService;

    @Test
    void getResourceReturnsDetail() throws Exception {
        StorageResourceResponse resp = new StorageResourceResponse();
        resp.setResourceUuid("res-uuid-001");
        resp.setMetadataUuid("md-uuid-001");
        resp.setResourceName("avatar.png");
        resp.setResourceType("IMAGE");
        resp.setCategory("AVATAR");
        resp.setVisibility("PUBLIC");
        resp.setStatus("READY");
        resp.setReferenceCount(5);

        when(resourceService.getByUuid("res-uuid-001")).thenReturn(resp);

        mvc.perform(get("/api/v1/storage/resources/res-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceUuid").value("res-uuid-001"))
                .andExpect(jsonPath("$.resourceName").value("avatar.png"))
                .andExpect(jsonPath("$.resourceType").value("IMAGE"))
                .andExpect(jsonPath("$.category").value("AVATAR"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.referenceCount").value(5));
    }

    @Test
    void searchReturnsPaginatedResults() throws Exception {
        StorageResourceResponse item = new StorageResourceResponse();
        item.setResourceUuid("res-uuid-001");
        item.setResourceName("avatar.png");
        item.setResourceType("IMAGE");
        item.setStatus("READY");

        SearchResultResponse<StorageResourceResponse> result =
                new SearchResultResponse<>(List.of(item), 1, 20, 1);

        when(resourceService.search(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), anyInt(), anyInt()))
                .thenReturn(result);

        mvc.perform(get("/api/v1/storage/resources/search")
                        .param("keyword", "avatar")
                        .param("resourceType", "IMAGE")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].resourceUuid").value("res-uuid-001"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void updateResourceReturnsUpdated() throws Exception {
        StorageResourceResponse resp = new StorageResourceResponse();
        resp.setResourceUuid("res-uuid-001");
        resp.setResourceName("new-avatar.png");
        resp.setCategory("LOGO");
        resp.setVisibility("LOGIN");

        when(resourceService.update(eq("res-uuid-001"), eq("new-avatar.png"),
                eq("new desc"), eq("LOGO"), eq("LOGIN"), isNull(), anyList()))
                .thenReturn(resp);

        String body = """
                {
                    "resourceName": "new-avatar.png",
                    "description": "new desc",
                    "category": "LOGO",
                    "visibility": "LOGIN",
                    "tags": ["cool", "new"]
                }
                """;

        mvc.perform(put("/api/v1/storage/resources/res-uuid-001")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceName").value("new-avatar.png"))
                .andExpect(jsonPath("$.category").value("LOGO"));
    }

    @Test
    void deleteResourceReturns204() throws Exception {
        mvc.perform(delete("/api/v1/storage/resources/res-uuid-001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPropertiesReturnsMap() throws Exception {
        when(resourceService.getProperties("res-uuid-001"))
                .thenReturn(Map.of("width", "1024", "height", "768"));

        mvc.perform(get("/api/v1/storage/resources/res-uuid-001/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").value("1024"))
                .andExpect(jsonPath("$.height").value("768"));
    }

    @Test
    void setPropertiesReturns200() throws Exception {
        String body = """
                {
                    "width": "1920",
                    "height": "1080"
                }
                """;

        mvc.perform(put("/api/v1/storage/resources/res-uuid-001/properties")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }
}