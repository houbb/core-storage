package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.AccessMode;

import java.time.LocalDateTime;

/**
 * 访问策略领域对象。
 */
public class StorageAccessPolicy {

    private Long id;
    private String resourceUuid;
    private AccessMode accessMode;
    private String roleName;
    private boolean allowDownload;
    private boolean allowPreview;
    private boolean allowUpdate;
    private boolean allowDelete;
    private boolean allowShare;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public AccessMode getAccessMode() { return accessMode; }
    public void setAccessMode(AccessMode accessMode) { this.accessMode = accessMode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public boolean isAllowDownload() { return allowDownload; }
    public void setAllowDownload(boolean allowDownload) { this.allowDownload = allowDownload; }

    public boolean isAllowPreview() { return allowPreview; }
    public void setAllowPreview(boolean allowPreview) { this.allowPreview = allowPreview; }

    public boolean isAllowUpdate() { return allowUpdate; }
    public void setAllowUpdate(boolean allowUpdate) { this.allowUpdate = allowUpdate; }

    public boolean isAllowDelete() { return allowDelete; }
    public void setAllowDelete(boolean allowDelete) { this.allowDelete = allowDelete; }

    public boolean isAllowShare() { return allowShare; }
    public void setAllowShare(boolean allowShare) { this.allowShare = allowShare; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
}