package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageScan;
import io.coreplatform.storage.application.domain.enums.ScanStatus;
import io.coreplatform.storage.application.domain.enums.ScanType;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageScanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容扫描服务 — 管理扫描记录状态，触发扫描（通过 SPI）。
 */
@Service
public class StorageScanService {

    private static final Logger log = LoggerFactory.getLogger(StorageScanService.class);

    private final StorageScanRepository scanRepo;

    public StorageScanService(StorageScanRepository scanRepo) {
        this.scanRepo = scanRepo;
    }

    /**
     * 为资源创建一条待扫描记录。
     */
    public StorageScan createScan(String resourceUuid, ScanType scanType) {
        StorageScan scan = StorageScan.create(resourceUuid, scanType);
        scan = scanRepo.save(scan);
        log.debug("Scan created: resourceUuid={}, scanType={}, status=PENDING", resourceUuid, scanType);
        return scan;
    }

    public StorageScan getScan(Long id) {
        return scanRepo.findById(id)
                .orElseThrow(() -> new ScanNotFoundException("Scan not found: id=" + id));
    }

    public List<StorageScan> listScans(String resourceUuid) {
        return scanRepo.findByResourceUuid(resourceUuid);
    }

    /**
     * 更新扫描结果（由外部扫描器插件调用）。
     */
    public void updateScanResult(Long id, ScanStatus status, String resultMessage) {
        scanRepo.updateStatus(id, status.name(), resultMessage);
        log.info("Scan result updated: id={}, status={}", id, status);
    }

    public List<StorageScan> search(String resourceUuid, String scanType, String status, int page, int size) {
        int offset = Math.max(0, page - 1) * size;
        return scanRepo.search(resourceUuid, scanType, status, offset, size);
    }

    public int countSearch(String resourceUuid, String scanType, String status) {
        return scanRepo.countSearch(resourceUuid, scanType, status);
    }

    // ─── inner exceptions ───

    public static class ScanNotFoundException extends RuntimeException {
        public ScanNotFoundException(String msg) { super(msg); }
    }
}