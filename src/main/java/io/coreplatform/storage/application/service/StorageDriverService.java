package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageDriverInfo;
import io.coreplatform.storage.application.domain.enums.DriverHealth;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.driver.DriverRegistry;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageDriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 驱动管理服务 — 提供驱动列表、详情、健康检查等功能。
 */
@Service
public class StorageDriverService {

    private static final Logger log = LoggerFactory.getLogger(StorageDriverService.class);

    private final DriverRegistry registry;
    private final StorageDriverRepository driverRepo;

    public StorageDriverService(DriverRegistry registry, StorageDriverRepository driverRepo) {
        this.registry = registry;
        this.driverRepo = driverRepo;
    }

    /**
     * 列出所有已注册的驱动及其运行时信息。
     */
    public List<StorageDriverInfo> listDrivers() {
        List<StorageDriverInfo> result = new ArrayList<>();
        Map<String, StorageDriver> all = registry.listAll();

        for (Map.Entry<String, StorageDriver> entry : all.entrySet()) {
            String name = entry.getKey();
            StorageDriver driver = entry.getValue();

            // 从数据库获取持久化信息（版本等）
            StorageDriverInfo info = driverRepo.findByDriverName(name)
                    .orElse(StorageDriverInfo.create(name, driver.type(), "1.0.0"));

            // 实时健康状态
            DriverHealth health = checkHealth(driver);
            info.setHealthStatus(health);

            result.add(info);
        }

        return result;
    }

    /**
     * 获取单个驱动详情。
     */
    public StorageDriverInfo getDriver(String name) {
        StorageDriver driver = registry.getRequired(name);

        StorageDriverInfo info = driverRepo.findByDriverName(name)
                .orElse(StorageDriverInfo.create(name, driver.type(), "1.0.0"));

        info.setHealthStatus(checkHealth(driver));
        return info;
    }

    /**
     * 健康检查 — 同时更新数据库中的健康状态。
     */
    public DriverHealth checkHealth(String name) {
        StorageDriver driver = registry.get(name);
        if (driver == null) {
            return DriverHealth.UNHEALTHY;
        }
        DriverHealth health = checkHealth(driver);
        driverRepo.updateHealthStatus(name, health);
        return health;
    }

    /**
     * 刷新所有驱动健康状态（定时任务可调用）。
     */
    public void refreshAllHealth() {
        log.info("Refreshing all driver health statuses...");
        for (Map.Entry<String, StorageDriver> entry : registry.listAll().entrySet()) {
            String name = entry.getKey();
            DriverHealth health = checkHealth(entry.getValue());
            driverRepo.updateHealthStatus(name, health);
        }
    }

    private DriverHealth checkHealth(StorageDriver driver) {
        try {
            return driver.health() ? DriverHealth.HEALTHY : DriverHealth.UNHEALTHY;
        } catch (Exception e) {
            log.warn("Health check failed for driver type={}: {}", driver.type(), e.getMessage());
            return DriverHealth.UNHEALTHY;
        }
    }
}
