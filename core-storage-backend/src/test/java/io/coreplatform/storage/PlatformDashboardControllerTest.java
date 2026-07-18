package io.coreplatform.storage;

import io.coreplatform.storage.api.controller.PlatformDashboardController;
import io.coreplatform.storage.application.service.PlatformDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlatformDashboardController.class)
class PlatformDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformDashboardService dashboardService;

    @Test
    void getDashboardReturnsOk() throws Exception {
        var data = new PlatformDashboardService.DashboardData();
        data.setTotalResources(100);
        data.setActiveTenants(3);
        data.setTotalDrivers(2);
        data.setHealthyDrivers(2);
        data.setAuditsToday(50);
        data.setScansPending(5);
        data.setStageCounts(new LinkedHashMap<>());
        when(dashboardService.getDashboard()).thenReturn(data);

        mockMvc.perform(get("/api/v1/storage/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalResources").value(100))
                .andExpect(jsonPath("$.activeTenants").value(3))
                .andExpect(jsonPath("$.auditsToday").value(50))
                .andExpect(jsonPath("$.scansPending").value(5));
    }
}