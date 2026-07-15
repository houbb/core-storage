package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageProfileResponse;
import io.coreplatform.storage.application.domain.StorageProfile;
import io.coreplatform.storage.application.service.StorageProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage/profiles")
@Tag(name = "Storage Profile", description = "存储配置管理（创建、修改、切换驱动绑定）")
public class StorageProfileController {

    private final StorageProfileService profileService;

    public StorageProfileController(StorageProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "列出所有存储配置")
    public List<StorageProfileResponse> listProfiles() {
        return profileService.listProfiles().stream()
                .map(StorageProfileController::toResponse)
                .toList();
    }

    @PostMapping
    @Operation(summary = "创建新的存储配置")
    public StorageProfileResponse createProfile(@RequestBody CreateProfileRequest request) {
        StorageProfile profile = profileService.createProfile(
                request.profileName(), request.driverName());
        return toResponse(profile);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新存储配置（切换驱动绑定）")
    public StorageProfileResponse updateProfile(@PathVariable Long id,
                                                 @RequestBody UpdateProfileRequest request) {
        StorageProfile updated = profileService.updateProfile(request.profileName(), request.driverName());
        return toResponse(updated);
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "设为默认配置")
    public ResponseEntity<Void> setDefault(@PathVariable Long id,
                                           @RequestBody SetDefaultRequest request) {
        profileService.setDefault(request.profileName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除存储配置")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id,
                                               @RequestParam String profileName) {
        profileService.deleteProfile(profileName);
        return ResponseEntity.noContent().build();
    }

    // --- request DTOs ---

    public record CreateProfileRequest(String profileName, String driverName) {}
    public record UpdateProfileRequest(String profileName, String driverName) {}
    public record SetDefaultRequest(String profileName) {}

    // --- helpers ---

    private static StorageProfileResponse toResponse(StorageProfile profile) {
        StorageProfileResponse resp = new StorageProfileResponse();
        resp.setId(profile.getId());
        resp.setProfileName(profile.getProfileName());
        resp.setDriverName(profile.getDriverName());
        resp.setDefault(profile.isDefault());
        resp.setCreateTime(profile.getCreateTime());
        return resp;
    }
}
