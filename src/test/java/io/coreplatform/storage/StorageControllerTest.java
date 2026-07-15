package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.application.domain.StorageFile;
import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.application.service.StorageMetadataService;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.application.service.StorageService;
import io.coreplatform.storage.infrastructure.config.StorageProperties;
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
            return mock(StorageDriver.class);
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
        StorageService storageService(StorageFileRepository repo, StorageDriver driver,
                                       StorageProperties props, StorageMetadataService metadataService,
                                       StorageResourceService resourceService) {
            return new StorageService(repo, driver, props, metadataService, resourceService);
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
        when(repository.save(any())).thenAnswer(inv -> {
            var f = (StorageFile) inv.getArgument(0);
            f.setId(1L);
            return f;
        });
        when(metadataRepo.save(any())).thenAnswer(inv -> {
            var m = (StorageMetadata) inv.getArgument(0);
            m.setId(10L);
            return m;
        });

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
        StorageFile domain = new StorageFile();
        domain.setId(1L);
        domain.setOriginalName("test.pdf");
        domain.setSize(1024);
        domain.setDeleted(false);
        domain.setStatus("ACTIVE");

        when(repository.findById(1L)).thenReturn(Optional.of(domain));

        mvc.perform(get("/api/v1/storage/file/1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("test.pdf"))
                .andExpect(jsonPath("$.size").value(1024));
    }

    @Test
    void getInfoReturns404() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/storage/file/999/info"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("File not found"));
    }

    @Test
    void deleteReturns204() throws Exception {
        StorageFile domain = new StorageFile();
        domain.setId(1L);
        domain.setDeleted(false);
        domain.setStatus("ACTIVE");
        domain.setUuid("abc123");

        when(repository.findById(1L)).thenReturn(Optional.of(domain));

        mvc.perform(delete("/api/v1/storage/file/1"))
                .andExpect(status().isNoContent());

        verify(repository).softDelete(1L);
    }

    @Test
    void deleteReturns404ForMissingFile() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/v1/storage/file/999"))
                .andExpect(status().isNotFound());
    }
}