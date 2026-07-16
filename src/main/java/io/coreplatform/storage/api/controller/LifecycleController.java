package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.*;
import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.application.domain.LifecycleTask;
import io.coreplatform.storage.application.domain.ResourceHold;
import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.application.domain.enums.LifecycleStage;
import io.coreplatform.storage.application.service.*;
import io.coreplatform.storage.infrastructure.persistence.repository.LifecycleTaskRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageResourceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage/lifecycle")
@Tag(name = "Lifecycle Runtime", description = "资源生命周期治理（策略管理、阶段转换、归档、Hold）")
public class LifecycleController {

    private final LifecyclePolicyService policyService;
    private final LifecycleEngine engine;
    private final ResourceHoldService holdService;
    private final LifecycleTaskRepository taskRepo;
    private final StorageResourceRepository resourceRepo;

    public LifecycleController(LifecyclePolicyService policyService,
                                LifecycleEngine engine,
                                ResourceHoldService holdService,
                                LifecycleTaskRepository taskRepo,
                                StorageResourceRepository resourceRepo) {
        this.policyService = policyService;
        this.engine = engine;
        this.holdService = holdService;
        this.taskRepo = taskRepo;
        this.resourceRepo = resourceRepo;
    }

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    @Operation(summary = "生命周期仪表盘统计")
    public LifecycleDashboardResponse getDashboard() {
        Map<String, Long> stageCounts = new LinkedHashMap<>();
        for (LifecycleStage stage : LifecycleStage.values()) {
            stageCounts.put(stage.name(), (long) resourceRepo.countByLifecycleStage(stage.name()));
        }

        LifecycleDashboardResponse resp = new LifecycleDashboardResponse();
        resp.setStageCounts(stageCounts);
        resp.setActiveHolds(holdService.countActiveHolds());
        resp.setPendingTasks(taskRepo.countSearch("PENDING", null));
        resp.setTotalPolicies(policyService.listPolicies().size());
        return resp;
    }

    // ---- Policies ----

    @GetMapping("/policies")
    @Operation(summary = "查询所有生命周期策略")
    public List<LifecyclePolicyResponse> listPolicies() {
        return policyService.listPolicies().stream()
                .map(this::toPolicyResponse)
                .toList();
    }

    @PostMapping("/policies")
    @Operation(summary = "创建生命周期策略")
    public LifecyclePolicyResponse createPolicy(@RequestBody CreatePolicyRequest req) {
        LifecyclePolicy policy = policyService.createPolicy(
                req.policyName, req.resourceType, req.category,
                req.activeDays != null ? req.activeDays : 0,
                req.warmDays != null ? req.warmDays : 0,
                req.coldDays != null ? req.coldDays : 0,
                req.archiveDays != null ? req.archiveDays : 0,
                req.deleteDays != null ? req.deleteDays : 0,
                req.description);
        return toPolicyResponse(policy);
    }

    @PutMapping("/policies/{id}")
    @Operation(summary = "更新生命周期策略")
    public LifecyclePolicyResponse updatePolicy(@PathVariable Long id,
                                                  @RequestBody UpdatePolicyRequest req) {
        LifecyclePolicy policy = policyService.updatePolicy(id, req.policyName,
                req.activeDays, req.warmDays, req.coldDays, req.archiveDays, req.deleteDays,
                req.enabled, req.description);
        return toPolicyResponse(policy);
    }

    @DeleteMapping("/policies/{id}")
    @Operation(summary = "删除生命周期策略")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Resource Lifecycle ----

    @GetMapping("/resources/{uuid}/state")
    @Operation(summary = "查询资源生命周期状态")
    public StorageResourceResponse getResourceLifecycleState(@PathVariable String uuid) {
        StorageResource r = resourceRepo.findByResourceUuid(uuid)
                .orElseThrow(() -> new StorageResourceService.ResourceNotFoundException(
                        "Resource not found: uuid=" + uuid));
        StorageResourceResponse resp = new StorageResourceResponse();
        resp.setResourceUuid(r.getResourceUuid());
        resp.setResourceName(r.getResourceName());
        resp.setCategory(r.getCategory() != null ? r.getCategory().name() : null);
        resp.setResourceType(r.getResourceType() != null ? r.getResourceType().name() : null);
        resp.setLifecycleStage(r.getLifecycleStage() != null ? r.getLifecycleStage().name() : "ACTIVE");
        resp.setStatus(r.getStatus() != null ? r.getStatus().name() : null);
        resp.setCreateTime(r.getCreateTime());
        resp.setUpdateTime(r.getUpdateTime());
        return resp;
    }

    @PostMapping("/resources/{uuid}/run")
    @Operation(summary = "手动触发生命周期评估")
    public LifecycleTaskResponse triggerLifecycle(@PathVariable String uuid) {
        LifecycleTask task = engine.triggerLifecycle(uuid);
        return toTaskResponse(task);
    }

    @PostMapping("/resources/{uuid}/archive")
    @Operation(summary = "归档资源")
    public LifecycleTaskResponse archiveResource(@PathVariable String uuid) {
        LifecycleTask task = engine.archiveResource(uuid);
        return toTaskResponse(task);
    }

