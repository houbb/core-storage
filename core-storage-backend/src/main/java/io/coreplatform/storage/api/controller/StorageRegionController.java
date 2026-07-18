package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageRegionResponse;
import io.coreplatform.storage.application.domain.StorageRegion;
import io.coreplatform.storage.application.service.StorageRegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage/regions")
@Tag(name = "Enterprise Runtime — Region", description = "区域管理（多区域存储驱动绑定）")
public class StorageRegionController {

    private final StorageRegionService regionService;

    public StorageRegionController(StorageRegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    @Operation(summary = "查询所有区域")
    public List<StorageRegionResponse> listRegions() {
        return regionService.listRegions().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @Operation(summary = "创建区域")
    public StorageRegionResponse createRegion(@RequestBody CreateRegionRequest req) {
        StorageRegion region = regionService.createRegion(
                req.regionCode(), req.regionName(), req.endpoint(), req.driverName());
        return toResponse(region);
    }

    @GetMapping("/{regionCode}")
    @Operation(summary = "查询区域详情")
    public StorageRegionResponse getRegion(@PathVariable String regionCode) {
        return toResponse(regionService.getRegion(regionCode));
    }

    @PutMapping("/{regionCode}")
    @Operation(summary = "更新区域信息")
    public StorageRegionResponse updateRegion(@PathVariable String regionCode,
                                               @RequestBody UpdateRegionRequest req) {
        StorageRegion region = regionService.updateRegion(
                regionCode, req.regionName(), req.endpoint(), req.driverName());
        return toResponse(region);
    }

    @DeleteMapping("/{regionCode}")
    @Operation(summary = "删除区域")
    public ResponseEntity<Void> deleteRegion(@PathVariable String regionCode) {
        regionService.deleteRegion(regionCode);
        return ResponseEntity.noContent().build();
    }

    // ─── helper mappers ───

    private StorageRegionResponse toResponse(StorageRegion r) {
        StorageRegionResponse resp = new StorageRegionResponse();
        resp.setRegionCode(r.getRegionCode());
        resp.setRegionName(r.getRegionName());
        resp.setEndpoint(r.getEndpoint());
        resp.setDriverName(r.getDriverName());
        resp.setCreateTime(r.getCreateTime());
        return resp;
    }

    // ─── request DTOs ───

    public record CreateRegionRequest(String regionCode, String regionName, String endpoint, String driverName) {}

    public record UpdateRegionRequest(String regionName, String endpoint, String driverName) {}
}