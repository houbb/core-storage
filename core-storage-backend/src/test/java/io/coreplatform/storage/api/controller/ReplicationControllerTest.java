package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.application.domain.StorageReplica;
import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.*;
import io.coreplatform.storage.application.service.ReplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReplicationController.class)
class ReplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReplicationService replicationService;

    // ---- 副本管理 ----

    @Test
    void listReplicasReturns200() throws Exception {
        StorageReplica replica = StorageReplica.create("res-001", "default", "local", ReplicaRole.PRIMARY);
        replica.setId(1L);
        replica.setReplicaStatus(ReplicaStatus.READY);

        when(replicationService.listReplicas("res-001")).thenReturn(List.of(replica));

        mockMvc.perform(get("/api/v1/storage/resources/res-001/replicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceUuid").value("res-001"))
                .andExpect(jsonPath("$[0].replicaRole").value("PRIMARY"))
                .andExpect(jsonPath("$[0].replicaStatus").value("READY"));
    }

    @Test
    void addReplicaReturnsCreatedReplica() throws Exception {
        StorageReplica replica = StorageReplica.create("res-001", "database", "database", ReplicaRole.BACKUP);
        replica.setId(1L);

        when(replicationService.addReplica(eq("res-001"), eq("database"), eq(ReplicaRole.BACKUP)))
                .thenReturn(replica);

        mockMvc.perform(post("/api/v1/storage/resources/res-001/replicas")
                        .contentType("application/json")
                        .content("{\"profileName\":\"database\",\"role\":\"BACKUP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileName").value("database"))
                .andExpect(jsonPath("$.driverName").value("database"))
                .andExpect(jsonPath("$.replicaRole").value("BACKUP"));
    }

    @Test
    void removeReplicaReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/storage/resources/res-001/replicas/1"))
                .andExpect(status().isNoContent());
    }

    // ---- 同步 / 迁移 / 恢复 ----

    @Test
    void syncResourceReturnsTaskList() throws Exception {
        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);

        when(replicationService.syncResource("res-001")).thenReturn(List.of(task));

        mockMvc.perform(post("/api/v1/storage/resources/res-001/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskType").value("SYNC"))
                .andExpect(jsonPath("$[0].targetProfile").value("database"));
    }

    @Test
    void migrateResourceReturnsTask() throws Exception {
        SyncTask task = SyncTask.create(SyncTaskType.MIGRATE, "res-001", "default", "database");
        task.setId(1L);

        when(replicationService.migrateResource("res-001", "default", "database")).thenReturn(task);

        mockMvc.perform(post("/api/v1/storage/resources/res-001/migrate")
                        .contentType("application/json")
                        .content("{\"sourceProfile\":\"default\",\"targetProfile\":\"database\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskType").value("MIGRATE"));
    }

    @Test
    void recoverResourceReturnsTask() throws Exception {
        SyncTask task = SyncTask.create(SyncTaskType.RECOVER, "res-001", "database", "default");
        task.setId(1L);

        when(replicationService.recoverResource("res-001")).thenReturn(task);

        mockMvc.perform(post("/api/v1/storage/resources/res-001/recover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskType").value("RECOVER"));
    }

    // ---- 任务管理 ----

    @Test
    void listTasksReturnsFilteredResults() throws Exception {
        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);

        when(replicationService.listTasks(isNull(), isNull(), isNull(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(task));
        when(replicationService.countTasks(isNull(), isNull(), isNull())).thenReturn(1);

        mockMvc.perform(get("/api/v1/storage/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].taskType").value("SYNC"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void getTaskReturnsDetail() throws Exception {
        SyncTask task = SyncTask.create(SyncTaskType.SYNC, "res-001", "default", "database");
        task.setId(1L);

        when(replicationService.getTask(1L)).thenReturn(task);

        mockMvc.perform(get("/api/v1/storage/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taskType").value("SYNC"));
    }

    @Test
    void pauseTaskReturns200() throws Exception {
        mockMvc.perform(post("/api/v1/storage/tasks/1/pause"))
                .andExpect(status().isOk());
    }

    @Test
    void resumeTaskReturns200() throws Exception {
        mockMvc.perform(post("/api/v1/storage/tasks/1/resume"))
                .andExpect(status().isOk());
    }
}