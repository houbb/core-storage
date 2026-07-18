package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageProfile;
import io.coreplatform.storage.infrastructure.driver.DriverRegistry;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 存储配置管理服务 — 提供 profile 的 CRUD 操作。
 */
@Service
public class StorageProfileService {

    private static final Logger log = LoggerFactory.getLogger(StorageProfileService.class);

    private final StorageProfileRepository profileRepo;
    private final DriverRegistry registry;

    public StorageProfileService(StorageProfileRepository profileRepo, DriverRegistry registry) {
        this.profileRepo = profileRepo;
        this.registry = registry;
    }

    /**
     * 列出所有 profile。
     */
    public List<StorageProfile> listProfiles() {
        return profileRepo.findAll();
    }

    /**
     * 根据名称获取 profile。
     */
    public StorageProfile getProfile(String profileName) {
        return profileRepo.findByProfileName(profileName)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found: " + profileName));
    }

    /**
     * 创建新的 profile。
     *
     * @param profileName profile 名称（唯一）
     * @param driverName  绑定的驱动名称（必须已在 Registry 中注册）
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageProfile createProfile(String profileName, String driverName) {
        if (profileRepo.existsByProfileName(profileName)) {
            throw new ProfileAlreadyExistsException("Profile already exists: " + profileName);
        }

        // 验证驱动存在
        if (!registry.contains(driverName)) {
            throw new InvalidDriverException("Driver not registered: " + driverName);
        }

        // 如果是第一个 profile，自动设为默认
        boolean isDefault = profileRepo.count() == 0;

        StorageProfile profile = StorageProfile.create(profileName, driverName, isDefault);
        StorageProfile saved = profileRepo.save(profile);

        log.info("Profile created: name={}, driver={}, isDefault={}", profileName, driverName, isDefault);
        return saved;
    }

    /**
     * 更新 profile 的驱动绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageProfile updateProfile(String profileName, String driverName) {
        StorageProfile profile = profileRepo.findByProfileName(profileName)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found: " + profileName));

        if (!registry.contains(driverName)) {
            throw new InvalidDriverException("Driver not registered: " + driverName);
        }

        profileRepo.update(profileName, driverName);
        profile.setDriverName(driverName);

        log.info("Profile updated: name={}, driver={}", profileName, driverName);
        return profile;
    }

    /**
     * 将指定 profile 设为默认（清除其他默认标记）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(String profileName) {
        if (!profileRepo.existsByProfileName(profileName)) {
            throw new ProfileNotFoundException("Profile not found: " + profileName);
        }
        profileRepo.setDefault(profileName);
        log.info("Default profile set: {}", profileName);
    }

    /**
     * 删除 profile（不允许删除默认 profile）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProfile(String profileName) {
        StorageProfile profile = profileRepo.findByProfileName(profileName)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found: " + profileName));

        if (profile.isDefault()) {
            throw new CannotDeleteDefaultProfileException("Cannot delete the default profile: " + profileName);
        }

        profileRepo.deleteByProfileName(profileName);
        log.info("Profile deleted: {}", profileName);
    }

    /**
     * 获取默认 profile。
     */
    public StorageProfile getDefaultProfile() {
        return profileRepo.findDefault()
                .orElseThrow(() -> new ProfileNotFoundException("No default profile configured"));
    }

    // ---- exceptions ----

    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(String message) { super(message); }
    }

    public static class ProfileAlreadyExistsException extends RuntimeException {
        public ProfileAlreadyExistsException(String message) { super(message); }
    }

    public static class InvalidDriverException extends RuntimeException {
        public InvalidDriverException(String message) { super(message); }
    }

    public static class CannotDeleteDefaultProfileException extends RuntimeException {
        public CannotDeleteDefaultProfileException(String message) { super(message); }
    }
}
