package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageDriverResponse;
import io.coreplatform.storage.application.domain.StorageDriverInfo;
import io.coreplatform.storage.application.domain.enums.DriverHealth;
import io.coreplatform.storage.application.service.StorageDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage/drivers")
@Tag(name = "Storage Driver Runtime", description = "存储驱动管理（驱动列表、详情、健康检查）")
public class StorageDriverController {

    private final StorageDriverService driverService;

    public StorageDriverController(StorageDriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    @Operation(summary = "列出所有已注册的存储驱动")
    public List<StorageDriverResponse> listDrivers() {
        return driverService.listDrivers().stream()
                .map(StorageDriverController::toResponse)
                .toList();
    }

    @GetMapping("/{name}")
    @Operation(summary = "获取驱动详情")
    public StorageDriverResponse getDriver(@PathVariable String name) {
        return toResponse(driverService.getDriver(name));
    }

    @GetMapping("/{name}/health")
    @Operation(summary = "驱动健康检查")
    public Map<String, Object> health(@PathVariable String name) {
        DriverHealth health = driverService.checkHealth(name);
        return Map.of(
                "driverName", name,
                "healthy", health == DriverHealth.HEALTHY,
                "status", health.name()
        );
    }

    // --- helpers ---

    static StorageDriverResponse toResponse(StorageDriverInfo info) {
        StorageDriverResponse resp = new StorageDriverResponse();
        resp.setId(info.getId());
        resp.setDriverName(info.getDriverName());
        resp.setDriverType(info.getDriverType() != null ? info.getDriverType().name() : null);
        resp.setVersion(info.getVersion());
        resp.setEnabled(info.isEnabled());
        resp.setStatus(info.getStatus() != null ? info.getStatus().name() : null);
        resp.setHealthStatus(info.getHealthStatus() != null ? info.getHealthStatus().name() : null);
        resp.setCapabilities(null); // populated by caller if needed
        resp.setCreateTime(info.getCreateTime());
        return resp;
    }
}
