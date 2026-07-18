package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageAuditResponse;
import io.coreplatform.storage.application.domain.StorageAudit;
import io.coreplatform.storage.application.service.StorageAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage/audit")
@Tag(name = "Enterprise Runtime — Audit", description = "统一审计日志查询")
public class StorageAuditController {

    private final StorageAuditService auditService;

    public StorageAuditController(StorageAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "多条件搜索审计日志")
    public SearchResultResponse<StorageAuditResponse> search(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String resourceUuid,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operatorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<StorageAuditResponse> items = auditService.search(tenantId, resourceUuid, action, operatorId, page, size)
                .stream().map(this::toResponse).toList();
        int total = auditService.countSearch(tenantId, resourceUuid, action, operatorId);

        return new SearchResultResponse<>(items, page, size, total);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询审计日志详情")
    public StorageAuditResponse getAudit(@PathVariable Long id) {
        return toResponse(auditService.getAudit(id));
    }

    // ─── helper mappers ───

    private StorageAuditResponse toResponse(StorageAudit a) {
        StorageAuditResponse r = new StorageAuditResponse();
        r.setId(a.getId());
        r.setTenantId(a.getTenantId());
        r.setResourceUuid(a.getResourceUuid());
        r.setOperatorId(a.getOperatorId());
        r.setAction(a.getAction() != null ? a.getAction().name() : null);
        r.setTarget(a.getTarget());
        r.setResult(a.getResult());
        r.setDetail(a.getDetail());
        r.setClientIp(a.getClientIp());
        r.setCreateTime(a.getCreateTime());
        return r;
    }
}