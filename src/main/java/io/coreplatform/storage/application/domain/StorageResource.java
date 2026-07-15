package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.ResourceCategory;
import io.coreplatform.storage.application.domain.enums.ResourceStatus;
import io.coreplatform.storage.application.domain.enums.ResourceType;
import io.coreplatform.storage.application.domain.enums.Visibility;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一资源领域对象 — 整个平台的资源入口。
 * 关系：StorageResource → StorageMetadata → StorageDriver → Binary
 */
public class StorageResource {

    private Long id;
    private String resourceUuid;
    private String metadataUuid;
    private String resourceName;
    private ResourceType resourceType;
    private ResourceCategory category;
    private String description;
    private String ownerType;
    private String ownerId;
    private Visibility visibility;
    private ResourceStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;

    /** 关联标签列表 */
    private List<String> tags = new ArrayList<>();

    /** 关联属性列表 */
    private List<ResourceProperty> properties = new ArrayList<>();

    /** 引用计数（非持久化，查询填充） */
    private int referenceCount;

    public StorageResource() {
    }

    /** 资源是否可被正常访问 */
    public boolean isAccessible() {
        return status == ResourceStatus.READY || status == ResourceStatus.REFERENCED;
    }

    /** 软删除 */
    public void markDeleted() {
        this.status = ResourceStatus.DELETED;
        this.updateTime = LocalDateTime.now();
    }

    /** 状态迁移 */
    public void transitionTo(ResourceStatus newStatus) {
        this.status = newStatus;
        this.updateTime = LocalDateTime.now();
    }

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getMetadataUuid() { return metadataUuid; }
    public void setMetadataUuid(String metadataUuid) { this.metadataUuid = metadataUuid; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }

    public ResourceCategory getCategory() { return category; }
    public void setCategory(ResourceCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<ResourceProperty> getProperties() { return properties; }
    public void setProperties(List<ResourceProperty> properties) { this.properties = properties; }

    public int getReferenceCount() { return referenceCount; }
    public void setReferenceCount(int referenceCount) { this.referenceCount = referenceCount; }

    /**
     * 资源扩展属性。
     */
    public static class ResourceProperty {
        private String key;
        private String value;

        public ResourceProperty() {}
        public ResourceProperty(String key, String value) { this.key = key; this.value = value; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}