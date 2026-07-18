package io.coreplatform.storage.application.service;

import io.coreplatform.storage.api.security.AccessContext;
import io.coreplatform.storage.application.domain.*;
import io.coreplatform.storage.application.domain.enums.*;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageAccessPolicyRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * 统一访问服务 — 所有资源的 download / preview / signed-url 都经过此服务。
 * 权限检查矩阵见 checkAccess 方法。
 */
@Service
public class StorageAccessService {

    private static final Logger log = LoggerFactory.getLogger(StorageAccessService.class);

    private final StorageResourceRepository resourceRepo;
    private final StorageAccessPolicyRepository policyRepo;
    private final StorageResourceShareRepository shareRepo;
    private final StorageMetadataRepository metadataRepo;
    private final StorageDriver driver;
    private final StorageAccessLogService logService;
    private final String signedUrlSecret;

    // 默认 secret — production 应通过配置覆盖
    private static final String DEFAULT_SECRET = "core-storage-secret-change-me";

    public StorageAccessService(StorageResourceRepository resourceRepo,
                                 StorageAccessPolicyRepository policyRepo,
                                 StorageResourceShareRepository shareRepo,
                                 StorageMetadataRepository metadataRepo,
                                 StorageDriver driver,
                                 StorageAccessLogService logService) {
        this.resourceRepo = resourceRepo;
        this.policyRepo = policyRepo;
        this.shareRepo = shareRepo;
        this.metadataRepo = metadataRepo;
        this.driver = driver;
        this.logService = logService;
        this.signedUrlSecret = DEFAULT_SECRET;
    }

    // ──── 下载 ────

    /**
     * 下载资源（经过权限检查）。
     * @return [metadataUuid, storageKey, storageName] 用于获取物理文件
     */
    public DownloadResult download(String resourceUuid, AccessContext ctx) {
        long start = System.currentTimeMillis();
        StorageResource resource = getResourceOrThrow(resourceUuid);

        try {
            checkAccess(resource, ctx, "DOWNLOAD");
            StorageMetadata meta = loadMetadata(resource);

            // 构造返回：通过 driver 获取流
            InputStream stream = driver.download(meta.getRelativePath(), meta.getStorageName());

            logAccess(resourceUuid, "DOWNLOAD", ctx, "SUCCESS", null, start);
            return new DownloadResult(resource, meta, stream);
        } catch (AccessDeniedException e) {
            logAccess(resourceUuid, "DOWNLOAD", ctx, "DENIED", e.getMessage(), start);
            throw e;
        } catch (IOException e) {
            logAccess(resourceUuid, "DOWNLOAD", ctx, "ERROR", e.getMessage(), start);
            throw new RuntimeException("Failed to read file from storage driver", e);
        }
    }

    // ──── 预览 ────

    /**
     * 预览资源（经过权限检查，返回 inline 流）。
     */
    public DownloadResult preview(String resourceUuid, AccessContext ctx) {
        long start = System.currentTimeMillis();
        StorageResource resource = getResourceOrThrow(resourceUuid);

        try {
            checkAccess(resource, ctx, "PREVIEW");
            StorageMetadata meta = loadMetadata(resource);
            InputStream stream = driver.download(meta.getRelativePath(), meta.getStorageName());

            logAccess(resourceUuid, "PREVIEW", ctx, "SUCCESS", null, start);
            return new DownloadResult(resource, meta, stream);
        } catch (AccessDeniedException e) {
            logAccess(resourceUuid, "PREVIEW", ctx, "DENIED", e.getMessage(), start);
            throw e;
        } catch (IOException e) {
            logAccess(resourceUuid, "PREVIEW", ctx, "ERROR", e.getMessage(), start);
            throw new RuntimeException("Failed to read file from storage driver for preview", e);
        }
    }

    // ──── Signed URL ────

    /**
     * 生成带签名的临时下载 URL。
     * @param expiresInSeconds 有效期秒数
     */
    public String generateSignedUrl(String resourceUuid, long expiresInSeconds) {
        getResourceOrThrow(resourceUuid);
        long expires = Instant.now().getEpochSecond() + expiresInSeconds;
        String payload = resourceUuid + ":" + expires;
        String signature = hmacSha256(payload, signedUrlSecret);

        return "/api/v1/storage/resources/" + resourceUuid + "/download"
                + "?expires=" + expires + "&signature=" + signature;
    }

