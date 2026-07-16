package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.SyncTaskStatus;
import io.coreplatform.storage.application.domain.enums.SyncTaskType;

import java.time.LocalDateTime;

/**
 * 同步任务领域对象 — 对应 storage_sync_task 表。
 * <p>
 * 所有同步、迁移、恢复、校验操作都通过 Task 模型管理，
 * 避免引入 MQ，保持 MVP 到企业版的一致架构。
 */
public class SyncTask {

    private Long id;
    private SyncTaskType taskType;
    private String resourceUuid;
    private String sourceProfile;
    private String targetProfile;
    private SyncTaskStatus status;
    private Integer progress;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public SyncTask() {
    }

    public static SyncTask create(SyncTaskType taskType, String resourceUuid,
                                   String sourceProfile, String targetProfile) {
        SyncTask task = new SyncTask();
        task.taskType = taskType;
        task.resourceUuid = resourceUuid;
        task.sourceProfile = sourceProfile;
        task.targetProfile = targetProfile;
        task.status = SyncTaskStatus.PENDING;
        task.progress = 0;
        task.createTime = LocalDateTime.now();
        task.updateTime = LocalDateTime.now();
        return task;
    }

    // -- getters & setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SyncTaskType getTaskType() { return taskType; }
    public void setTaskType(SyncTaskType taskType) { this.taskType = taskType; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getSourceProfile() { return sourceProfile; }
    public void setSourceProfile(String sourceProfile) { this.sourceProfile = sourceProfile; }

    public String getTargetProfile() { return targetProfile; }
    public void setTargetProfile(String targetProfile) { this.targetProfile = targetProfile; }

    public SyncTaskStatus getStatus() { return status; }
    public void setStatus(SyncTaskStatus status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}