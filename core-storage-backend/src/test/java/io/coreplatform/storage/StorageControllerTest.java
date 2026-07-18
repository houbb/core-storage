package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageFileResponse;
import io.coreplatform.storage.application.domain.StorageFile;
import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.application.domain.enums.DriverType;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.application.service.StorageImageService;
import io.coreplatform.storage.application.service.StorageMetadataService;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.application.service.StorageService;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageFileRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataIndexRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({StorageController.class, StorageMetadataController.class})
@Import(StorageControllerTest.TestConfig.class)
class StorageControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StorageFileRepository repository;
    @Autowired
    private StorageDriver driver;
    @Autowired
    private StorageDriverFactory driverFactory;
    @Autowired
    private StorageService storageService;
    @Autowired
    private StorageMetadataRepository metadataRepo;
    @Autowired
    private StorageReferenceRepository referenceRepo;
    @Autowired
    private StorageMetadataIndexRepository indexRepo;

    @TestConfiguration
    static class TestConfig {

        @Bean
        StorageFileRepository storageFileRepository() {
            return mock(StorageFileRepository.class);
        }

        @Bean
        StorageDriver storageDriver() {
            StorageDriver driver = mock(StorageDriver.class);
            when(driver.type()).thenReturn(DriverType.LOCAL);
            return driver;
        }

        @Bean
        StorageDriverFactory storageDriverFactory(StorageDriver driver) {
            StorageDriverFactory factory = mock(StorageDriverFactory.class);
            when(factory.getDriverForProfile(any())).thenReturn(driver);
            return factory;
        }

        @Bean
        StorageProperties storageProperties() {
            StorageProperties props = new StorageProperties();
            StorageProperties.Local local = new StorageProperties.Local();
            local.setRoot(System.getProperty("java.io.tmpdir") + "/core-storage-test");
            local.setDatePath(false);
            props.setLocal(local);
            return props;
        }

        @Bean
        StorageMetadataRepository storageMetadataRepository() {
            return mock(StorageMetadataRepository.class);
        }

        @Bean
        StorageReferenceRepository storageReferenceRepository() {
            return mock(StorageReferenceRepository.class);
        }

        @Bean
        StorageMetadataIndexRepository storageMetadataIndexRepository() {
            return mock(StorageMetadataIndexRepository.class);
        }

        @Bean
        StorageMetadataService storageMetadataService(StorageMetadataRepository metadataRepo,
                                                         StorageReferenceRepository referenceRepo,
                                                         StorageMetadataIndexRepository indexRepo,
                                                         StorageResourceService resourceService) {
            return new StorageMetadataService(metadataRepo, referenceRepo, indexRepo, resourceService);
        }

        @Bean
        StorageResourceService storageResourceService() {
            return mock(StorageResourceService.class);
        }

        @Bean
        StorageImageService storageImageService() {
            return mock(StorageImageService.class);
        }

        @Bean
        StorageService storageService() {
            return mock(StorageService.class);
        }

        @Bean
        StorageController storageController(StorageService service) {
            return new StorageController(service);
        }

        @Bean
        StorageMetadataController storageMetadataController(StorageMetadataService metadataService) {
            return new StorageMetadataController(metadataService);
        }

        @Bean
        io.coreplatform.storage.api.exception.GlobalExceptionHandler globalExceptionHandler() {
            return new io.coreplatform.storage.api.exception.GlobalExceptionHandler();
        }
    }

    @BeforeEach
    void resetMocks() {
        reset(repository, driver, metadataRepo, referenceRepo, indexRepo);
    }

    @Test
    void uploadReturnsJson() throws Exception {
        when(storageService.upload(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StorageFileResponse(1L, "/api/v1/storage/file/1", "hello.txt", 11));

        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello world".getBytes());

        mvc.perform(multipart("/api/v1/storage/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/storage/file/1"))
                .andExpect(jsonPath("$.filename").value("hello.txt"));
    }

    @Test
    void getInfoReturnsMetadata() throws Exception {
        when(storageService.getInfo(1L))
                .thenReturn(new StorageFileResponse(1L, "/api/v1/storage/file/1", "test.pdf", 1024));

        mvc.perform(get("/api/v1/storage/file/1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("test.pdf"))
                .andExpect(jsonPath("$.size").value(1024));
    }

    @Test
    void getInfoReturns404() throws Exception {
        when(storageService.getInfo(999L))
                .thenThrow(new StorageService.FileNotFoundException("File not found: id=999"));

        mvc.perform(get("/api/v1/storage/file/999/info"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("File not found"));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(storageService).delete(1L);

        mvc.perform(delete("/api/v1/storage/file/1"))
                .andExpect(status().isNoContent());

        verify(storageService).delete(1L);
    }

    @Test
    void deleteReturns404ForMissingFile() throws Exception {
        doThrow(new StorageService.FileNotFoundException("File not found: id=999"))
                .when(storageService).delete(999L);

        mvc.perform(delete("/api/v1/storage/file/999"))
                .andExpect(status().isNotFound());
    }
}