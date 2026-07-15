package io.coreplatform.storage.infrastructure.config;

import io.coreplatform.storage.application.domain.enums.DriverHealth;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.driver.DatabaseDriver;
import io.coreplatform.storage.infrastructure.driver.DriverRegistry;
import io.coreplatform.storage.infrastructure.driver.LocalDiskDriver;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageDriverBlobRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageDriverRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 存储驱动配置 — P5 重构为多驱动架构。
 * <p>
 * 启动流程：
 * <ol>
 *     <li>创建 LocalDiskDriver、DatabaseDriver 实例</li>
 *     <li>注册到 DriverRegistry</li>
 *     <li>自动在 storage_driver 表中插入/更新驱动记录</li>
 *     <li>如果 storage_profile 表为空，自动创建 "default" profile 绑定到 "local" 驱动</li>
 *     <li>保持向后兼容：仍提供默认 StorageDriver bean，解析 "default" profile</li>
 * </ol>
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    // --- driver beans ---

    @Bean
    public LocalDiskDriver localDiskDriver(StorageProperties properties) {
        return new LocalDiskDriver(properties.getLocal());
    }

    @Bean
    public DatabaseDriver databaseDriver(JdbcTemplate jdbcTemplate,
                                          StorageDriverBlobRepository blobRepo) {
        return new DatabaseDriver(jdbcTemplate, blobRepo);
    }

    // --- registry ---

    @Bean
    public DriverRegistry driverRegistry(LocalDiskDriver localDiskDriver,
                                          DatabaseDriver databaseDriver) {
        DriverRegistry registry = new DriverRegistry();
        registry.register("local", localDiskDriver);
        registry.register("database", databaseDriver);
        log.info("DriverRegistry initialized with {} drivers", registry.listNames().size());
        return registry;
    }

    // --- factory ---

    @Bean
    public StorageDriverFactory storageDriverFactory(DriverRegistry registry,
                                                      StorageProfileRepository profileRepo,
                                                      io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository resourceRepo) {
        return new StorageDriverFactory(registry, profileRepo, resourceRepo);
    }

    /**
     * 向后兼容：为 P0-P4 的代码提供一个默认 StorageDriver bean。
     * 自动解析为 "default" profile 绑定的驱动。
     */
    @Bean
    public StorageDriver storageDriver(StorageDriverFactory factory) {
        return factory.getDriverForProfile(null);
    }

    // --- startup initialization ---

    @Bean
    public ApplicationRunner driverRuntimeInitializer(
            DriverRegistry registry,
            StorageDriverRepository driverRepo,
            StorageProfileRepository profileRepo) {
        return args -> {
            log.info("=== P5 Driver Runtime Initialization ===");

            // 1. 将所有已注册的驱动同步到 storage_driver 表
            for (var entry : registry.listAll().entrySet()) {
                String name = entry.getKey();
                StorageDriver driver = entry.getValue();
                String version = getDriverVersion(driver);

                var info = io.coreplatform.storage.application.domain.StorageDriverInfo.create(
                        name, driver.type(), version);
                info.setHealthStatus(DriverHealth.UNKNOWN);
                driverRepo.save(info);
                log.info("  Driver synced to DB: name={}, type={}, version={}", name, driver.type(), version);
            }

            // 2. 如果 storage_profile 表为空，创建 "default" profile 绑定到 "local"
            if (profileRepo.count() == 0) {
                var defaultProfile = io.coreplatform.storage.application.domain.StorageProfile.create(
                        "default", "local", true);
                profileRepo.save(defaultProfile);
                log.info("  Auto-created default profile: name=default, driver=local");
            } else {
                log.info("  Profiles already exist, skipping default creation");
            }

            log.info("=== P5 Driver Runtime Initialized ===");
        };
    }

    private static String getDriverVersion(StorageDriver driver) {
        if (driver instanceof LocalDiskDriver ld) return ld.getVersion();
        if (driver instanceof DatabaseDriver dd) return dd.getVersion();
        return "1.0.0";
    }
}
