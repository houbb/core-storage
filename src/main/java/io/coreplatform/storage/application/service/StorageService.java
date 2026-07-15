package io.coreplatform.storage.application.service;

import io.coreplatform.storage.api.response.StorageFileResponse;
import io.coreplatform.storage.application.domain.StorageFile;
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
import java.util.HexFormat;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final StorageFileRepository repository;
    private final StorageDriver driver;
    private final StorageProperties properties;

    public StorageService(StorageFileRepository repository, StorageDriver driver, StorageProperties properties) {
        this.repository = repository;
        this.driver = driver;
        this.properties = properties;
    }

    /**
     * 上传文件 — 元数据先入库，再通过 Driver 写磁盘。
     * 任何一步失败自动回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse upload(MultipartFile multipartFile) throws IOException {
        String originalName = multipartFile.getOriginalFilename();
        String extension = extractExtension(originalName);
        String mimeType = multipartFile.getContentType();

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storageName = uuid + ".bin";
        String relativePath = buildDatePath();

        // 1. 先存到临时目录，计算 SHA-256 和文件大小
        Path tempDir = Paths.get(properties.getLocal().getRoot(), ".temp");
        Files.createDirectories(tempDir);
        Path tempFile = tempDir.resolve(uuid + ".tmp");

        try {
            Files.copy(multipartFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            long size = Files.size(tempFile);
            String hash = computeSha256(tempFile);

            // 2. 插入元数据到数据库
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
            log.info("Metadata saved: id={}, uuid={}", saved.getId(), uuid);

            // 3. Driver 上传文件字节
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
     * 软删除 — 只标记 deleted=true，不删除物理文件（P8 生命周期统一清理）。
     */
    public void delete(Long id) {
        StorageFile file = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found: id=" + id));

        if (file.isDeleted()) {
            return; // 幂等
        }

        repository.softDelete(id);
        log.info("File soft-deleted: id={}", id);
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