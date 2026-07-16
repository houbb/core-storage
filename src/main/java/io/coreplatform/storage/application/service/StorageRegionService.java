package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageRegion;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageRegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 区域管理服务 — 负责区域的 CRUD，支持区域到存储驱动的映射。
 */
@Service
public class StorageRegionService {

    private static final Logger log = LoggerFactory.getLogger(StorageRegionService.class);

    private final StorageRegionRepository regionRepo;

    public StorageRegionService(StorageRegionRepository regionRepo) {
        this.regionRepo = regionRepo;
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageRegion createRegion(String regionCode, String regionName, String endpoint, String driverName) {
        if (regionRepo.existsByRegionCode(regionCode)) {
            throw new RegionAlreadyExistsException("Region already exists: " + regionCode);
        }
        StorageRegion region = StorageRegion.create(regionCode, regionName, endpoint, driverName);
        region = regionRepo.save(region);
        log.info("Region created: regionCode={}, regionName={}", regionCode, regionName);
        return region;
    }

    public StorageRegion getRegion(String regionCode) {
        return regionRepo.findByRegionCode(regionCode)
                .orElseThrow(() -> new RegionNotFoundException("Region not found: " + regionCode));
    }

    public List<StorageRegion> listRegions() {
        return regionRepo.listAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageRegion updateRegion(String regionCode, String regionName, String endpoint, String driverName) {
        StorageRegion region = getRegion(regionCode);
        if (regionName != null) region.setRegionName(regionName);
        if (endpoint != null) region.setEndpoint(endpoint);
        if (driverName != null) region.setDriverName(driverName);
        region = regionRepo.save(region);
        log.info("Region updated: regionCode={}", regionCode);
        return region;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRegion(String regionCode) {
        getRegion(regionCode);
        regionRepo.deleteByRegionCode(regionCode);
        log.info("Region deleted: regionCode={}", regionCode);
    }

    // ─── inner exceptions ───

    public static class RegionNotFoundException extends RuntimeException {
        public RegionNotFoundException(String msg) { super(msg); }
    }

    public static class RegionAlreadyExistsException extends RuntimeException {
        public RegionAlreadyExistsException(String msg) { super(msg); }
    }
}