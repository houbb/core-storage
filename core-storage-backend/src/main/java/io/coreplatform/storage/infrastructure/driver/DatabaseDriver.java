package io.coreplatform.storage.infrastructure.driver;

import io.coreplatform.storage.application.domain.enums.DriverType;
import io.coreplatform.storage.application.domain.enums.StorageCapability;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageDriverBlobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 数据库存储驱动 — 将文件内容以 BLOB 形式存储在数据库中。
 * <p>
 * 适用于需要多节点共享文件而无需额外组件（如 NAS、MinIO）的场景。
 * 默认通过 JdbcTemplate 操作数据库，同时兼容 SQLite 和 MySQL。
 * <p>
 * 这是 StorageDriver 的正式实现之一，与 LocalDiskDriver 同等地位，
 * 不是临时方案，而是 MVP、多节点部署和企业演进中的正式存储后端。
 */
public class DatabaseDriver implements StorageDriver {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDriver.class);
    private static final String VERSION = "1.0.0";

    private final JdbcTemplate jdbc;
    private final StorageDriverBlobRepository blobRepo;

    /**
     * @param jdbc    用于健康检查的 JdbcTemplate
     * @param blobRepo BLOB 持久化仓库
     */
    public DatabaseDriver(JdbcTemplate jdbc, StorageDriverBlobRepository blobRepo) {
        this.jdbc = jdbc;
        this.blobRepo = blobRepo;
        log.info("DatabaseDriver initialized");
    }

    // ---- SPI: identity ----

    @Override
    public DriverType type() {
        return DriverType.DATABASE;
    }

    @Override
    public Set<StorageCapability> capabilities() {
        return Set.of(StorageCapability.TRANSACTION);
    }

    // ---- SPI: basic CRUD ----

    @Override
    public void upload(String relativePath, String storageName, InputStream in) throws IOException {
        String storageKey = buildKey(relativePath, storageName);
        byte[] data = in.readAllBytes();
        blobRepo.insert(storageKey, data, "application/octet-stream");
        log.debug("DatabaseDriver uploaded: key={}, size={}", storageKey, data.length);
    }

    @Override
    public InputStream download(String relativePath, String storageName) throws IOException {
        String storageKey = buildKey(relativePath, storageName);
        StorageDriverBlobRepository.BlobRow row = blobRepo.findByStorageKey(storageKey);
        if (row == null || row.content == null) {
            throw new IOException("Blob not found: " + storageKey);
        }
        return new ByteArrayInputStream(row.content);
    }

    @Override
    public boolean delete(String relativePath, String storageName) throws IOException {
        String storageKey = buildKey(relativePath, storageName);
        boolean deleted = blobRepo.deleteByStorageKey(storageKey);
        log.debug("DatabaseDriver deleted: key={}, success={}", storageKey, deleted);
        return deleted;
    }

    @Override
    public boolean exists(String relativePath, String storageName) {
        String storageKey = buildKey(relativePath, storageName);
        return blobRepo.exists(storageKey);
    }

    // ---- SPI: file operations (P5) ----

    @Override
    public boolean move(String sourceRelativePath, String sourceStorageName,
                        String targetRelativePath, String targetStorageName) throws IOException {
        String sourceKey = buildKey(sourceRelativePath, sourceStorageName);
        String targetKey = buildKey(targetRelativePath, targetStorageName);

        StorageDriverBlobRepository.BlobRow row = blobRepo.findByStorageKey(sourceKey);
        if (row == null) {
            return false;
        }

        boolean updated = blobRepo.updateStorageKey(sourceKey, targetKey, row.content, row.contentType);
        log.debug("DatabaseDriver moved: {} → {}, success={}", sourceKey, targetKey, updated);
        return updated;
    }

    @Override
    public String copy(String sourceRelativePath, String sourceStorageName,
                       String targetRelativePath, String targetStorageName) throws IOException {
        String sourceKey = buildKey(sourceRelativePath, sourceStorageName);
        String targetKey = buildKey(targetRelativePath, targetStorageName);

        StorageDriverBlobRepository.BlobRow row = blobRepo.findByStorageKey(sourceKey);
        if (row == null) {
            throw new IOException("Source blob not found: " + sourceKey);
        }

        blobRepo.insert(targetKey, row.content, row.contentType);
        log.debug("DatabaseDriver copied: {} → {}", sourceKey, targetKey);
        return targetKey;
    }

    // ---- SPI: metadata & access (P5) ----

    @Override
    public Map<String, Object> metadata(String relativePath, String storageName) throws IOException {
        String storageKey = buildKey(relativePath, storageName);
        StorageDriverBlobRepository.BlobRow row = blobRepo.findByStorageKey(storageKey);
        if (row == null) {
            throw new IOException("Blob not found: " + storageKey);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("size", row.size);
        meta.put("lastModified", row.updateTime != null
                ? row.updateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : row.createTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        meta.put("contentType", row.contentType != null ? row.contentType : "application/octet-stream");
        return meta;
    }

    @Override
    public String url(String relativePath, String storageName, long ttlSeconds) {
        String storageKey = buildKey(relativePath, storageName);
        return "/api/v1/storage/database-blob/" + storageKey;
    }

    // ---- SPI: health (P5) ----

    @Override
    public boolean health() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.warn("DatabaseDriver health check failed", e);
            return false;
        }
    }

    // ---- helpers ----

    private String buildKey(String relativePath, String storageName) {
        String normalizedPath = relativePath == null || relativePath.isBlank()
                ? "" : relativePath.replace('\\', '/');
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        if (normalizedPath.isEmpty()) {
            return storageName;
        }
        return normalizedPath + "/" + storageName;
    }

    public String getVersion() {
        return VERSION;
    }
}
