package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageReplicaResponse;
import io.coreplatform.storage.api.response.SyncTaskResponse;
import io.coreplatform.storage.application.domain.StorageReplica;
import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.ReplicaRole;
import io.coreplatform.storage.application.service.ReplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Replication Runtime", description = "副本管理、同步、迁移、恢复")
public class ReplicationController {

    private final ReplicationService replicationService;

    public ReplicationController(ReplicationService replicationService) {
        this.replicationService = replicationService;
    }

    // ---- Replica endpoints ----

    @GetMapping("/resources/{uuid}/replicas")
    @Operation(summary = "查询资源的所有副本")
    public List<StorageReplicaResponse> listReplicas(@PathVariable String uuid) {
        return replicationService.listReplicas(uuid).stream()
                .map(StorageReplicaResponse::from)
                .toList();
    }

    @PostMapping("/resources/{uuid}/replicas")
    @Operation(summary = "为资源添加副本（自动创建同步任务）")
    public StorageReplicaResponse addReplica(@PathVariable String uuid,
                                              @RequestBody AddReplicaRequest request) {
        ReplicaRole role = request.role != null ? ReplicaRole.valueOf(request.role) : ReplicaRole.BACKUP;
        StorageReplica replica = replicationService.addReplica(uuid, request.profileName, role);
        return StorageReplicaResponse.from(replica);
    }

    @DeleteMapping("/resources/{uuid}/replicas/{id}")
    @Operation(summary = "删除副本")
    public ResponseEntity<Void> removeReplica(@PathVariable String uuid, @PathVariable Long id) {
        replicationService.removeReplica(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Sync / Migrate / Recover endpoints ----

    @PostMapping("/resources/{uuid}/sync")
    @Operation(summary = "触发资源同步 — 为所有非 PRIMARY 副本创建同步任务")
    public List<SyncTaskResponse> syncResource(@PathVariable String uuid) {
        return replicationService.syncResource(uuid).stream()
                .map(SyncTaskResponse::from)
                .toList();
    }

    @PostMapping("/resources/{uuid}/migrate")
    @Operation(summary = "触发资源迁移 — 将 PRIMARY 切换到目标 Profile")
    public SyncTaskResponse migrateResource(@PathVariable String uuid,
                                              @RequestBody MigrateRequest request) {
        SyncTask task = replicationService.migrateResource(uuid,
                request.sourceProfile, request.targetProfile);
        return SyncTaskResponse.from(task);
    }

    @PostMapping("/resources/{uuid}/recover")
    @Operation(summary = "触发故障恢复 — 从健康的 SECONDARY 恢复 PRIMARY")
    public SyncTaskResponse recoverResource(@PathVariable String uuid) {
        SyncTask task = replicationService.recoverResource(uuid);
        return SyncTaskResponse.from(task);
    }

    // ---- Task endpoints ----

    @GetMapping("/tasks")
    @Operation(summary = "查询同步任务列表（支持过滤）")
    public SearchResultResponse<SyncTaskResponse> listTasks(
            @RequestParam(required = false) String resourceUuid,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createTime") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<SyncTask> tasks = replicationService.listTasks(resourceUuid, taskType, status, sort, order, page, size);
        int total = replicationService.countTasks(resourceUuid, taskType, status);

        List<SyncTaskResponse> items = tasks.stream()
                .map(SyncTaskResponse::from)
                .toList();

        return new SearchResultResponse<>(items, page, size, total);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "查询任务详情")
    public SyncTaskResponse getTask(@PathVariable Long id) {
        return SyncTaskResponse.from(replicationService.getTask(id));
    }

    @PostMapping("/tasks/{id}/pause")
    @Operation(summary = "暂停任务")
    public ResponseEntity<Void> pauseTask(@PathVariable Long id) {
        replicationService.pauseTask(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{id}/resume")
    @Operation(summary = "恢复任务")
    public ResponseEntity<Void> resumeTask(@PathVariable Long id) {
        replicationService.resumeTask(id);
        return ResponseEntity.ok().build();
    }

    // ---- request DTOs ----

    public record AddReplicaRequest(
            String profileName,
            String role) {
    }

    public record MigrateRequest(
            String sourceProfile,
            String targetProfile) {
    }
}