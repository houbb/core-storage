package io.coreplatform.storage;

import io.coreplatform.storage.application.domain.ResourceHold;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.enums.HoldType;
import io.coreplatform.storage.application.service.ResourceHoldService;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.infrastructure.persistence.repository.ResourceHoldRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResourceHoldServiceTest {

    private ResourceHoldService holdService;
    private ResourceHoldRepository holdRepo;
    private StorageResourceRepository resourceRepo;

    @BeforeEach
    void setUp() {
        holdRepo = mock(ResourceHoldRepository.class);
        resourceRepo = mock(StorageResourceRepository.class);
        holdService = new ResourceHoldService(holdRepo, resourceRepo);
    }

    @Test
    void placeHoldSavesAndReturns() {
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(new StorageResource()));
        when(holdRepo.hasActiveHold("res-001")).thenReturn(false);
        when(holdRepo.save(any())).thenAnswer(inv -> {
            ResourceHold h = inv.getArgument(0);
            h.setId(1L);
            return h;
        });

        ResourceHold result = holdService.placeHold("res-001", "LEGAL", "诉讼保全", "admin", null);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("res-001", result.getResourceUuid());
        assertEquals(HoldType.LEGAL, result.getHoldType());
    }

    @Test
    void placeHoldWithActiveHoldThrows() {
        when(resourceRepo.findByResourceUuid("res-001")).thenReturn(Optional.of(new StorageResource()));
        when(holdRepo.hasActiveHold("res-001")).thenReturn(true);

        assertThrows(ResourceHoldService.HoldAlreadyExistsException.class,
                () -> holdService.placeHold("res-001", "LEGAL", "reason", "admin", null));
    }

    @Test
    void releaseHoldReleasesAllActive() {
        ResourceHold hold = ResourceHold.create("res-001", HoldType.LEGAL, "reason", "admin", null);
        hold.setId(1L);
        when(holdRepo.findActiveByResourceUuid("res-001")).thenReturn(List.of(hold));

        holdService.releaseHold("res-001", "admin2");

        verify(holdRepo).release(1L, "admin2");
    }

    @Test
    void releaseHoldNotFoundThrows() {
        when(holdRepo.findActiveByResourceUuid("res-001")).thenReturn(List.of());

        assertThrows(ResourceHoldService.HoldNotFoundException.class,
                () -> holdService.releaseHold("res-001", "admin"));
    }

    @Test
    void hasActiveHoldReturnsTrue() {
        when(holdRepo.hasActiveHold("res-001")).thenReturn(true);
        assertTrue(holdService.hasActiveHold("res-001"));
    }

    @Test
    void hasActiveHoldReturnsFalse() {
        when(holdRepo.hasActiveHold("res-001")).thenReturn(false);
        assertFalse(holdService.hasActiveHold("res-001"));
    }

    @Test
    void countActiveHoldsDelegates() {
        when(holdRepo.countActiveHolds()).thenReturn(5);
        assertEquals(5, holdService.countActiveHolds());
    }
}