package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageResourceResponse;
import io.coreplatform.storage.api.security.AccessContext;
import io.coreplatform.storage.application.domain.StorageAccessPolicy;
import io.coreplatform.storage.application.domain.StorageResourceShare;
import io.coreplatform.storage.application.domain.enums.AccessMode;
import io.coreplatform.storage.application.service.StorageAccessService;
import io.coreplatform.storage.application.service.StorageAccessService.AccessDeniedException;
import io.coreplatform.storage.application.service.StorageAccessService.DownloadResult;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.application.service.StorageShareService;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageAccessPolicyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 统一访问入口 — 所有资源的 download / preview / share / signed-url / policy 均由此 Controller 提供。
 * 这是 P3 Access Runtime 的核心对外 API。
 */
@RestController
@RequestMapping("/api/v1/storage/resources")
@Tag(name = "Access Runtime", description = "统一资源访问（下载、预览、分享、签名URL、策略）")
public class StorageAccessController {

    private final StorageAccessService accessService;
    private final StorageShareService shareService;
    private final StorageResourceService resourceService;
    private final StorageAccessPolicyRepository policyRepo;
    private final AccessContext accessCtx;

    public StorageAccessController(StorageAccessService accessService,
                                   StorageShareService shareService,
                                   StorageResourceService resourceService,
                                   StorageAccessPolicyRepository policyRepo,
                                   AccessContext accessCtx) {
        this.accessService = accessService;
        this.shareService = shareService;
        this.resourceService = resourceService;
        this.policyRepo = policyRepo;
        this.accessCtx = accessCtx;
    }

    // ────────── 统一访问入口 ──────────

    @GetMapping("/{uuid}/access")
    @Operation(summary = "统一资源访问入口（会检查权限但返回元数据）")
    public StorageResourceResponse accessResource(@PathVariable String uuid) {
        return resourceService.getByUuid(uuid);
    }

    // ────────── 下载 ──────────