    @PostMapping("/resources/{uuid}/restore")
    @Operation(summary = "从归档恢复资源")
    public LifecycleTaskResponse restoreResource(@PathVariable String uuid) {
        LifecycleTask task = engine.restoreResource(uuid);
        return toTaskResponse(task);
    }

    // ---- Legal Hold ----

    @PostMapping("/resources/{uuid}/hold")
    @Operation(summary = "设置 Legal Hold（法律保留）")
    public ResourceHoldResponse placeHold(@PathVariable String uuid,
                                            @RequestBody PlaceHoldRequest req) {
        ResourceHold hold = holdService.placeHold(uuid, req.holdType, req.reason,
                req.operatorId, req.expireTime);
        return toHoldResponse(hold);
    }

    @DeleteMapping("/resources/{uuid}/hold")
    @Operation(summary = "解除 Legal Hold")
    public ResponseEntity<Void> releaseHold(@PathVariable String uuid,
                                             @RequestParam(defaultValue = "admin") String operatorId) {
        holdService.releaseHold(uuid, operatorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resources/{uuid}/holds")
    @Operation(summary = "查询资源的 Hold 列表")
    public List<ResourceHoldResponse> listHolds(@PathVariable String uuid) {
        return holdService.listHolds(uuid).stream()
                .map(this::toHoldResponse)
                .toList();
    }

    // ---- Tasks ----

    @GetMapping("/resources/{uuid}/tasks")
    @Operation(summary = "查询资源关联的生命周期任务")
    public List<LifecycleTaskResponse> getResourceTasks(@PathVariable String uuid) {
        return taskRepo.findByResourceUuid(uuid).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @GetMapping("/tasks")
    @Operation(summary = "全局任务列表（支持过滤和分页）")
    public SearchResultResponse<LifecycleTaskResponse> searchTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resourceUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        int offset = Math.max(0, page - 1) * size;
        List<LifecycleTaskResponse> items = taskRepo.search(status, resourceUuid, offset, size)
                .stream().map(this::toTaskResponse).toList();
        int total = taskRepo.countSearch(status, resourceUuid);

        return new SearchResultResponse<>(items, page, size, total);
    }

    // ---- helper mappers ----

    private LifecyclePolicyResponse toPolicyResponse(LifecyclePolicy p) {
        LifecyclePolicyResponse r = new LifecyclePolicyResponse();
        r.setId(p.getId());
        r.setPolicyName(p.getPolicyName());
        r.setResourceType(p.getResourceType());
        r.setCategory(p.getCategory());
        r.setActiveDays(p.getActiveDays());
        r.setWarmDays(p.getWarmDays());
        r.setColdDays(p.getColdDays());
        r.setArchiveDays(p.getArchiveDays());
        r.setDeleteDays(p.getDeleteDays());
        r.setEnabled(p.isEnabled());
        r.setDescription(p.getDescription());
        r.setCreateTime(p.getCreateTime());
        r.setUpdateTime(p.getUpdateTime());
        return r;
    }

    private LifecycleTaskResponse toTaskResponse(LifecycleTask t) {
        LifecycleTaskResponse r = new LifecycleTaskResponse();
        r.setId(t.getId());
        r.setResourceUuid(t.getResourceUuid());
        r.setPolicyId(t.getPolicyId());
        r.setAction(t.getAction() != null ? t.getAction().name() : null);
        r.setTargetStage(t.getTargetStage());
        r.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        r.setExecuteTime(t.getExecuteTime());
        r.setFinishTime(t.getFinishTime());
        r.setErrorMessage(t.getErrorMessage());
        r.setRetryCount(t.getRetryCount());
        r.setCreateTime(t.getCreateTime());
        r.setUpdateTime(t.getUpdateTime());
        return r;
    }

    private ResourceHoldResponse toHoldResponse(ResourceHold h) {
        ResourceHoldResponse r = new ResourceHoldResponse();
        r.setId(h.getId());
        r.setResourceUuid(h.getResourceUuid());
        r.setHoldType(h.getHoldType() != null ? h.getHoldType().name() : null);
        r.setReason(h.getReason());
        r.setOperatorId(h.getOperatorId());
        r.setExpireTime(h.getExpireTime());
        r.setReleased(h.isReleased());
        r.setReleasedTime(h.getReleasedTime());
        r.setReleaseOperatorId(h.getReleaseOperatorId());
        r.setCreateTime(h.getCreateTime());
        return r;
    }

    // ---- request DTOs ----

    public record CreatePolicyRequest(
            String policyName,
            String resourceType,
            String category,
            Integer activeDays,
            Integer warmDays,
            Integer coldDays,
            Integer archiveDays,
            Integer deleteDays,
            String description) {
    }

    public record UpdatePolicyRequest(
            String policyName,
            Integer activeDays,
            Integer warmDays,
            Integer coldDays,
            Integer archiveDays,
            Integer deleteDays,
            Boolean enabled,
            String description) {
    }

    public record PlaceHoldRequest(
            String holdType,
            String reason,
            String operatorId,
            LocalDateTime expireTime) {
    }
}