package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.HoldType;

import java.time.LocalDateTime;

/**
 * 资源法律保留领域对象 — 对应 storage_resource_hold 表。
 * <p>
 * 当资源处于 Hold 状态时（法律保留、审计保留、调查保留），
 * 所有生命周期操作（删除、归档、移动）都将被禁止，直到 Hold 被解除。
 * 对标 S3 Object Lock、企业 ECM、金融档案系统的 Legal Hold 能力。
 */
public class ResourceHold {

    private Long id;
    private String resourceUuid;
    private HoldType holdType;
    private String reason;
    private String operatorId;
    private LocalDateTime expireTime;
    private boolean released;
    private LocalDateTime releasedTime;
    private String releaseOperatorId;
    private LocalDateTime createTime;

    public ResourceHold() {
    }

    public static ResourceHold create(String resourceUuid, HoldType holdType,
                                       String reason, String operatorId,
                                       LocalDateTime expireTime) {
        ResourceHold h = new ResourceHold();
        h.resourceUuid = resourceUuid;
        h.holdType = holdType;
        h.reason = reason;
        h.operatorId = operatorId;
        h.expireTime = expireTime;
        h.released = false;
        h.createTime = LocalDateTime.now();
        return h;
    }

    /** 当前是否生效（未解除且未过期）。 */
    public boolean isActive() {
        if (released) return false;
        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) return false;
        return true;
    }

    // -- getters & setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public HoldType getHoldType() { return holdType; }
    public void setHoldType(HoldType holdType) { this.holdType = holdType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public boolean isReleased() { return released; }
    public void setReleased(boolean released) { this.released = released; }

    public LocalDateTime getReleasedTime() { return releasedTime; }
    public void setReleasedTime(LocalDateTime releasedTime) { this.releasedTime = releasedTime; }

    public String getReleaseOperatorId() { return releaseOperatorId; }
    public void setReleaseOperatorId(String releaseOperatorId) { this.releaseOperatorId = releaseOperatorId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}