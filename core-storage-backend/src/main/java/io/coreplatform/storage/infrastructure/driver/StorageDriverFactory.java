package io.coreplatform.storage.infrastructure.driver;

import io.coreplatform.storage.application.domain.StorageProfile;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageProfileRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 驱动工厂 — 根据配置（StorageProfile）解析出对应的 StorageDriver。
 * <p>
 * 设计原则：Resource 永远绑定 StorageProfile，而不是直接绑定 Driver。
 * 切换存储后端只需切换 Profile，不需要修改 Resource 或业务代码。
 */
@Component
public class StorageDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(StorageDriverFactory.class);

    private final DriverRegistry registry;
    private final StorageProfileRepository profileRepo;
    private final StorageResourceRepository resourceRepo;

    public StorageDriverFactory(DriverRegistry registry,
                                StorageProfileRepository profileRepo,
                                StorageResourceRepository resourceRepo) {
        this.registry = registry;
        this.profileRepo = profileRepo;
        this.resourceRepo = resourceRepo;
    }

    /**
     * 根据 profile 名称解析驱动。
     * 如果 profileName 为 null 或空，使用默认 profile。
     *
     * @param profileName profile 名称（可为 null，表示使用默认）
     * @return 对应的 StorageDriver
     * @throws DriverRegistry.DriverNotFoundException 如果 profile 或 driver 不存在
     */
    public StorageDriver getDriverForProfile(String profileName) {
        String name = (profileName == null || profileName.isBlank()) ? "default" : profileName;

        StorageProfile profile = profileRepo.findByProfileName(name)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found: " + name));

        StorageDriver driver = registry.get(profile.getDriverName());
        if (driver == null) {
            throw new DriverRegistry.DriverNotFoundException(
                    "Driver not found for profile '" + name + "': driver=" + profile.getDriverName());
        }

        log.debug("Resolved driver: profile={} → driver={} ({})", name, profile.getDriverName(), driver.type());
        return driver;
    }

    /**
     * 根据资源 UUID 解析驱动。
     * 先从 resource 表中查询该资源绑定的 profile_name，
     * 再通过 profile 找到对应的 driver。
     *
     * @param resourceUuid 资源的 resource_uuid
     * @return 对应的 StorageDriver
     */
    public StorageDriver getDriverForResource(String resourceUuid) {
        io.coreplatform.storage.application.domain.StorageResource resource =
                resourceRepo.findByResourceUuid(resourceUuid)
                        .orElseThrow(() -> new ProfileNotFoundException("Resource not found: " + resourceUuid));

        String profileName = resource.getProfileName();
        if (profileName == null || profileName.isBlank()) {
            profileName = "default";
        }

        return getDriverForProfile(profileName);
    }

    /**
     * 检查 profile 对应的 driver 是否健康。
     */
    public boolean isProfileHealthy(String profileName) {
        try {
            StorageDriver driver = getDriverForProfile(profileName);
            return driver.health();
        } catch (Exception e) {
            return false;
        }
    }

    // ---- exception ----

    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(String message) {
            super(message);
        }
    }
}
