package io.coreplatform.storage.api.response;

import io.coreplatform.storage.application.domain.SyncTask;

import java.time.LocalDateTime;

/**
 * 同步任务响应对象。
 */
public class SyncTaskResponse {

    private Long id;
    private String taskType;
    private String resourceUuid;
    private String sourceProfile;
    private String targetProfile;
    private String status;
    private Integer progress;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getSourceProfile() { return sourceProfile; }
    public void setSourceProfile(String sourceProfile) { this.sourceProfile = sourceProfile; }

    public String getTargetProfile() { return targetProfile; }
    public void setTargetProfile(String targetProfile) { this.targetProfile = targetProfile; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    /**
     * 从领域对象构建响应。
     */
    public static SyncTaskResponse from(SyncTask task) {
        SyncTaskResponse resp = new SyncTaskResponse();
        resp.setId(task.getId());
        resp.setTaskType(task.getTaskType() != null ? task.getTaskType().name() : null);
        resp.setResourceUuid(task.getResourceUuid());
        resp.setSourceProfile(task.getSourceProfile());
        resp.setTargetProfile(task.getTargetProfile());
        resp.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        resp.setProgress(task.getProgress());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setCreateTime(task.getCreateTime());
        resp.setUpdateTime(task.getUpdateTime());
        return resp;
    }
}