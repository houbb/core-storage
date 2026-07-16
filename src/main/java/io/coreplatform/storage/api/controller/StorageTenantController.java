package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageTenantResponse;
import io.coreplatform.storage.application.domain.StorageTenant;
import io.coreplatform.storage.application.domain.enums.TenantStatus;
import io.coreplatform.storage.application.service.StorageTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage/tenants")
@Tag(name = "Enterprise Runtime — Tenant", description = "多租户管理（创建、查询、激活、暂停、删除）")
public class StorageTenantController {

    private final StorageTenantService tenantService;

    public StorageTenantController(StorageTenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @Operation(summary = "查询所有租户")
    public List<StorageTenantResponse> listTenants() {
        return tenantService.listTenants().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @Operation(summary = "创建租户")
    public StorageTenantResponse createTenant(@RequestBody CreateTenantRequest req) {
        StorageTenant tenant = tenantService.createTenant(req.tenantId(), req.tenantName());
        return toResponse(tenant);
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "查询租户详情")
    public StorageTenantResponse getTenant(@PathVariable String tenantId) {
        return toResponse(tenantService.getTenant(tenantId));
    }

    @PutMapping("/{tenantId}")
    @Operation(summary = "更新租户信息（名称/状态）")
    public StorageTenantResponse updateTenant(@PathVariable String tenantId,
                                               @RequestBody UpdateTenantRequest req) {
        TenantStatus status = null;
        if (req.status() != null && !req.status().isBlank()) {
            status = safeEnum(TenantStatus.class, req.status(), TenantStatus.ACTIVE);
        }
        StorageTenant tenant = tenantService.updateTenant(tenantId, req.tenantName(), status);
        return toResponse(tenant);
    }

    @DeleteMapping("/{tenantId}")
    @Operation(summary = "删除租户（软删除：状态设为 DELETED）")
    public ResponseEntity<Void> deleteTenant(@PathVariable String tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    // ─── helper mappers ───

    private StorageTenantResponse toResponse(StorageTenant t) {
        StorageTenantResponse r = new StorageTenantResponse();
        r.setTenantId(t.getTenantId());
        r.setTenantName(t.getTenantName());
        r.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        r.setCreateTime(t.getCreateTime());
        r.setUpdateTime(t.getUpdateTime());
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

    public record CreateTenantRequest(String tenantId, String tenantName) {}

    public record UpdateTenantRequest(String tenantName, String status) {}
}