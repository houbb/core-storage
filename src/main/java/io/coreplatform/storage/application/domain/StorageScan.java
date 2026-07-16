package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.ScanStatus;
import io.coreplatform.storage.application.domain.enums.ScanType;

import java.time.LocalDateTime;

/**
 * 内容扫描领域对象 — 对应 storage_scan 表。
 */
public class StorageScan {

    private Long id;
    private String resourceUuid;
    private ScanType scanType;
    private ScanStatus status;
    private String resultMessage;
    private LocalDateTime scanTime;
    private LocalDateTime createTime;

    public StorageScan() {}

    public static StorageScan create(String resourceUuid, ScanType scanType) {
        StorageScan s = new StorageScan();
        s.setResourceUuid(resourceUuid);
        s.setScanType(scanType);
        s.setStatus(ScanStatus.PENDING);
        s.setCreateTime(LocalDateTime.now());
        return s;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String resourceUuid) { this.resourceUuid = resourceUuid; }

    public ScanType getScanType() { return scanType; }
    public void setScanType(ScanType scanType) { this.scanType = scanType; }

    public ScanStatus getStatus() { return status; }
    public void setStatus(ScanStatus status) { this.status = status; }

    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }

    public LocalDateTime getScanTime() { return scanTime; }
    public void setScanTime(LocalDateTime scanTime) { this.scanTime = scanTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}