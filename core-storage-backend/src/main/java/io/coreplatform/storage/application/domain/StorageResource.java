package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源领域对象 — 对应 storage_resource 表。
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
    private AccessMode accessMode;
    private ResourceStatus status;
    private List<String> tags;
    private List<Property> properties;
    private int referenceCount;
    private String profileName;
    private LifecycleStage lifecycleStage;
    private String tenantId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;

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

    public AccessMode getAccessMode() { return accessMode; }
    public void setAccessMode(AccessMode accessMode) { this.accessMode = accessMode; }

    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }

    public List<String> getTags() { return tags != null ? tags : (tags = new ArrayList<>()); }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<Property> getProperties() { return properties != null ? properties : (properties = new ArrayList<>()); }
    public void setProperties(List<Property> properties) { this.properties = properties; }

    public int getReferenceCount() { return referenceCount; }
    public void setReferenceCount(int referenceCount) { this.referenceCount = referenceCount; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public LifecycleStage getLifecycleStage() { return lifecycleStage; }
    public void setLifecycleStage(LifecycleStage lifecycleStage) { this.lifecycleStage = lifecycleStage; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }

    public static class Property {
        private String key;
        private String value;

        public Property() {}

        public Property(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}