package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.StorageResourceShare;
import io.coreplatform.storage.api.security.AccessContext;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceShareRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 资源分享服务。
 */
@Service
public class StorageShareService {

    private final StorageResourceShareRepository shareRepo;
    private final StorageResourceRepository resourceRepo;

    public StorageShareService(StorageResourceShareRepository shareRepo,
                                StorageResourceRepository resourceRepo) {
        this.shareRepo = shareRepo;
        this.resourceRepo = resourceRepo;
    }

    /**
     * 创建分享链接。
     * @param expireSeconds 86400（1天）/ 604800（7天）/ 0（永久）
     */
    public StorageResourceShare createShare(String resourceUuid, int expireSeconds, AccessContext ctx) {
        StorageResource resource = resourceRepo.findByResourceUuid(resourceUuid)
                .orElseThrow(() -> new StorageResourceService.ResourceNotFoundException(
                        "Resource not found: uuid=" + resourceUuid));

        StorageResourceShare share = new StorageResourceShare();
        share.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        share.setResourceUuid(resourceUuid);
        share.setExpireSeconds(expireSeconds);
        share.setCreatorId(ctx.getUserId());

        if (expireSeconds > 0) {
            share.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        } else {
            // 永久分享：expireTime = 2099-12-31
            share.setExpireTime(LocalDateTime.of(2099, 12, 31, 23, 59, 59));
        }

        return shareRepo.save(share);
    }

    /**
     * 通过 token 验证并返回分享（如果未过期）。
     */
    public Optional<StorageResourceShare> validateToken(String shareToken) {
        return shareRepo.findByToken(shareToken)
                .filter(share -> !share.isExpired());
    }

    /**
     * 查询某资源的所有分享链接。
     */
    public List<StorageResourceShare> listShares(String resourceUuid) {
        return shareRepo.findByResourceUuid(resourceUuid);
    }

    /**
     * 撤销分享链接。
     */
    public boolean revokeShare(Long shareId) {
        return shareRepo.deleteById(shareId) > 0;
    }

    /**
     * 清理已过期的分享。
     */
    public int cleanExpired() {
        return shareRepo.deleteExpired();
    }
}