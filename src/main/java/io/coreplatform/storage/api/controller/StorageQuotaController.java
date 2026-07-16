package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageQuotaResponse;
import io.coreplatform.storage.application.domain.StorageQuota;
import io.coreplatform.storage.application.service.StorageQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Enterprise Runtime — Quota", description = "配额管理（设置、查询、校验）")
public class StorageQuotaController {

    private final StorageQuotaService quotaService;

    public StorageQuotaController(StorageQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping("/quotas")
    @Operation(summary = "查询所有配额")
    public List<StorageQuotaResponse> listAllQuotas() {
        return quotaService.listAllQuotas().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/quotas/{tenantId}")
    @Operation(summary = "查询租户的所有配额")
    public List<StorageQuotaResponse> getQuotas(@PathVariable String tenantId) {
        return quotaService.getQuotas(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/quota")
    @Operation(summary = "查询租户特定资源类型的配额")
    public StorageQuotaResponse getQuota(@RequestParam String tenantId,
                                          @RequestParam(defaultValue = "*") String resourceType) {
        return toResponse(quotaService.getQuota(tenantId, resourceType));
    }

    @PutMapping("/quota")
    @Operation(summary = "设置或更新配额")
    public StorageQuotaResponse setQuota(@RequestBody SetQuotaRequest req) {
        String resourceType = req.resourceType() != null ? req.resourceType() : "*";
        StorageQuota quota = quotaService.setQuota(req.tenantId(), resourceType, req.limitSize());
        return toResponse(quota);
    }

    // ─── helper mappers ───

    private StorageQuotaResponse toResponse(StorageQuota q) {
        StorageQuotaResponse r = new StorageQuotaResponse();
        r.setId(q.getId());
        r.setTenantId(q.getTenantId());
        r.setResourceType(q.getResourceType());
        r.setLimitSize(q.getLimitSize());
        r.setUsedSize(q.getUsedSize());
        r.setRemainingSize(q.remainingBytes());
        r.setCreateTime(q.getCreateTime());
        r.setUpdateTime(q.getUpdateTime());
        return r;
    }

    // ─── request DTOs ───

    public record SetQuotaRequest(String tenantId, String resourceType, long limitSize) {}
}