package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.VersionAction;

import java.time.LocalDateTime;

/**
 * 版本操作历史 — 完整审计追踪。
 */
public class StorageVersionHistory {

    private Long id;
    private String versionUuid;
    private String resourceUuid;
    private VersionAction action;
    private String previousStatus;
    private String newStatus;
    private String operatorId;
    private String remark;
    private LocalDateTime createTime;

    public StorageVersionHistory() {
    }

    public static StorageVersionHistory record(String versionUuid, String resourceUuid,
                                                VersionAction action,
                                                String previousStatus, String newStatus,
                                                String operatorId, String remark) {
        StorageVersionHistory h = new StorageVersionHistory();
        h.versionUuid = versionUuid;
        h.resourceUuid = resourceUuid;
        h.action = action;
        h.previousStatus = previousStatus;
        h.newStatus = newStatus;
        h.operatorId = operatorId;
        h.remark = remark;
        h.createTime = LocalDateTime.now();
        return h;
    }

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionUuid() { return versionUuid; }
    public void setVersionUuid(String versionUuid) { this.versionUuid = versionUuid; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public VersionAction getAction() { return action; }
    public void setAction(VersionAction action) { this.action = action; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
