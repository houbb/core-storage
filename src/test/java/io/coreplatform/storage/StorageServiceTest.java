package io.coreplatform.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.coreplatform.storage.api.response.StorageFileResponse;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.application.domain.enums.DriverType;
import io.coreplatform.storage.application.service.ReplicationService;
import io.coreplatform.storage.application.service.StorageImageService;
import io.coreplatform.storage.application.service.StorageMetadataService;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.application.service.StorageService;
import io.coreplatform.storage.application.service.StorageVersionService;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
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
    private StorageDriverFactory driverFactory;
    private StorageProperties properties;
    private StorageMetadataService metadataService;
    private StorageResourceService resourceService;
    private StorageImageService imageService;
    private ReplicationService replicationService;
    private StorageVersionService versionService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        StorageProperties.Local local = new StorageProperties.Local();
        local.setRoot(tempDir.toString());
        local.setDatePath(false);
        properties.setLocal(local);
        properties.setImage(new StorageProperties.Image());
        properties.setReplication(new StorageProperties.Replication());

        driver = mock(StorageDriver.class);
        when(driver.type()).thenReturn(DriverType.LOCAL);
        driverFactory = mock(StorageDriverFactory.class);
        when(driverFactory.getDriverForProfile(isNull())).thenReturn(driver);
        when(driverFactory.getDriverForProfile(anyString())).thenReturn(driver);

        repository = mock(StorageFileRepository.class);
        metadataService = mock(StorageMetadataService.class);
        resourceService = mock(StorageResourceService.class);
        imageService = mock(StorageImageService.class);
        replicationService = mock(ReplicationService.class);
        versionService = mock(StorageVersionService.class);

        service = new StorageService(repository, driverFactory, properties, metadataService, resourceService, imageService, replicationService, versionService);
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
        when(metadataService.saveMetadata(any())).thenAnswer(inv -> inv.getArgument(0));

        StorageFileResponse resp = service.upload(file, null, null, null, null, null, null, null, null);

        assertEquals(1L, resp.getId());
        assertEquals("/api/v1/storage/file/1", resp.getDownloadUrl());
        assertEquals("test.txt", resp.getFilename());
        assertEquals(5, resp.getSize());

        verify(driver).upload(anyString(), anyString(), any());
        verify(repository).save(any());
        verify(metadataService).saveMetadata(any());
    }

    @Test
    void uploadWithProfileResolvesCorrectDriver() throws IOException {
        StorageDriver profileDriver = mock(StorageDriver.class);
        when(profileDriver.type()).thenReturn(DriverType.LOCAL);
        when(driverFactory.getDriverForProfile("image")).thenReturn(profileDriver);

        MultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "hello".getBytes());

        when(repository.save(any())).thenAnswer(inv -> {
            var f = (io.coreplatform.storage.application.domain.StorageFile) inv.getArgument(0);
            f.setId(2L);
            return f;
        });
        when(metadataService.saveMetadata(any())).thenAnswer(inv -> inv.getArgument(0));

        service.uploadWithProfile(file, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, "image");

        verify(driverFactory).getDriverForProfile("image");
        verify(profileDriver).upload(anyString(), anyString(), any());
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
