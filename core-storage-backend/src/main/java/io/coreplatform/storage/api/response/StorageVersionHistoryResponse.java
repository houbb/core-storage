package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * 版本历史响应 DTO — 对应 storage_version_history 表。
 */
public class StorageVersionHistoryResponse {

    private String versionUuid;
    private String resourceUuid;
    private String action;
    private String previousStatus;
    private String newStatus;
    private String operatorId;
    private String remark;
    private LocalDateTime createTime;

    // ---- getters & setters ----

    public String getVersionUuid() { return versionUuid; }
    public void setVersionUuid(String versionUuid) { this.versionUuid = versionUuid; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

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
