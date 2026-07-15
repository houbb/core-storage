package io.coreplatform.storage.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.coreplatform.storage.api.response.StorageMetadataResponse;
import io.coreplatform.storage.api.response.StorageReferenceResponse;
import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.application.domain.StorageReference;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataIndexRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class StorageMetadataServiceTest {

    private StorageMetadataService metadataService;
    private StorageMetadataRepository metadataRepo;
    private StorageReferenceRepository referenceRepo;
    private StorageMetadataIndexRepository indexRepo;

    @BeforeEach
    void setUp() {
        metadataRepo = mock(StorageMetadataRepository.class);
        referenceRepo = mock(StorageReferenceRepository.class);
        indexRepo = mock(StorageMetadataIndexRepository.class);
        metadataService = new StorageMetadataService(metadataRepo, referenceRepo, indexRepo);
    }

    // ---- UUID 查询 ----

    @Test
    void getByUuidReturnsMetadata() {
        StorageMetadata m = buildMetadata("uuid-001", "test.png", "ACTIVE");
        when(metadataRepo.findByUuid("uuid-001")).thenReturn(Optional.of(m));
        when(referenceRepo.countByMetadataUuid("uuid-001")).thenReturn(3);

        StorageMetadataResponse resp = metadataService.getByUuid("uuid-001");

        assertEquals("uuid-001", resp.getUuid());
        assertEquals("test.png", resp.getOriginalName());
        assertEquals(3, resp.getReferenceCount());
        assertEquals("ACTIVE", resp.getStatus());
    }

    @Test
    void getByUuidThrowsWhenNotFound() {
        when(metadataRepo.findByUuid("not-exist")).thenReturn(Optional.empty());

        assertThrows(StorageMetadataService.MetadataNotFoundException.class,
                () -> metadataService.getByUuid("not-exist"));
    }

    // ---- 搜索 ----

    @Test
    void searchReturnsPaginatedResults() {
        when(metadataRepo.countSearch(any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(25);
        when(metadataRepo.search(any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(buildMetadata("uuid-001", "file1.png", "ACTIVE"),
                        buildMetadata("uuid-002", "file2.jpg", "REFERENCED")));
        when(referenceRepo.countByMetadataUuid(anyString())).thenReturn(0);

        var result = metadataService.search("file", null, "ACTIVE", null,
                null, null, null, null, null, null, null, "createTime", "desc", 1, 20);

        assertEquals(2, result.getItems().size());
        assertEquals(25, result.getTotal());
        assertEquals(2, result.getTotalPages());
    }

    // ---- 引用创建 + 状态机 ----

    @Test
    void createReferenceTransitionsActiveToReferenced() {
        StorageMetadata m = buildMetadata("uuid-001", "doc.pdf", "ACTIVE");
        when(metadataRepo.findByUuid("uuid-001")).thenReturn(Optional.of(m));
        when(referenceRepo.countByMetadataUuid("uuid-001")).thenReturn(1);
        when(referenceRepo.save(any())).thenAnswer(inv -> {
            StorageReference ref = inv.getArgument(0);
            ref.setId(100L);
            return ref;
        });

        StorageReferenceResponse resp = metadataService.createReference(
                "uuid-001", "core-user", "avatar", "user", "1001");

        assertNotNull(resp);
        assertEquals(100L, resp.getId());
        verify(metadataRepo).updateStatus("uuid-001", "REFERENCED");
        verify(indexRepo).updateStatus("uuid-001", "REFERENCED");
    }

    @Test
    void createReferenceSecondRefDoesNotReTransition() {
        StorageMetadata m = buildMetadata("uuid-001", "doc.pdf", "REFERENCED");
        when(metadataRepo.findByUuid("uuid-001")).thenReturn(Optional.of(m));
        when(referenceRepo.countByMetadataUuid("uuid-001")).thenReturn(1).thenReturn(2);
        when(referenceRepo.save(any())).thenAnswer(inv -> {
            StorageReference ref = inv.getArgument(0);
            ref.setId(101L);
            return ref;
        });

        metadataService.createReference("uuid-001", "core-plugin", "plugin-market", "plugin", "p001");

        // 已是 REFERENCED，不应再次切状态
        verify(metadataRepo, never()).updateStatus(eq("uuid-001"), eq("REFERENCED"));
    }

    // ---- 引用删除 + 状态机 ----

    @Test
    void deleteLastReferenceTransitionsToUnreferenced() {
        when(referenceRepo.countByMetadataUuid("uuid-001")).thenReturn(0);

        metadataService.deleteReferenceByUuid(1L, "uuid-001");

        verify(referenceRepo).deleteById(1L);
        verify(metadataRepo).updateStatus("uuid-001", "UNREFERENCED");
        verify(indexRepo).updateStatus("uuid-001", "UNREFERENCED");
    }

    @Test
    void deleteReferenceWithRemainingRefsDoesNotTransition() {
        when(referenceRepo.countByMetadataUuid("uuid-001")).thenReturn(1);

        metadataService.deleteReferenceByUuid(1L, "uuid-001");

        verify(referenceRepo).deleteById(1L);
        verify(metadataRepo, never()).updateStatus(eq("uuid-001"), eq("UNREFERENCED"));
    }

    // ---- 引用列表 ----

    @Test
    void getReferencesReturnsList() {
        StorageReference ref1 = new StorageReference();
        ref1.setId(1L);
        ref1.setMetadataUuid("uuid-001");
        ref1.setSystemName("core-user");
        ref1.setBusinessType("user");
        ref1.setBusinessId("1001");

        when(referenceRepo.findByMetadataUuid("uuid-001")).thenReturn(List.of(ref1));

        var refs = metadataService.getReferences("uuid-001");

        assertEquals(1, refs.size());
        assertEquals("core-user", refs.get(0).getSystemName());
    }

    // ---- 软删除 ----

    @Test
    void softDeleteUpdatesBothTables() {
        metadataService.softDelete("uuid-001");

        verify(metadataRepo).softDelete("uuid-001");
        verify(indexRepo).updateStatus("uuid-001", "SOFT_DELETED");
    }

    // ---- helpers ----

    private StorageMetadata buildMetadata(String uuid, String name, String status) {
        StorageMetadata m = new StorageMetadata();
        m.setId(1L);
        m.setUuid(uuid);
        m.setOriginalName(name);
        m.setResourceName(name);
        m.setStatus(status);
        m.setFileSize(1024L);
        m.setHashSha256("abc123def456");
        m.setStorageDriver("local");
        m.setStorageKey("2026/07/15/" + uuid + ".bin");
        m.setMimeType("image/png");
        m.setCreateTime(LocalDateTime.now());
        m.setDeleted(false);
        return m;
    }
}