    /**
     * 验证 Signed URL 签名 + 过期。
     */
    public boolean verifySignedUrl(String resourceUuid, long expires, String signature) {
        if (Instant.now().getEpochSecond() > expires) return false;
        String payload = resourceUuid + ":" + expires;
        String expected = hmacSha256(payload, signedUrlSecret);
        return expected.equals(signature);
    }

    // ──── 权限检查核心 ────

    /**
     * 权限检查矩阵。
     */
    void checkAccess(StorageResource resource, AccessContext ctx, String operation) {
        AccessMode mode = resource.getAccessMode() != null ? resource.getAccessMode() : AccessMode.PUBLIC;

        switch (mode) {
            case PUBLIC:
                return; // 任何人
            case SYSTEM:
                if (!ctx.isSystem()) throw new AccessDeniedException(mode, operation);
                return;
            case LOGIN:
                if (!ctx.isAuthenticated()) throw new AccessDeniedException(mode, operation);
                return;
            case OWNER:
                if (!ctx.isSystem()
                        && !ctx.getUserId().equals(resource.getOwnerId()))
                    throw new AccessDeniedException(mode, operation);
                return;
            case SIGNED_URL:
                // Signed URL 校验在 Controller 层已做（或内部调用绕过）
                return;
            case TOKEN:
                // Share token 校验在 Controller 层已做
                return;
            case ROLE:
                checkRoleAccess(resource, ctx, operation);
                return;
            default:
                throw new AccessDeniedException(mode, operation);
        }
    }

    private void checkRoleAccess(StorageResource resource, AccessContext ctx, String operation) {
        List<StorageAccessPolicy> policies = policyRepo.findByResourceUuid(resource.getResourceUuid());
        if (policies.isEmpty()) {
            // 没有策略 = 默认允许（兼容）
            return;
        }

        for (StorageAccessPolicy policy : policies) {
            if (policy.getAccessMode() != AccessMode.ROLE) continue;
            if (policy.getRoleName() == null || !ctx.hasRole(policy.getRoleName())) continue;

            boolean allowed = switch (operation) {
                case "DOWNLOAD" -> policy.isAllowDownload();
                case "PREVIEW" -> policy.isAllowPreview();
                case "UPDATE" -> policy.isAllowUpdate();
                case "DELETE" -> policy.isAllowDelete();
                case "SHARE"  -> policy.isAllowShare();
                default -> false;
            };
            if (allowed) return;
        }

        throw new AccessDeniedException(AccessMode.ROLE, operation);
    }

    // ──── 内部 ────

    private StorageResource getResourceOrThrow(String resourceUuid) {
        return resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new StorageResourceService.ResourceNotFoundException(
                        "Resource not found: uuid=" + resourceUuid));
    }

    private StorageMetadata loadMetadata(StorageResource resource) {
        return metadataRepo.findByUuid(resource.getMetadataUuid())
                .orElseThrow(() -> new RuntimeException(
                        "Metadata not found for resource: " + resource.getResourceUuid()));
    }

    private void logAccess(String resourceUuid, String accessType, AccessContext ctx,
                            String result, String reason, long startMs) {
        StorageAccessLog log = new StorageAccessLog();
        log.setResourceUuid(resourceUuid);
        log.setAccessType(accessType);
        log.setOperatorId(ctx.getUserId());
        log.setOperatorRoles(ctx.getRoles() != null ? String.join(",", ctx.getRoles()) : null);
        log.setClientIp(ctx.getClientIp());
        log.setResult(result);
        log.setReason(reason);
        log.setDurationMs((int) (System.currentTimeMillis() - startMs));
        logService.log(log);
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }

    // ──── 异常 ────

    public static class AccessDeniedException extends RuntimeException {
        private final AccessMode mode;
        private final String operation;

        public AccessDeniedException(AccessMode mode, String operation) {
            super("Access denied: mode=" + mode + ", operation=" + operation);
            this.mode = mode;
            this.operation = operation;
        }

        public AccessMode getMode() { return mode; }
        public String getOperation() { return operation; }
    }

    // ──── 返回 DTO ────

    public static class DownloadResult {
        private final StorageResource resource;
        private final StorageMetadata metadata;
        private final InputStream stream;

        public DownloadResult(StorageResource resource, StorageMetadata metadata, InputStream stream) {
            this.resource = resource;
            this.metadata = metadata;
            this.stream = stream;
        }

        public StorageResource getResource() { return resource; }
        public StorageMetadata getMetadata() { return metadata; }
        public InputStream getStream() { return stream; }
    }
}