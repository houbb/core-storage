package io.coreplatform.storage.infrastructure.driver;

import io.coreplatform.storage.application.domain.enums.DriverType;
import io.coreplatform.storage.application.domain.enums.StorageCapability;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 本地磁盘存储驱动 — 文件按 root/{relativePath}/{storageName} 存储。
 * <p>
 * P0 仅实现了 upload/download/delete/exists。
 * P5 扩展为完整的 StorageDriver，包括 move/copy/metadata/url/health 及能力声明。
 */
public class LocalDiskDriver implements StorageDriver {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskDriver.class);
    private static final String VERSION = "1.0.0";

    private final Path root;
    private final boolean datePath;

    public LocalDiskDriver(StorageProperties.Local props) {
        this.root = Paths.get(props.getRoot()).toAbsolutePath().normalize();
        this.datePath = props.isDatePath();
        log.info("LocalDiskDriver initialized: root={}, datePath={}", this.root, this.datePath);
    }

    // ---- SPI: identity ----

    @Override
    public DriverType type() {
        return DriverType.LOCAL;
    }

    @Override
    public Set<StorageCapability> capabilities() {
        return Set.of(StorageCapability.STREAMING);
    }

    // ---- SPI: basic CRUD (P0) ----

    @Override
    public void upload(String relativePath, String storageName, InputStream in) throws IOException {
        Path targetDir = resolveDir(relativePath);
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(storageName);
        Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Uploaded: {}", targetFile);
    }

    @Override
    public InputStream download(String relativePath, String storageName) throws IOException {
        Path file = resolveFile(relativePath, storageName);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + file);
        }
        return new FileInputStream(file.toFile());
    }

    @Override
    public boolean delete(String relativePath, String storageName) throws IOException {
        Path file = resolveFile(relativePath, storageName);
        if (!Files.exists(file)) {
            return false;
        }
        Files.delete(file);
        log.debug("Deleted: {}", file);
        return true;
    }

    @Override
    public boolean exists(String relativePath, String storageName) {
        Path file = resolveFile(relativePath, storageName);
        return Files.exists(file);
    }

    // ---- SPI: file operations (P5) ----

    @Override
    public boolean move(String sourceRelativePath, String sourceStorageName,
                        String targetRelativePath, String targetStorageName) throws IOException {
        Path source = resolveFile(sourceRelativePath, sourceStorageName);
        if (!Files.exists(source)) {
            return false;
        }
        Path targetDir = resolveDir(targetRelativePath);
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(targetStorageName);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Moved: {} → {}", source, target);
        return true;
    }

    @Override
    public String copy(String sourceRelativePath, String sourceStorageName,
                       String targetRelativePath, String targetStorageName) throws IOException {
        Path source = resolveFile(sourceRelativePath, sourceStorageName);
        if (!Files.exists(source)) {
            throw new IOException("Source file not found: " + source);
        }
        Path targetDir = resolveDir(targetRelativePath);
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(targetStorageName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Copied: {} → {}", source, target);
        return target.toAbsolutePath().toString();
    }

    // ---- SPI: metadata & access (P5) ----

    @Override
    public Map<String, Object> metadata(String relativePath, String storageName) throws IOException {
        Path file = resolveFile(relativePath, storageName);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + file);
        }
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("size", attrs.size());
        meta.put("lastModified", attrs.lastModifiedTime().toMillis());
        meta.put("contentType", Files.probeContentType(file));
        return meta;
    }

    @Override
    public String url(String relativePath, String storageName, long ttlSeconds) {
        Path file = resolveFile(relativePath, storageName);
        return file.toUri().toString();
    }

    // ---- SPI: health (P5) ----

    @Override
    public boolean health() {
        return Files.exists(root) && Files.isWritable(root);
    }

    // ---- package-private helpers ----

    /**
     * 解析完整文件路径：root / relativePath / storageName
     */
    Path resolveFile(String relativePath, String storageName) {
        return resolveDir(relativePath).resolve(storageName);
    }

    /**
     * 解析目录路径：root / relativePath
     */
    Path resolveDir(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return root;
        }
        String normalized = relativePath.replace('\\', '/');
        return root.resolve(normalized).normalize();
    }

    /**
     * 获取驱动版本号。
     */
    public String getVersion() {
        return VERSION;
    }
}
