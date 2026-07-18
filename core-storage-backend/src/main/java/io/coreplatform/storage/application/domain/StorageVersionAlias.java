package io.coreplatform.storage.application.domain;

import java.time.LocalDateTime;

/**
 * 版本别名领域对象 — 给版本打语义化标签。
 * <p>
 * 常见别名：latest, stable, beta, preview, lts。
 * 每个 Resource 下的别名唯一。
 */
public class StorageVersionAlias {

    private Long id;
    private String versionUuid;
    private String resourceUuid;
    private String aliasName;
    private LocalDateTime createTime;

    public StorageVersionAlias() {
    }

    public static StorageVersionAlias create(String versionUuid, String resourceUuid, String aliasName) {
        StorageVersionAlias a = new StorageVersionAlias();
        a.versionUuid = versionUuid;
        a.resourceUuid = resourceUuid;
        a.aliasName = aliasName;
        a.createTime = LocalDateTime.now();
        return a;
    }

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionUuid() { return versionUuid; }
    public void setVersionUuid(String versionUuid) { this.versionUuid = versionUuid; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
