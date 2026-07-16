package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * 版本别名响应 DTO — 对应 storage_version_alias 表。
 */
public class StorageVersionAliasResponse {

    private String versionUuid;
    private String resourceUuid;
    private String aliasName;
    private LocalDateTime createTime;

    // ---- getters & setters ----

    public String getVersionUuid() { return versionUuid; }
    public void setVersionUuid(String versionUuid) { this.versionUuid = versionUuid; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
