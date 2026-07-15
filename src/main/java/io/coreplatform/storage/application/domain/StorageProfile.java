package io.coreplatform.storage.application.domain;

import java.time.LocalDateTime;

/**
 * 存储配置（Storage Profile）领域对象 — 对应 storage_profile 表。
 * <p>
 * 设计原则：Resource 永远绑定 StorageProfile，而不是绑定具体 Driver。
 * 切换存储后端只需要切换 Profile，不需要修改 Resource。
 */
public class StorageProfile {

    private Long id;
    private String profileName;
    private String driverName;
    private boolean isDefault;
    private LocalDateTime createTime;

    public StorageProfile() {
    }

    public static StorageProfile create(String profileName, String driverName, boolean isDefault) {
        StorageProfile profile = new StorageProfile();
        profile.profileName = profileName;
        profile.driverName = driverName;
        profile.isDefault = isDefault;
        profile.createTime = LocalDateTime.now();
        return profile;
    }

    // -- getters & setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
