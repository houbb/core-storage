package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * 资源 Hold 响应 DTO。
 */
public class ResourceHoldResponse {

    private Long id;
    private String resourceUuid;
    private String holdType;
    private String reason;
    private String operatorId;
    private LocalDateTime expireTime;
    private boolean released;
    private LocalDateTime releasedTime;
    private String releaseOperatorId;
    private LocalDateTime createTime;

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getHoldType() { return holdType; }
    public void setHoldType(String holdType) { this.holdType = holdType; }

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