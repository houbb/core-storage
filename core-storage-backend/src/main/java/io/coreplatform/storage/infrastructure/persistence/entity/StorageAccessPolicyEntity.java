package io.coreplatform.storage.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * 访问策略实体 — 对应 storage_access_policy 表。
 */
public class StorageAccessPolicyEntity {

    private Long id;
    private String resourceUuid;
    private String accessMode;
    private String roleName;
    private Integer allowDownload;
    private Integer allowPreview;
    private Integer allowUpdate;
    private Integer allowDelete;
    private Integer allowShare;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getAccessMode() { return accessMode; }
    public void setAccessMode(String accessMode) { this.accessMode = accessMode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public Integer getAllowDownload() { return allowDownload; }
    public void setAllowDownload(Integer allowDownload) { this.allowDownload = allowDownload; }

    public Integer getAllowPreview() { return allowPreview; }
    public void setAllowPreview(Integer allowPreview) { this.allowPreview = allowPreview; }

    public Integer getAllowUpdate() { return allowUpdate; }
    public void setAllowUpdate(Integer allowUpdate) { this.allowUpdate = allowUpdate; }

    public Integer getAllowDelete() { return allowDelete; }
    public void setAllowDelete(Integer allowDelete) { this.allowDelete = allowDelete; }

    public Integer getAllowShare() { return allowShare; }
    public void setAllowShare(Integer allowShare) { this.allowShare = allowShare; }

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