package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.AuditAction;

import java.time.LocalDateTime;

/**
 * 审计领域对象 — 对应 storage_audit 表。
 */
public class StorageAudit {

    private Long id;
    private String tenantId;
    private String resourceUuid;
    private String operatorId;
    private AuditAction action;
    private String target;
    private String result;
    private String detail;
    private String clientIp;
    private LocalDateTime createTime;

    public StorageAudit() {}

    public static StorageAudit create(String tenantId, String resourceUuid, String operatorId,
                                       AuditAction action, String target, String clientIp) {
        StorageAudit a = new StorageAudit();
        a.setTenantId(tenantId);
        a.setResourceUuid(resourceUuid);
        a.setOperatorId(operatorId);
        a.setAction(action);
        a.setTarget(target);
        a.setResult("SUCCESS");
        a.setClientIp(clientIp);
        a.setCreateTime(LocalDateTime.now());
        return a;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}