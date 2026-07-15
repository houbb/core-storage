package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageMetadataResponse;
import io.coreplatform.storage.api.response.StorageReferenceResponse;
import io.coreplatform.storage.application.service.StorageMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Metadata Runtime", description = "元数据查询、搜索、引用管理")
public class StorageMetadataController {

    private final StorageMetadataService metadataService;

    public StorageMetadataController(StorageMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/metadata/{uuid}")
    @Operation(summary = "查询元数据详情")
    public StorageMetadataResponse getMetadata(@PathVariable String uuid) {
        return metadataService.getByUuid(uuid);
    }

    @GetMapping("/metadata/search")
    @Operation(summary = "搜索元数据（多条件过滤 + 分页 + 排序）")
    public SearchResultResponse<StorageMetadataResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String hash,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String system,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "createTime") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        return metadataService.search(keyword, mimeType, status, hash,
                ownerType, ownerId, system, module, tag,
                startTime, endTime, sort, order, page, size);
    }

    @GetMapping("/metadata/{uuid}/references")
    @Operation(summary = "查询资源的引用列表")
    public List<StorageReferenceResponse> getReferences(@PathVariable String uuid) {
        return metadataService.getReferences(uuid);
    }

    @PostMapping("/reference")
    @Operation(summary = "创建业务引用")
    public StorageReferenceResponse createReference(@RequestBody CreateReferenceRequest request) {
        return metadataService.createReference(
                request.metadataUuid, request.system, request.module,
                request.businessType, request.businessId);
    }

    @DeleteMapping("/reference/{id}")
    @Operation(summary = "删除业务引用")
    public ResponseEntity<Void> deleteReference(@PathVariable Long id,
                                                  @RequestParam String metadataUuid) {
        metadataService.deleteReferenceByUuid(id, metadataUuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * 创建引用的请求体。
     */
    public record CreateReferenceRequest(
            String metadataUuid,
            String system,
            String module,
            String businessType,
            String businessId) {
    }
}