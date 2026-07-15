package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资源响应 DTO — 聚合了资源信息、标签、属性。
 */
public class StorageResourceResponse {

    private Long id;
    private String resourceUuid;
    private String metadataUuid;
    private String resourceName;
    private String resourceType;
    private String category;
    private String description;
    private String ownerType;
    private String ownerId;
    private String visibility;
    private String accessMode;
    private String status;
    private String profileName;
    private List<String> tags;
    private List<PropertyItem> properties;
    private int referenceCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String downloadUrl;

    // ---- nested DTO ----

    public static class PropertyItem {
        private String key;
        private String value;

        public PropertyItem() {}
        public PropertyItem(String key, String value) { this.key = key; this.value = value; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
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

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getAccessMode() { return accessMode; }
    public void setAccessMode(String accessMode) { this.accessMode = accessMode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<PropertyItem> getProperties() { return properties; }
    public void setProperties(List<PropertyItem> properties) { this.properties = properties; }

    public int getReferenceCount() { return referenceCount; }
    public void setReferenceCount(int referenceCount) { this.referenceCount = referenceCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}