package io.coreplatform.storage.application.service;

import io.coreplatform.storage.api.response.StorageResourceResponse;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.enums.ResourceCategory;
import io.coreplatform.storage.application.domain.enums.ResourceStatus;
import io.coreplatform.storage.application.domain.enums.ResourceType;
import io.coreplatform.storage.application.domain.enums.Visibility;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourcePropertyRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceTagRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StorageResourceServiceTest {

    private StorageResourceService resourceService;
    private StorageResourceRepository resourceRepo;
    private StorageResourceTagRepository tagRepo;
    private StorageResourcePropertyRepository propertyRepo;
    private StorageReferenceRepository referenceRepo;

    @BeforeEach
    void setUp() {
        resourceRepo = mock(StorageResourceRepository.class);
        tagRepo = mock(StorageResourceTagRepository.class);
        propertyRepo = mock(StorageResourcePropertyRepository.class);
        referenceRepo = mock(StorageReferenceRepository.class);
        resourceService = new StorageResourceService(resourceRepo, tagRepo, propertyRepo, referenceRepo);
    }

    // ---- 创建资源 ----

    @Test
    void createResourceSavesAndReturns() {
        when(resourceRepo.save(any())).thenAnswer(inv -> {
            StorageResource r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        StorageResource result = resourceService.createResource(
                "md-uuid-001", "avatar.png", "IMAGE", "AVATAR", "用户头像",
                "USER", "1001", "PUBLIC", null,
                List.of("dark", "round"), null);

        assertNotNull(result);
        assertEquals("md-uuid-001", result.getMetadataUuid());
        assertEquals("avatar.png", result.getResourceName());
        assertEquals(ResourceType.IMAGE, result.getResourceType());
        assertEquals(ResourceCategory.AVATAR, result.getCategory());
        assertEquals(Visibility.PUBLIC, result.getVisibility());
        assertEquals(ResourceStatus.UPLOADING, result.getStatus());
        assertNotNull(result.getResourceUuid());
        verify(tagRepo).replaceTags(anyString(), eq(List.of("dark", "round")));
    }

    @Test
    void createResourceDefaultsCategoryToOther() {
        when(resourceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StorageResource result = resourceService.createResource(
                "md-uuid-002", "test.zip", null, null, "", null, null, null, null, null, null);

        assertEquals(ResourceType.OTHER, result.getResourceType());
        assertEquals(ResourceCategory.OTHER, result.getCategory());
        assertEquals(Visibility.PUBLIC, result.getVisibility());
    }

    @Test
    void createResourceWithProperties() {
        when(resourceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> props = new LinkedHashMap<>();
        props.put("width", "1024");
        props.put("height", "768");

        resourceService.createResource("md-uuid-003", "banner.jpg", "IMAGE", "BANNER",
                "", "SYSTEM", "sys-1", "PUBLIC", null, null, props);

        verify(propertyRepo).setProperties(anyString(), eq(props));
    }

    // ---- 查询 ----

    @Test
    void getByUuidReturnsDetail() {
        StorageResource r = buildResource("res-uuid-001", "md-uuid-001", "avatar.png",
                ResourceType.IMAGE, ResourceCategory.AVATAR, ResourceStatus.READY);
        when(resourceRepo.findByResourceUuid("res-uuid-001")).thenReturn(Optional.of(r));
        when(tagRepo.findTagsByResourceUuid("res-uuid-001")).thenReturn(List.of("dark"));
        when(referenceRepo.countByMetadataUuid("md-uuid-001")).thenReturn(3);

        StorageResourceResponse resp = resourceService.getByUuid("res-uuid-001");

        assertEquals("res-uuid-001", resp.getResourceUuid());
        assertEquals("avatar.png", resp.getResourceName());
        assertEquals("IMAGE", resp.getResourceType());
        assertEquals("AVATAR", resp.getCategory());
        assertEquals(3, resp.getReferenceCount());
        assertEquals(List.of("dark"), resp.getTags());
    }

    @Test
    void getByUuidThrowsWhenNotFound() {
        when(resourceRepo.findByResourceUuid("not-exist")).thenReturn(Optional.empty());

        assertThrows(StorageResourceService.ResourceNotFoundException.class,
                () -> resourceService.getByUuid("not-exist"));
    }

    // ---- 搜索 ----

    @Test
    void searchReturnsPaginatedResults() {
        when(resourceRepo.countSearch(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(10);
        when(resourceRepo.search(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        buildResource("r1", "m1", "a.png", ResourceType.IMAGE, ResourceCategory.AVATAR, ResourceStatus.READY),
                        buildResource("r2", "m2", "b.zip", ResourceType.ARCHIVE, ResourceCategory.PLUGIN, ResourceStatus.REFERENCED)
                ));
        when(tagRepo.findTagsByResourceUuid(anyString())).thenReturn(List.of());
        when(referenceRepo.countByMetadataUuid(anyString())).thenReturn(0);

        var result = resourceService.search("a", "IMAGE", null, "PUBLIC",
                null, null, null, null, "createTime", "desc", 1, 10);

        assertEquals(2, result.getItems().size());
        assertEquals(10, result.getTotal());
        assertEquals(1, result.getTotalPages());
    }

    @Test
    void searchEmptyReturnsEmptyList() {
        when(resourceRepo.countSearch(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);
        when(resourceRepo.search(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        var result = resourceService.search("nonexistent", null, null, null,
                null, null, null, null, "createTime", "desc", 1, 10);

        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
    }

    // ---- 更新 ----

    @Test
    void updateResourceReturnsUpdated() {
        StorageResource r = buildResource("res-uuid-001", "md-uuid-001", "old.png",
                ResourceType.IMAGE, ResourceCategory.AVATAR, ResourceStatus.READY);
        r.setTags(List.of("old-tag"));
        when(resourceRepo.findByResourceUuid("res-uuid-001"))
                .thenReturn(Optional.of(r))
                .thenReturn(Optional.of(buildResource("res-uuid-001", "md-uuid-001", "new.png",
                        ResourceType.IMAGE, ResourceCategory.LOGO, ResourceStatus.READY)));
        when(tagRepo.findTagsByResourceUuid("res-uuid-001")).thenReturn(List.of("new-tag"));
        when(referenceRepo.countByMetadataUuid("md-uuid-001")).thenReturn(2);

        StorageResourceResponse resp = resourceService.update("res-uuid-001",
                "new.png", "updated desc", "LOGO", "LOGIN", null, List.of("new-tag"));

        verify(resourceRepo).update(eq("res-uuid-001"), eq("new.png"), eq("updated desc"),
                eq("LOGO"), eq("LOGIN"), eq("PUBLIC"), eq(List.of("new-tag")));
        verify(tagRepo).replaceTags("res-uuid-001", List.of("new-tag"));
        assertEquals("RES-UUID-001", resp.getResourceUuid().toUpperCase());
    }

    // ---- 状态同步 ----

    @Test
    void updateStatusByMetadataUuidFindsAndUpdates() {
        StorageResource r = buildResource("res-uuid-001", "md-uuid-001", "x.png",
                ResourceType.IMAGE, ResourceCategory.OTHER, ResourceStatus.READY);
        when(resourceRepo.findByMetadataUuid("md-uuid-001")).thenReturn(Optional.of(r));

        resourceService.updateStatus("md-uuid-001", "REFERENCED");

        verify(resourceRepo).updateStatus("res-uuid-001", "REFERENCED");
    }

    @Test
    void updateStatusNotFoundDoesNothing() {
        when(resourceRepo.findByMetadataUuid("no-md")).thenReturn(Optional.empty());

        resourceService.updateStatus("no-md", "DELETED");

        verify(resourceRepo, never()).updateStatus(anyString(), anyString());
    }

    // ---- 软删除 ----

    @Test
    void softDeleteUpdatesStatusAndClearsTags() {
        StorageResource r = buildResource("res-uuid-001", "md-uuid-001", "x.png",
                ResourceType.IMAGE, ResourceCategory.OTHER, ResourceStatus.READY);
        when(resourceRepo.findByResourceUuid("res-uuid-001")).thenReturn(Optional.of(r));

        resourceService.softDelete("res-uuid-001");

        verify(resourceRepo).updateStatus("res-uuid-001", "DELETED");
        verify(tagRepo).deleteByResourceUuid("res-uuid-001");
    }

    @Test
    void softDeleteThrowsWhenNotFound() {
        when(resourceRepo.findByResourceUuid("not-exist")).thenReturn(Optional.empty());

        assertThrows(StorageResourceService.ResourceNotFoundException.class,
                () -> resourceService.softDelete("not-exist"));
    }

    // ---- 扩展属性 ----

    @Test
    void setPropertiesDelegatesToRepo() {
        StorageResource r = buildResource("res-uuid-001", "md-uuid-001", "x.png",
                ResourceType.IMAGE, ResourceCategory.OTHER, ResourceStatus.READY);
        when(resourceRepo.findByResourceUuid("res-uuid-001")).thenReturn(Optional.of(r));

        Map<String, String> props = Map.of("width", "1024", "height", "768");
        resourceService.setProperties("res-uuid-001", props);

        verify(propertyRepo).setProperties("res-uuid-001", props);
    }

    @Test
    void getPropertiesReturnsMap() {
        when(propertyRepo.findByResourceUuid("res-uuid-001"))
                .thenReturn(List.of());

        Map<String, String> result = resourceService.getProperties("res-uuid-001");

        assertTrue(result.isEmpty());
    }

    // ---- helpers ----

    private StorageResource buildResource(String resourceUuid, String metadataUuid,
                                            String name, ResourceType type,
                                            ResourceCategory category, ResourceStatus status) {
        StorageResource r = new StorageResource();
        r.setId(1L);
        r.setResourceUuid(resourceUuid);
        r.setMetadataUuid(metadataUuid);
        r.setResourceName(name);
        r.setResourceType(type);
        r.setCategory(category);
        r.setVisibility(Visibility.PUBLIC);
        r.setStatus(status);
        r.setTags(new ArrayList<>());
        r.setProperties(new ArrayList<>());
        return r;
    }
}