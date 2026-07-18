package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageResourceResponse;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage/resources")
@Tag(name = "Resource Runtime", description = "统一资源管理（上传、查询、搜索、更新、删除）")
public class StorageResourceController {

    private final StorageResourceService resourceService;

    public StorageResourceController(StorageResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "查询资源详情")
    public StorageResourceResponse getResource(@PathVariable String uuid) {
        return resourceService.getByUuid(uuid);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索资源（多条件过滤 + 分页）")
    public SearchResultResponse<StorageResourceResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "createTime") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        return resourceService.search(keyword, resourceType, category, visibility,
                ownerType, ownerId, tag, status, tenantId, sort, order, page, size);
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "更新资源信息（名称/描述/分类/可见性/标签）")
    public StorageResourceResponse updateResource(@PathVariable String uuid,
                                                    @RequestBody UpdateResourceRequest request) {
        return resourceService.update(uuid, request.resourceName, request.description,
                request.category, request.visibility, request.accessMode, request.tags);
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "进入生命周期删除流程（两阶段：软删除→宽限期→物理删除）")
    public ResponseEntity<Void> deleteResource(@PathVariable String uuid) {
        resourceService.enterLifecycleDeletion(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uuid}/properties")
    @Operation(summary = "获取资源扩展属性")
    public Map<String, String> getProperties(@PathVariable String uuid) {
        return resourceService.getProperties(uuid);
    }

    @PutMapping("/{uuid}/properties")
    @Operation(summary = "设置资源扩展属性")
    public ResponseEntity<Void> setProperties(@PathVariable String uuid,
                                               @RequestBody Map<String, String> properties) {
        resourceService.setProperties(uuid, properties);
        return ResponseEntity.ok().build();
    }

    // ---- request DTOs ----

    public record UpdateResourceRequest(
            String resourceName,
            String description,
            String category,
            String visibility,
            String accessMode,
            List<String> tags) {
    }
}