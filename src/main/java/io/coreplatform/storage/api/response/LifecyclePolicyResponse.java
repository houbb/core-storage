package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * 生命周期策略响应 DTO。
 */
public class LifecyclePolicyResponse {

    private Long id;
    private String policyName;
    private String resourceType;
    private String category;
    private int activeDays;
    private int warmDays;
    private int coldDays;
    private int archiveDays;
    private int deleteDays;
    private boolean enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getActiveDays() { return activeDays; }
    public void setActiveDays(int activeDays) { this.activeDays = activeDays; }

    public int getWarmDays() { return warmDays; }
    public void setWarmDays(int warmDays) { this.warmDays = warmDays; }

    public int getColdDays() { return coldDays; }
    public void setColdDays(int coldDays) { this.coldDays = coldDays; }

    public int getArchiveDays() { return archiveDays; }
    public void setArchiveDays(int archiveDays) { this.archiveDays = archiveDays; }

    public int getDeleteDays() { return deleteDays; }
    public void setDeleteDays(int deleteDays) { this.deleteDays = deleteDays; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}