package io.coreplatform.storage.infrastructure.driver;

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

/**
 * 本地磁盘存储驱动 — P0 唯一实现。
 * 文件按 root/yyyy/MM/dd/uuid.bin 存储，路径分隔符统一用 /，内部自动转换。
 */
public class LocalDiskDriver implements StorageDriver {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskDriver.class);

    private final Path root;
    private final boolean datePath;

    public LocalDiskDriver(StorageProperties.Local props) {
        this.root = Paths.get(props.getRoot()).toAbsolutePath().normalize();
        this.datePath = props.isDatePath();
        log.info("LocalDiskDriver initialized: root={}, datePath={}", this.root, this.datePath);
    }

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
        // 统一路径分隔符
        String normalized = relativePath.replace('\\', '/');
        return root.resolve(normalized).normalize();
    }
}