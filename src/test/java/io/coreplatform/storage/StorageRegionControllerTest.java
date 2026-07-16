package io.coreplatform.storage;

import io.coreplatform.storage.api.controller.StorageRegionController;
import io.coreplatform.storage.application.domain.StorageRegion;
import io.coreplatform.storage.application.service.StorageRegionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorageRegionController.class)
class StorageRegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageRegionService regionService;

    @Test
    void listRegionsReturnsOk() throws Exception {
        when(regionService.listRegions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/storage/regions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void createRegionReturnsOk() throws Exception {
        var region = new StorageRegion();
        region.setRegionCode("cn");
        region.setRegionName("China");
        region.setEndpoint("https://oss.cn");
        region.setDriverName("oss");
        when(regionService.createRegion(anyString(), anyString(), anyString(), anyString())).thenReturn(region);

        String body = "{\"regionCode\":\"cn\",\"regionName\":\"China\",\"endpoint\":\"https://oss.cn\",\"driverName\":\"oss\"}";

        mockMvc.perform(post("/api/v1/storage/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionCode").value("cn"));
    }

    @Test
    void getRegionReturnsOk() throws Exception {
        var region = new StorageRegion();
        region.setRegionCode("cn");
        region.setRegionName("China");
        when(regionService.getRegion("cn")).thenReturn(region);

        mockMvc.perform(get("/api/v1/storage/regions/cn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionCode").value("cn"));
    }

    @Test
    void deleteRegionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/storage/regions/cn"))
                .andExpect(status().isNoContent());
    }
}