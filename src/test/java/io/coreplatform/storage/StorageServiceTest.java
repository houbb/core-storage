package io.coreplatform.storage.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.coreplatform.storage.api.response.StorageFileResponse;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;

class StorageServiceTest {

    private StorageService service;
    private StorageFileRepository repository;
    private StorageDriver driver;
    private StorageProperties properties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        StorageProperties.Local local = new StorageProperties.Local();
        local.setRoot(tempDir.toString());
        local.setDatePath(false);
        properties.setLocal(local);

        driver = mock(StorageDriver.class);
        repository = mock(StorageFileRepository.class);

        service = new StorageService(repository, driver, properties);
    }

    @Test
    void uploadReturnsResponseWithIdAndUrl() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes());

        when(repository.save(any())).thenAnswer(inv -> {
            var f = (io.coreplatform.storage.application.domain.StorageFile) inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        StorageFileResponse resp = service.upload(file);

        assertEquals(1L, resp.getId());
        assertEquals("/api/v1/storage/file/1", resp.getDownloadUrl());
        assertEquals("test.txt", resp.getFilename());
        assertEquals(5, resp.getSize());

        verify(driver).upload(anyString(), anyString(), any());
        verify(repository).save(any());
    }

    @Test
    void deleteThrowsWhenFileNotFound() {
        when(repository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(StorageService.FileNotFoundException.class,
                () -> service.delete(999L));
    }

    @Test
    void getInfoThrowsWhenFileNotFound() {
        when(repository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(StorageService.FileNotFoundException.class,
                () -> service.getInfo(999L));
    }
}