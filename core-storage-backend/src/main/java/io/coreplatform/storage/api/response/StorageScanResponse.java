package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * 内容扫描响应 DTO。
 */
public class StorageScanResponse {

    private Long id;
    private String resourceUuid;
    private String scanType;
    private String status;
    private String resultMessage;
    private LocalDateTime scanTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public String getScanType() { return scanType; }
    public void setScanType(String scanType) { this.scanType = scanType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }

    public LocalDateTime getScanTime() { return scanTime; }
    public void setScanTime(LocalDateTime scanTime) { this.scanTime = scanTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}