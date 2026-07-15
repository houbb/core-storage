package io.coreplatform.storage.application.service;

import io.coreplatform.storage.api.response.StorageFileResponse;
import io.coreplatform.storage.application.domain.StorageFile;
import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.application.domain.enums.ResourceType;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.HexFormat;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final StorageFileRepository repository;
    private final StorageDriver driver;
    private final StorageProperties properties;
    private final StorageMetadataService metadataService;
    private final StorageResourceService resourceService;
    private final StorageImageService imageService;

    public StorageService(StorageFileRepository repository, StorageDriver driver,
                           StorageProperties properties, StorageMetadataService metadataService,
                           StorageResourceService resourceService,
                           StorageImageService imageService) {
        this.repository = repository;
        this.driver = driver;
        this.properties = properties;
        this.metadataService = metadataService;
        this.resourceService = resourceService;
        this.imageService = imageService;
    }

    /**
     * 上传文件 — 元数据先入库，再通过 Driver 写磁盘。
     * P1：同步双写 storage_metadata + storage_metadata_index。
     * P2：当传入 resourceType 时自动创建 StorageResource。
     * 可选自动创建引用。
     * 任何一步失败自动回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse upload(MultipartFile multipartFile,
                                        String ownerType, String ownerId,
                                        String system, String module,
                                        String businessType, String businessId,
                                        String tags, String remark) throws IOException {
        return uploadInternal(multipartFile, ownerType, ownerId, system, module,
                businessType, businessId, tags, remark,
                null, null, null, null, null, null, null);
    }

    /**
     * P2：资源感知上传 — 携带 resourceType/category/description/visibility/tagList/props。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse uploadResource(MultipartFile multipartFile,
                                               String ownerType, String ownerId,
                                               String system, String module,
                                               String businessType, String businessId,
                                               String tags, String remark,
                                               String resourceType, String category,
                                               String description, String visibility,
                                               String accessMode,
                                               List<String> tagList, Map<String, String> props) throws IOException {
        return uploadInternal(multipartFile, ownerType, ownerId, system, module,
                businessType, businessId, tags, remark,
                resourceType, category, description, visibility, accessMode, tagList, props);
    }

    private StorageFileResponse uploadInternal(MultipartFile multipartFile,
                                                String ownerType, String ownerId,
                                                String system, String module,
                                                String businessType, String businessId,
                                                String tags, String remark,
                                                String resourceType, String category,
                                                String description, String visibility,
                                                String accessMode,
                                                List<String> tagList, Map<String, String> props) throws IOException {
        String originalName = multipartFile.getOriginalFilename();
        String extension = extractExtension(originalName);
        String mimeType = multipartFile.getContentType();

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storageName = uuid + ".bin";
        String relativePath = buildDatePath();
        String storageKey = (relativePath.isEmpty() ? "" : relativePath + "/") + storageName;

        // 1. 先存到临时目录，计算 SHA-256 和文件大小
        Path tempDir = Paths.get(properties.getLocal().getRoot(), ".temp");
        Files.createDirectories(tempDir);
        Path tempFile = tempDir.resolve(uuid + ".tmp");

        try {
            Files.copy(multipartFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            long size = Files.size(tempFile);
            String hash = computeSha256(tempFile);

            // 2. 插入元数据到 P0 storage_file 表（向后兼容）
            StorageFile file = new StorageFile();
            file.setUuid(uuid);
            file.setOriginalName(originalName != null ? originalName : "unnamed");
            file.setStorageName(storageName);
            file.setExtension(extension);
            file.setMimeType(mimeType);
            file.setSize(size);
            file.setStorageType("local");
            file.setRelativePath(relativePath);
            file.setHash(hash);
            file.setStatus("ACTIVE");
            file.setDeleted(false);

            StorageFile saved = repository.save(file);
            log.info("P0 Metadata saved: id={}, uuid={}", saved.getId(), uuid);

            // 3. 双写 P1 storage_metadata 表
            StorageMetadata metadata = new StorageMetadata();
            metadata.setUuid(uuid);
            metadata.setResourceName(originalName != null ? originalName : "unnamed");
            metadata.setOriginalName(originalName != null ? originalName : "unnamed");
            metadata.setExtension(extension);
            metadata.setMimeType(mimeType);
            metadata.setFileSize(size);
            metadata.setHashSha256(hash);
            metadata.setStorageDriver("local");
            metadata.setStorageKey(storageKey);
            metadata.setRelativePath(relativePath);
            metadata.setStorageName(storageName);
            metadata.setStorageType("local");
            metadata.setOwnerType(ownerType);
            metadata.setOwnerId(ownerId);
            metadata.setSystemName(system);
            metadata.setModuleName(module);
            metadata.setTags(tags);
            metadata.setRemark(remark);
            metadata.setStatus("UPLOADING");
            metadata.setDeleted(false);

            metadataService.saveMetadata(metadata);

            // 4. 写轻量索引
            metadataService.saveIndex(uuid, ownerType, ownerId, mimeType, module, tags, "ACTIVE");
            metadataService.updateStatus(uuid, "ACTIVE");
            log.info("P1 Metadata saved: uuid={}, status=ACTIVE", uuid);

            // 4b. P2：如果带了 resourceType 参数，自动创建 StorageResource
            if (resourceType != null && !resourceType.isBlank()) {
                // 如果未填 resourceType 或为空，按 MIME 自动推断
                String inferredType = resourceType;
                if ("AUTO".equalsIgnoreCase(inferredType) || inferredType.isBlank()) {
                    inferredType = ResourceType.fromMimeType(mimeType).name();
                }
                String resourceName = originalName != null ? originalName : "unnamed";
                List<String> tagListSafe = tagList != null ? tagList : new ArrayList<>();
                Map<String, String> propsSafe = props != null ? props : new HashMap<>();

                // 将逗号分隔的 tags 也合并到 tagList
                if (tags != null && !tags.isBlank()) {
                    for (String t : tags.split(",")) {
                        String trimmed = t.trim();
                        if (!trimmed.isEmpty() && !tagListSafe.contains(trimmed)) {
                            tagListSafe.add(trimmed);
                        }
                    }
                }

                resourceService.createResource(uuid, resourceName, inferredType,
                        category, description, ownerType, ownerId, visibility,
                        accessMode,
                        tagListSafe, propsSafe);
                resourceService.updateStatus(uuid, "READY");
                log.info("P2 Resource created: metadataUuid={}, type={}", uuid, inferredType);

                // 4c. P4：如果是 IMAGE 类型，自动触发 Image Runtime
                if (ResourceType.IMAGE.name().equals(inferredType)) {
                    imageService.processUploadedImage(uuid, saved, tempFile);
                    log.info("P4 Image Runtime triggered: metadataUuid={}", uuid);
                }
            }

            // 5. 可选：上传时自动创建初始引用
            if (system != null && !system.isBlank()
                    && businessType != null && !businessType.isBlank()
                    && businessId != null && !businessId.isBlank()) {
                metadataService.createReference(uuid, system, module, businessType, businessId);
                log.info("Auto-reference created: uuid={}, businessType={}, businessId={}", uuid, businessType, businessId);
            }

            // 6. Driver 上传文件字节
            try (InputStream in = new FileInputStream(tempFile.toFile())) {
                driver.upload(relativePath, storageName, in);
            }
            log.info("File stored: id={}, name={}", saved.getId(), storageName);

            return toResponse(saved);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 下载文件字节流 — Controller 层负责封装 HTTP 响应。
     */
    public FileDownloadResult download(Long id) throws IOException {
        StorageFile file = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found: id=" + id));

        if (!file.isValid()) {
            throw new FileNotFoundException("File has been deleted: id=" + id);
        }

        InputStream in = driver.download(file.getRelativePath(), file.getStorageName());
        return new FileDownloadResult(file, in);
    }

    /**
     * 软删除 — 标记 deleted=true，同步软删除 metadata。
     */
    public void delete(Long id) {
        StorageFile file = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found: id=" + id));

        if (file.isDeleted()) {
            return; // 幂等
        }

        repository.softDelete(id);
        metadataService.softDelete(file.getUuid());
        log.info("File soft-deleted: id={}, uuid={}", id, file.getUuid());
    }

    /**
     * 查询文件元数据
     */
    public StorageFileResponse getInfo(Long id) {
        StorageFile file = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found: id=" + id));

        return toResponse(file);
    }

    // ---- private helpers ----

    private StorageFileResponse toResponse(StorageFile file) {
        return new StorageFileResponse(
                file.getId(),
                "/api/v1/storage/file/" + file.getId(),
                file.getOriginalName(),
                file.getSize()
        );
    }

    private String buildDatePath() {
        if (!properties.getLocal().isDatePath()) {
            return "";
        }
        LocalDate now = LocalDate.now();
        return String.format("%04d/%02d/%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String computeSha256(Path file) throws IOException {
        try (InputStream in = new FileInputStream(file.toFile())) {
            MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ---- inner types ----

    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(String message) {
            super(message);
        }
    }

    public record FileDownloadResult(StorageFile metadata, InputStream stream) {
    }
}