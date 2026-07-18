package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageScanResponse;
import io.coreplatform.storage.application.domain.StorageScan;
import io.coreplatform.storage.application.domain.enums.ScanStatus;
import io.coreplatform.storage.application.domain.enums.ScanType;
import io.coreplatform.storage.application.service.StorageScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage/scan")
@Tag(name = "Enterprise Runtime — Content Scan", description = "内容安全扫描管理")
public class StorageScanController {

    private final StorageScanService scanService;

    public StorageScanController(StorageScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping
    @Operation(summary = "为资源触发新的扫描请求")
    public StorageScanResponse createScan(@RequestBody CreateScanRequest req) {
        ScanType scanType = safeEnum(ScanType.class, req.scanType(), ScanType.VIRUS);
        StorageScan scan = scanService.createScan(req.resourceUuid(), scanType);
        return toResponse(scan);
    }

    @GetMapping
    @Operation(summary = "搜索扫描记录")
    public SearchResultResponse<StorageScanResponse> search(
            @RequestParam(required = false) String resourceUuid,
            @RequestParam(required = false) String scanType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<StorageScanResponse> items = scanService.search(resourceUuid, scanType, status, page, size)
                .stream().map(this::toResponse).toList();
        int total = scanService.countSearch(resourceUuid, scanType, status);

        return new SearchResultResponse<>(items, page, size, total);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询扫描详情")
    public StorageScanResponse getScan(@PathVariable Long id) {
        return toResponse(scanService.getScan(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新扫描结果（由外部扫描器调用）")
    public StorageScanResponse updateScan(@PathVariable Long id,
                                           @RequestBody UpdateScanRequest req) {
        ScanStatus status = safeEnum(ScanStatus.class, req.status(), ScanStatus.CLEAN);
        scanService.updateScanResult(id, status, req.resultMessage());
        return toResponse(scanService.getScan(id));
    }

    // ─── helper mappers ───

    private StorageScanResponse toResponse(StorageScan s) {
        StorageScanResponse r = new StorageScanResponse();
        r.setId(s.getId());
        r.setResourceUuid(s.getResourceUuid());
        r.setScanType(s.getScanType() != null ? s.getScanType().name() : null);
        r.setStatus(s.getStatus() != null ? s.getStatus().name() : null);
        r.setResultMessage(s.getResultMessage());
        r.setScanTime(s.getScanTime());
        r.setCreateTime(s.getCreateTime());
        return r;
    }

    private static <E extends Enum<E>> E safeEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    // ─── request DTOs ───

    public record CreateScanRequest(String resourceUuid, String scanType) {}

    public record UpdateScanRequest(String status, String resultMessage) {}
}