    @GetMapping("/{uuid}/download")
    @Operation(summary = "下载资源（经过 Access Runtime 权限检查）")
    public ResponseEntity<Resource> download(
            @PathVariable String uuid,
            @RequestParam(required = false) String shareToken,
            @RequestParam(required = false) Long expires,
            @RequestParam(required = false) String signature) {

        // Signed URL 模式
        if (expires != null && signature != null) {
            if (!accessService.verifySignedUrl(uuid, expires, signature)) {
                throw new AccessDeniedException(AccessMode.SIGNED_URL, "DOWNLOAD");
            }
        }

        // Share Token 模式
        if (shareToken != null) {
            shareService.validateToken(shareToken)
                    .orElseThrow(() -> new AccessDeniedException(AccessMode.TOKEN, "DOWNLOAD"));
        }

        DownloadResult result = accessService.download(uuid, accessCtx);

        String filename = result.getResource().getResourceName();

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(result.getStream()));
    }

    // ────────── 预览 ──────────

    @GetMapping("/{uuid}/preview")
    @Operation(summary = "预览资源（返回 inline 流，图片/PDF/文本等可直接在浏览器展示）")
    public ResponseEntity<Resource> preview(@PathVariable String uuid) {
        DownloadResult result = accessService.preview(uuid, accessCtx);

        String filename = result.getResource().getResourceName();
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        // 尝试从 resource metadata 推断 MIME
        String mimeType = result.getMetadata() != null ? result.getMetadata().getMimeType() : null;
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(mimeType))
                .body(new InputStreamResource(result.getStream()));
    }

    // ────────── 分享 ──────────

    @PostMapping("/{uuid}/share")
    @Operation(summary = "生成分享链接")
    public ShareResponse createShare(
            @PathVariable String uuid,
            @RequestBody ShareRequest request) {
        int expireSeconds = request.expireSeconds > 0 ? request.expireSeconds : 86400;
        StorageResourceShare share = shareService.createShare(uuid, expireSeconds, accessCtx);
        ShareResponse resp = new ShareResponse();
        resp.setShareToken(share.getShareToken());
        resp.setExpireTime(share.getExpireTime());
        resp.setShareUrl("/api/v1/storage/resources/" + uuid + "/download?shareToken=" + share.getShareToken());
        return resp;
    }

    @GetMapping("/{uuid}/shares")
    @Operation(summary = "查询资源的所有分享链接")
    public List<ShareResponse> listShares(@PathVariable String uuid) {
        return shareService.listShares(uuid).stream().map(s -> {
            ShareResponse resp = new ShareResponse();
            resp.setId(s.getId());
            resp.setShareToken(s.getShareToken());
            resp.setExpireTime(s.getExpireTime());
            resp.setShareUrl("/api/v1/storage/resources/" + uuid + "/download?shareToken=" + s.getShareToken());
            resp.setExpired(s.isExpired());
            return resp;
        }).toList();
    }

    @DeleteMapping("/{uuid}/shares/{shareId}")
    @Operation(summary = "撤销分享链接")
    public ResponseEntity<Void> revokeShare(@PathVariable String uuid, @PathVariable Long shareId) {
        boolean deleted = shareService.revokeShare(shareId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ────────── Signed URL ──────────

    @PostMapping("/{uuid}/signed-url")
    @Operation(summary = "生成签名下载 URL")
    public SignedUrlResponse generateSignedUrl(
            @PathVariable String uuid,
            @RequestBody SignedUrlRequest request) {
        long expiresIn = request.expiresInSeconds > 0 ? request.expiresInSeconds : 3600;
        String url = accessService.generateSignedUrl(uuid, expiresIn);
        SignedUrlResponse resp = new SignedUrlResponse();
        resp.setUrl(url);
        resp.setExpiresInSeconds(expiresIn);
        return resp;
    }

    // ────────── Policy ──────────

    @GetMapping("/{uuid}/policy")
    @Operation(summary = "查看资源的访问策略列表")
    public List<PolicyResponse> getPolicies(@PathVariable String uuid) {
        List<StorageAccessPolicy> policies = policyRepo.findByResourceUuid(uuid);
        return policies.stream().map(p -> {
            PolicyResponse resp = new PolicyResponse();
            resp.setId(p.getId());
            resp.setAccessMode(p.getAccessMode() != null ? p.getAccessMode().name() : "PUBLIC");
            resp.setRoleName(p.getRoleName());
            resp.setAllowDownload(p.isAllowDownload());
            resp.setAllowPreview(p.isAllowPreview());
            resp.setAllowUpdate(p.isAllowUpdate());
            resp.setAllowDelete(p.isAllowDelete());
            resp.setAllowShare(p.isAllowShare());
            resp.setExpireTime(p.getExpireTime());
            return resp;
        }).toList();
    }

    @PutMapping("/{uuid}/policy")
    @Operation(summary = "替换资源的访问策略（覆盖写入）")
    public ResponseEntity<Void> updatePolicies(
            @PathVariable String uuid,
            @RequestBody List<PolicyRequest> policyRequests) {
        List<StorageAccessPolicy> policies = policyRequests.stream().map(req -> {
            StorageAccessPolicy p = new StorageAccessPolicy();
            p.setAccessMode(safeEnum(AccessMode.class, req.accessMode, AccessMode.PUBLIC));
            p.setRoleName(req.roleName);
            p.setAllowDownload(req.allowDownload != null ? req.allowDownload : true);
            p.setAllowPreview(req.allowPreview != null ? req.allowPreview : true);
            p.setAllowUpdate(req.allowUpdate != null ? req.allowUpdate : false);
            p.setAllowDelete(req.allowDelete != null ? req.allowDelete : false);
            p.setAllowShare(req.allowShare != null ? req.allowShare : false);
            return p;
        }).toList();

        policyRepo.replacePolicies(uuid, policies);
        return ResponseEntity.ok().build();
    }

    // ────────── DTOs ──────────

    public static class ShareRequest {
        /** 过期秒数：86400（1天）/ 604800（7天）/ 0（永久） */
        public int expireSeconds = 86400;
    }

    public static class ShareResponse {
        private Long id;
        private String shareToken;
        private String shareUrl;
        private java.time.LocalDateTime expireTime;
        private boolean expired;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getShareToken() { return shareToken; }
        public void setShareToken(String shareToken) { this.shareToken = shareToken; }
        public String getShareUrl() { return shareUrl; }
        public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }
        public java.time.LocalDateTime getExpireTime() { return expireTime; }
        public void setExpireTime(java.time.LocalDateTime expireTime) { this.expireTime = expireTime; }
        public boolean isExpired() { return expired; }
        public void setExpired(boolean expired) { this.expired = expired; }
    }

    public static class SignedUrlRequest {
        /** 签名有效期（秒），默认 3600（1小时） */
        public long expiresInSeconds = 3600;
    }

    public static class SignedUrlResponse {
        private String url;
        private long expiresInSeconds;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public long getExpiresInSeconds() { return expiresInSeconds; }
        public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
    }

    public static class PolicyRequest {
        public String accessMode;
        public String roleName;
        public Boolean allowDownload;
        public Boolean allowPreview;
        public Boolean allowUpdate;
        public Boolean allowDelete;
        public Boolean allowShare;
    }

    public static class PolicyResponse {
        private Long id;
        private String accessMode;
        private String roleName;
        private boolean allowDownload;
        private boolean allowPreview;
        private boolean allowUpdate;
        private boolean allowDelete;
        private boolean allowShare;
        private java.time.LocalDateTime expireTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getAccessMode() { return accessMode; }
        public void setAccessMode(String accessMode) { this.accessMode = accessMode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public boolean isAllowDownload() { return allowDownload; }
        public void setAllowDownload(boolean allowDownload) { this.allowDownload = allowDownload; }
        public boolean isAllowPreview() { return allowPreview; }
        public void setAllowPreview(boolean allowPreview) { this.allowPreview = allowPreview; }
        public boolean isAllowUpdate() { return allowUpdate; }
        public void setAllowUpdate(boolean allowUpdate) { this.allowUpdate = allowUpdate; }
        public boolean isAllowDelete() { return allowDelete; }
        public void setAllowDelete(boolean allowDelete) { this.allowDelete = allowDelete; }
        public boolean isAllowShare() { return allowShare; }
        public void setAllowShare(boolean allowShare) { this.allowShare = allowShare; }
        public java.time.LocalDateTime getExpireTime() { return expireTime; }
        public void setExpireTime(java.time.LocalDateTime expireTime) { this.expireTime = expireTime; }
    }

    // ────────── helpers ──────────

    private static <E extends Enum<E>> E safeEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
