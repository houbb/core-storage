package io.coreplatform.storage.application.service;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageResourceResponse;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.StorageResource.ResourceProperty;
import io.coreplatform.storage.application.domain.enums.ResourceCategory;
import io.coreplatform.storage.application.domain.enums.ResourceStatus;
import io.coreplatform.storage.application.domain.enums.ResourceType;
import io.coreplatform.storage.application.domain.enums.Visibility;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageResourcePropertyEntity;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourcePropertyRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceTagRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StorageResourceService {

    private static final Logger log = LoggerFactory.getLogger(StorageResourceService.class);

    private final StorageResourceRepository resourceRepo;
    private final StorageResourceTagRepository tagRepo;
    private final StorageResourcePropertyRepository propertyRepo;
    private final StorageReferenceRepository referenceRepo;

    public StorageResourceService(StorageResourceRepository resourceRepo,
                                   StorageResourceTagRepository tagRepo,
                                   StorageResourcePropertyRepository propertyRepo,
                                   StorageReferenceRepository referenceRepo) {
        this.resourceRepo = resourceRepo;
        this.tagRepo = tagRepo;
        this.propertyRepo = propertyRepo;
        this.referenceRepo = referenceRepo;
    }

    /**
     * 创建资源（上传时调用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageResource createResource(String metadataUuid,
                                           String resourceName,
                                           String resourceType,
                                           String category,
                                           String description,
                                           String ownerType,
                                           String ownerId,
                                           String visibility,
                                           List<String> tags,
                                           Map<String, String> properties) {
        StorageResource resource = new StorageResource();
        resource.setResourceUuid(UUID.randomUUID().toString().replace("-", ""));
        resource.setMetadataUuid(metadataUuid);
        resource.setResourceName(resourceName != null ? resourceName : "unnamed");
        resource.setResourceType(safeEnum(ResourceType.class, resourceType, ResourceType.OTHER));
        resource.setCategory(safeEnum(ResourceCategory.class, category, ResourceCategory.OTHER));
        resource.setDescription(description);
        resource.setOwnerType(ownerType);
        resource.setOwnerId(ownerId);
        resource.setVisibility(safeEnum(Visibility.class, visibility, Visibility.PUBLIC));
        resource.setStatus(ResourceStatus.UPLOADING);

        StorageResource saved = resourceRepo.save(resource);

        // 保存标签
        if (tags != null && !tags.isEmpty()) {
            tagRepo.replaceTags(saved.getResourceUuid(), tags);
        }

        // 保存属性
        if (properties != null && !properties.isEmpty()) {
            propertyRepo.setProperties(saved.getResourceUuid(), properties);
        }

        log.info("Resource created: resourceUuid={}, metadataUuid={}, type={}",
                saved.getResourceUuid(), metadataUuid, saved.getResourceType());
        return saved;
    }

    /**
     * 根据 UUID 查询资源详情。
     */
    public StorageResourceResponse getByUuid(String resourceUuid) {
        StorageResource r = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: uuid=" + resourceUuid));

        r.setTags(tagRepo.findTagsByResourceUuid(resourceUuid));
        r.setProperties(loadProperties(resourceUuid));
        r.setReferenceCount(referenceRepo.countByMetadataUuid(r.getMetadataUuid()));
        return toResponse(r);
    }

    /**
     * 搜索资源。
     */
    public SearchResultResponse<StorageResourceResponse> search(
            String keyword, String resourceType, String category,
            String visibility, String ownerType, String ownerId,
            String tag, String status,
            String sort, String order, int page, int size) {

        int total = resourceRepo.countSearch(keyword, resourceType, category,
                visibility, ownerType, ownerId, tag, status);

        int offset = Math.max(0, page - 1) * size;
        List<StorageResource> list = resourceRepo.search(keyword, resourceType, category,
                visibility, ownerType, ownerId, tag, status,
                sort, order, offset, size);

        List<StorageResourceResponse> items = list.stream().map(r -> {
            r.setTags(tagRepo.findTagsByResourceUuid(r.getResourceUuid()));
            r.setReferenceCount(referenceRepo.countByMetadataUuid(r.getMetadataUuid()));
            return toResponse(r);
        }).toList();

        return new SearchResultResponse<>(items, page, size, total);
    }

    /**
     * 更新资源信息（名称/描述/分类/可见性/标签）。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageResourceResponse update(String resourceUuid,
                                            String resourceName,
                                            String description,
                                            String category,
                                            String visibility,
                                            List<String> tags) {
        StorageResource r = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: uuid=" + resourceUuid));

        resourceRepo.update(resourceUuid,
                resourceName != null ? resourceName : r.getResourceName(),
                description != null ? description : r.getDescription(),
                category != null ? category : (r.getCategory() != null ? r.getCategory().name() : "OTHER"),
                visibility != null ? visibility : (r.getVisibility() != null ? r.getVisibility().name() : "PUBLIC"),
                tags);

        if (tags != null) {
            tagRepo.replaceTags(resourceUuid, tags);
        }

        log.info("Resource updated: resourceUuid={}", resourceUuid);

        // 重新加载返回
        return getByUuid(resourceUuid);
    }

    /**
     * 更新资源状态（由 MetadataService 同步调用）。
     */
    public void updateStatus(String metadataUuid, String newStatus) {
        resourceRepo.findByMetadataUuid(metadataUuid).ifPresent(r -> {
            resourceRepo.updateStatus(r.getResourceUuid(), newStatus);
            log.info("Resource status synced: resourceUuid={}, status={}", r.getResourceUuid(), newStatus);
        });
    }

    /**
     * 软删除资源。
     */
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(String resourceUuid) {
        StorageResource r = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: uuid=" + resourceUuid));

        resourceRepo.updateStatus(resourceUuid, ResourceStatus.DELETED.name());
        tagRepo.deleteByResourceUuid(resourceUuid);
        log.info("Resource soft-deleted: resourceUuid={}", resourceUuid);
    }

    /**
     * 获取扩展属性。
     */
    public Map<String, String> getProperties(String resourceUuid) {
        List<StorageResourcePropertyEntity> props = propertyRepo.findByResourceUuid(resourceUuid);
        Map<String, String> result = new LinkedHashMap<>();
        for (var p : props) {
            result.put(p.getPropKey(), p.getPropValue());
        }
        return result;
    }

    /**
     * 设置扩展属性。
     */
    @Transactional(rollbackFor = Exception.class)
    public void setProperties(String resourceUuid, Map<String, String> properties) {
        resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: uuid=" + resourceUuid));
        propertyRepo.setProperties(resourceUuid, properties);
        log.info("Resource properties set: resourceUuid={}, count={}", resourceUuid, properties.size());
    }

    // ---- helpers ----

    private StorageResourceResponse toResponse(StorageResource r) {
        StorageResourceResponse resp = new StorageResourceResponse();
        resp.setId(r.getId());
        resp.setResourceUuid(r.getResourceUuid());
        resp.setMetadataUuid(r.getMetadataUuid());
        resp.setResourceName(r.getResourceName());
        resp.setResourceType(r.getResourceType() != null ? r.getResourceType().name() : null);
        resp.setCategory(r.getCategory() != null ? r.getCategory().name() : null);
        resp.setDescription(r.getDescription());
        resp.setOwnerType(r.getOwnerType());
        resp.setOwnerId(r.getOwnerId());
        resp.setVisibility(r.getVisibility() != null ? r.getVisibility().name() : null);
        resp.setStatus(r.getStatus() != null ? r.getStatus().name() : null);
        resp.setTags(r.getTags());
        resp.setProperties(r.getProperties().stream()
                .map(p -> new StorageResourceResponse.PropertyItem(p.getKey(), p.getValue()))
                .toList());
        resp.setReferenceCount(r.getReferenceCount());
        resp.setCreateTime(r.getCreateTime());
        resp.setUpdateTime(r.getUpdateTime());
        resp.setDownloadUrl("/api/v1/storage/resources/" + r.getResourceUuid() + "/download");
        return resp;
    }

    private List<ResourceProperty> loadProperties(String resourceUuid) {
        return propertyRepo.findByResourceUuid(resourceUuid).stream()
                .map(p -> new ResourceProperty(p.getPropKey(), p.getPropValue()))
                .toList();
    }

    private <E extends Enum<E>> E safeEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    // ---- inner exceptions ----

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}