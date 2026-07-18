package io.coreplatform.storage.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * 生命周期策略实体 — 对应 storage_lifecycle_policy 表。
 */
public class LifecyclePolicyEntity {

    private Long id;
    private String policyName;
    private String resourceType;
    private String category;
    private Integer activeDays;
    private Integer warmDays;
    private Integer coldDays;
    private Integer archiveDays;
    private Integer deleteDays;
    private Integer enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // -- getters & setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getActiveDays() { return activeDays; }
    public void setActiveDays(Integer activeDays) { this.activeDays = activeDays; }

    public Integer getWarmDays() { return warmDays; }
    public void setWarmDays(Integer warmDays) { this.warmDays = warmDays; }

    public Integer getColdDays() { return coldDays; }
    public void setColdDays(Integer coldDays) { this.coldDays = coldDays; }

    public Integer getArchiveDays() { return archiveDays; }
    public void setArchiveDays(Integer archiveDays) { this.archiveDays = archiveDays; }

    public Integer getDeleteDays() { return deleteDays; }
    public void setDeleteDays(Integer deleteDays) { this.deleteDays = deleteDays; }

    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}