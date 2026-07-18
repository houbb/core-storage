package io.coreplatform.storage.application.domain;

import java.time.LocalDateTime;

/**
 * 生命周期策略领域对象 — 对应 storage_lifecycle_policy 表。
 * <p>
 * 策略绑定到 resource_type + category，而不是 Driver。
 * 例如：resource_type=DOCUMENT, category=EXPORT → 30天后删除。
 * resource_type=IMAGE, category=AVATAR → 永久保留。
 * <p>
 * 各 day 字段表示资源从创建起，经过多少天后应进入该阶段。
 * delete_days=0 表示永不过期。
 */
public class LifecyclePolicy {

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

    public LifecyclePolicy() {
    }

    public static LifecyclePolicy create(String policyName, String resourceType,
                                          String category, int activeDays, int warmDays,
                                          int coldDays, int archiveDays, int deleteDays,
                                          String description) {
        LifecyclePolicy p = new LifecyclePolicy();
        p.policyName = policyName;
        p.resourceType = resourceType;
        p.category = category;
        p.activeDays = activeDays;
        p.warmDays = warmDays;
        p.coldDays = coldDays;
        p.archiveDays = archiveDays;
        p.deleteDays = deleteDays;
        p.enabled = true;
        p.description = description;
        p.createTime = LocalDateTime.now();
        p.updateTime = LocalDateTime.now();
        return p;
    }

    /**
     * 判断指定天数后资源应处于哪个生命周期阶段。
     *
     * @param daysSinceCreation 资源自创建以来的天数
     * @return 应处于的阶段名称
     */
    public String targetStage(long daysSinceCreation) {
        if (deleteDays > 0 && daysSinceCreation >= deleteDays) return "DELETED";
        if (archiveDays > 0 && daysSinceCreation >= archiveDays) return "ARCHIVED";
        if (coldDays > 0 && daysSinceCreation >= coldDays) return "COLD";
        if (warmDays > 0 && daysSinceCreation >= warmDays) return "WARM";
        return "ACTIVE";
    }

    // -- getters & setters --

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