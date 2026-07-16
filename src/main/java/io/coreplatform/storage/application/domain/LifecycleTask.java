package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.LifecycleAction;
import io.coreplatform.storage.application.domain.enums.LifecycleTaskStatus;

import java.time.LocalDateTime;

/**
 * 生命周期任务领域对象 — 对应 storage_lifecycle_task 表。
 * <p>
 * 所有生命周期操作（转换、归档、删除、验证）都通过 Task 模型执行，
 * 与 SyncTask 保持一致的设计模式，保证可暂停、可恢复、可重试。
 */
public class LifecycleTask {

    private Long id;
    private String resourceUuid;
    private Long policyId;
    private LifecycleAction action;
    private String targetStage;
    private LifecycleTaskStatus status;
    private LocalDateTime executeTime;
    private LocalDateTime finishTime;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public LifecycleTask() {
    }

    public static LifecycleTask create(String resourceUuid, Long policyId,
                                        LifecycleAction action, String targetStage) {
        LifecycleTask task = new LifecycleTask();
        task.resourceUuid = resourceUuid;
        task.policyId = policyId;
        task.action = action;
        task.targetStage = targetStage;
        task.status = LifecycleTaskStatus.PENDING;
        task.retryCount = 0;
        task.createTime = LocalDateTime.now();
        task.updateTime = LocalDateTime.now();
        return task;
    }

    // -- getters & setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    public LifecycleAction getAction() { return action; }
    public void setAction(LifecycleAction action) { this.action = action; }

    public String getTargetStage() { return targetStage; }
    public void setTargetStage(String targetStage) { this.targetStage = targetStage; }

    public LifecycleTaskStatus getStatus() { return status; }
    public void setStatus(LifecycleTaskStatus status) { this.status = status; }

    public LocalDateTime getExecuteTime() { return executeTime; }
    public void setExecuteTime(LocalDateTime executeTime) { this.executeTime = executeTime; }

    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}