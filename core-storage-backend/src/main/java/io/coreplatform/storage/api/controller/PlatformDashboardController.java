package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.PlatformDashboardResponse;
import io.coreplatform.storage.application.service.PlatformDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/storage/dashboard")
@Tag(name = "Enterprise Runtime — Dashboard", description = "企业资源仪表板")
public class PlatformDashboardController {

    private final PlatformDashboardService dashboardService;

    public PlatformDashboardController(PlatformDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "获取平台仪表板数据（资源统计、驱动健康、审计概要等）")
    public PlatformDashboardResponse getDashboard() {
        PlatformDashboardService.DashboardData data = dashboardService.getDashboard();
        return toResponse(data);
    }

    private PlatformDashboardResponse toResponse(PlatformDashboardService.DashboardData data) {
        PlatformDashboardResponse r = new PlatformDashboardResponse();
        r.setTotalResources(data.getTotalResources());
        r.setActiveTenants(data.getActiveTenants());
        r.setTotalDrivers(data.getTotalDrivers());
        r.setHealthyDrivers(data.getHealthyDrivers());
        r.setTotalReplicas(data.getTotalReplicas());
        r.setHealthyReplicas(data.getHealthyReplicas());
        r.setAuditsToday(data.getAuditsToday());
        r.setScansPending(data.getScansPending());
        r.setStageCounts(data.getStageCounts());
        return r;
    }
}