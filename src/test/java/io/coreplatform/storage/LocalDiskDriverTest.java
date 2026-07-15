package io.coreplatform.storage.infrastructure.driver;

import static org.junit.jupiter.api.Assertions.*;

import io.coreplatform.storage.infrastructure.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class LocalDiskDriverTest {

    private LocalDiskDriver driver;
    private StorageProperties.Local props;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        props = new StorageProperties.Local();
        props.setRoot(tempDir.toString());
        props.setDatePath(false); // 关闭日期路径，简化测试
        driver = new LocalDiskDriver(props);
    }

    @Test
    void uploadAndDownload() throws IOException {
        byte[] content = "Hello, core-storage!".getBytes();
        driver.upload("test", "file.bin", new ByteArrayInputStream(content));

        assertTrue(driver.exists("test", "file.bin"));

        try (InputStream in = driver.download("test", "file.bin")) {
            byte[] result = in.readAllBytes();
            assertArrayEquals(content, result);
        }
    }

    @Test
    void existsReturnsFalseWhenMissing() {
        assertFalse(driver.exists("test", "nonexistent.bin"));
    }

    @Test
    void deleteRemovesFile() throws IOException {
        driver.upload("test", "file.bin", new ByteArrayInputStream("data".getBytes()));
        assertTrue(driver.exists("test", "file.bin"));

        boolean deleted = driver.delete("test", "file.bin");
        assertTrue(deleted);
        assertFalse(driver.exists("test", "file.bin"));
    }

    @Test
    void deleteReturnsFalseForMissingFile() throws IOException {
        assertFalse(driver.delete("test", "nonexistent.bin"));
    }

    @Test
    void downloadThrowsForMissingFile() {
        assertThrows(IOException.class, () -> driver.download("test", "nonexistent.bin"));
    }

    @Test
    void filesAreCreatedUnderRoot() throws IOException {
        driver.upload("sub/dir", "x.bin", new ByteArrayInputStream("x".getBytes()));
        Path expected = tempDir.resolve("sub/dir/x.bin");
        assertTrue(Files.exists(expected));
    }
}