package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageVersionAliasResponse;
import io.coreplatform.storage.api.response.StorageVersionHistoryResponse;
import io.coreplatform.storage.api.response.StorageVersionResponse;
import io.coreplatform.storage.api.security.AccessContext;
import io.coreplatform.storage.application.domain.StorageVersion;
import io.coreplatform.storage.application.domain.StorageVersionAlias;
import io.coreplatform.storage.application.domain.StorageVersionHistory;
import io.coreplatform.storage.application.service.StorageVersionService;
import io.coreplatform.storage.application.service.StorageVersionService.VersionCompareResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P7 Version Runtime — 统一资源版本管理 API。
 * <p>
 * 覆盖版本 CRUD、发布/回滚、别名管理、历史审计、版本比较。
 */
@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Version Runtime", description = "统一资源版本管理（创建、发布、回滚、别名、历史、比较）")
public class StorageVersionController {

    private final StorageVersionService versionService;
    private final AccessContext accessContext;

    public StorageVersionController(StorageVersionService versionService,
                                     AccessContext accessContext) {
        this.versionService = versionService;
        this.accessContext = accessContext;
    }

    // ---- Resource-scoped version operations ----

    @GetMapping("/resources/{uuid}/versions")
    @Operation(summary = "列出资源的所有版本")
    public List<StorageVersionResponse> listVersions(@PathVariable String uuid) {
        return versionService.listVersions(uuid).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @GetMapping("/resources/{uuid}/versions/latest")
    @Operation(summary = "获取资源的最新版本")
    public StorageVersionResponse getLatestVersion(@PathVariable String uuid) {
        return toVersionResponse(versionService.getLatestVersion(uuid));
    }

    @PostMapping("/resources/{uuid}/versions")
    @Operation(summary = "为资源创建新版本（Draft）")
    public StorageVersionResponse createVersion(@PathVariable String uuid,
                                                 @RequestBody CreateVersionRequest request) {
        StorageVersion v = versionService.createNewVersion(uuid, request.metadataUuid(),
                request.checksum(), request.versionName(), getOperatorId());
        return toVersionResponse(v);
    }

    @GetMapping("/resources/{uuid}/versions/history")
    @Operation(summary = "获取资源的版本操作历史（分页）")
    public SearchResultResponse<StorageVersionHistoryResponse> getResourceHistory(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<StorageVersionHistoryResponse> items = versionService.getResourceHistory(uuid, page, size).stream()
                .map(this::toHistoryResponse)
                .toList();
        int total = versionService.countHistory(uuid);
        return new SearchResultResponse<>(items, page, size, total);
    }

    // ---- Version-scoped operations ----

    @GetMapping("/versions/{uuid}")
    @Operation(summary = "获取版本详情")
    public StorageVersionResponse getVersion(@PathVariable String uuid) {
        return toVersionResponse(versionService.getVersion(uuid));
    }

    @PostMapping("/versions/{uuid}/publish")
    @Operation(summary = "发布版本（切换 Latest Pointer）")
    public StorageVersionResponse publishVersion(@PathVariable String uuid) {
        return toVersionResponse(versionService.publish(uuid, getOperatorId()));
    }

    @PostMapping("/versions/{uuid}/rollback")
    @Operation(summary = "回滚到指定版本（切换 Latest Pointer）")
    public StorageVersionResponse rollbackVersion(@PathVariable String uuid,
                                                   @RequestBody(required = false) RollbackRequest request) {
        // Resolve resourceUuid from the version itself
        StorageVersion version = versionService.getVersion(uuid);
        return toVersionResponse(versionService.rollback(version.getResourceUuid(), uuid, getOperatorId()));
    }

    @PostMapping("/versions/{uuid}/deprecate")
    @Operation(summary = "标记版本为不推荐")
    public StorageVersionResponse deprecateVersion(@PathVariable String uuid) {
        return toVersionResponse(versionService.deprecate(uuid, getOperatorId()));
    }

    @PostMapping("/versions/{uuid}/archive")
    @Operation(summary = "归档版本")
    public StorageVersionResponse archiveVersion(@PathVariable String uuid) {
        return toVersionResponse(versionService.archive(uuid, getOperatorId()));
    }

    @DeleteMapping("/versions/{uuid}")
    @Operation(summary = "删除版本（不能删除 latest 版本）")
    public ResponseEntity<Void> deleteVersion(@PathVariable String uuid) {
        versionService.deleteVersion(uuid, getOperatorId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/versions/{v1}/compare/{v2}")
    @Operation(summary = "比较两个版本")
    public VersionCompareResult compareVersions(@PathVariable String v1,
                                                 @PathVariable String v2) {
        return versionService.compare(v1, v2);
    }

    @GetMapping("/versions/{uuid}/history")
    @Operation(summary = "获取版本的操作历史")
    public List<StorageVersionHistoryResponse> getVersionHistory(@PathVariable String uuid) {
        return versionService.getHistory(uuid).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    // ---- Alias operations ----

    @PostMapping("/versions/{uuid}/aliases")
    @Operation(summary = "为版本设置别名（如 stable、beta、lts）")
    public StorageVersionAliasResponse setAlias(@PathVariable String uuid,
                                                 @RequestBody SetAliasRequest request) {
        StorageVersionAlias alias = versionService.setAlias(uuid, request.aliasName());
        return toAliasResponse(alias);
    }

    @DeleteMapping("/resources/{uuid}/aliases/{aliasName}")
    @Operation(summary = "移除资源的别名")
    public ResponseEntity<Void> removeAlias(@PathVariable String uuid,
                                             @PathVariable String aliasName) {
        versionService.removeAlias(uuid, aliasName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resources/{uuid}/aliases")
    @Operation(summary = "列出资源的所有别名")
    public List<StorageVersionAliasResponse> listAliases(@PathVariable String uuid) {
        return versionService.listAliases(uuid).stream()
                .map(this::toAliasResponse)
                .toList();
    }

    // ---- helpers ----

    private String getOperatorId() {
        String userId = accessContext.getUserId();
        return userId != null ? userId : "system";
    }

    private StorageVersionResponse toVersionResponse(StorageVersion v) {
        StorageVersionResponse r = new StorageVersionResponse();
        r.setVersionUuid(v.getVersionUuid());
        r.setResourceUuid(v.getResourceUuid());
        r.setMetadataUuid(v.getMetadataUuid());
        r.setVersionName(v.getVersionName());
        r.setVersionCode(v.getVersionCode());
        r.setStatus(v.getStatus() != null ? v.getStatus().name() : null);
        r.setPublished(v.isPublished());
        r.setLatest(v.isLatest());
        r.setChecksum(v.getChecksum());
        r.setCreateTime(v.getCreateTime());
        r.setPublishTime(v.getPublishTime());
        return r;
    }

    private StorageVersionAliasResponse toAliasResponse(StorageVersionAlias a) {
        StorageVersionAliasResponse r = new StorageVersionAliasResponse();
        r.setVersionUuid(a.getVersionUuid());
        r.setResourceUuid(a.getResourceUuid());
        r.setAliasName(a.getAliasName());
        r.setCreateTime(a.getCreateTime());
        return r;
    }

    private StorageVersionHistoryResponse toHistoryResponse(StorageVersionHistory h) {
        StorageVersionHistoryResponse r = new StorageVersionHistoryResponse();
        r.setVersionUuid(h.getVersionUuid());
        r.setResourceUuid(h.getResourceUuid());
        r.setAction(h.getAction() != null ? h.getAction().name() : null);
        r.setPreviousStatus(h.getPreviousStatus());
        r.setNewStatus(h.getNewStatus());
        r.setOperatorId(h.getOperatorId());
        r.setRemark(h.getRemark());
        r.setCreateTime(h.getCreateTime());
        return r;
    }

    // ---- request DTOs ----

    public record CreateVersionRequest(
            String metadataUuid,
            String checksum,
            String versionName) {
    }

    public record RollbackRequest(
            String resourceUuid) {
    }

    public record SetAliasRequest(
            String aliasName) {
    }
}