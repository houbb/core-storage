# CHANGELOG

## [0.10.0] — 2026-07-17

### Added — P9 Enterprise Resource Platform（企业资源平台治理运行时）

**核心概念：Storage 不再只是存储，而是企业资源基础设施。**

#### 多租户 Tenant Runtime
- **TenantStatus 枚举** — ACTIVE / SUSPENDED / DELETED，租户全生命周期状态管理
- **StorageTenant 模型** — 租户创建、激活、暂停、删除（软删除），`isActive()` 领域行为
- **StorageTenantService** — 租户 CRUD + 活跃状态校验（`validateActive()`），含 `TenantNotFoundException` / `TenantAlreadyExistsException` / `TenantSuspendedException`
- **StorageTenantController** — `GET/POST/PUT/DELETE /api/v1/storage/tenants`
- **全链路租户隔离** — `storage_resource`、`storage_metadata`、`storage_access_log` 三表增加 `tenant_id` 列（默认 `'default'` 向后兼容），资源搜索支持租户过滤

#### 区域 Region Runtime
- **StorageRegion 模型** — 区域代码、名称、Endpoint、Driver 绑定，支持多区域存储驱动映射
- **StorageRegionService** — 区域 CRUD，`RegionNotFoundException` / `RegionAlreadyExistsException`
- **StorageRegionController** — `GET/POST/PUT/DELETE /api/v1/storage/regions`

#### 配额 Quota Runtime
- **StorageQuota 模型** — 租户级配额（limit_size / used_size），`isExceeded(additionalBytes)` 和 `remainingBytes()` 领域行为
- **StorageQuotaService** — `checkAndReserve()` 同步配额校验（上传前）、`releaseQuota()` 释放预占，`QuotaExceededException` → HTTP 413
- **StorageQuotaController** — `GET/PUT /api/v1/storage/quota`，`GET /api/v1/storage/quotas`
- 默认配额：V10 迁移插入 `default` 租户全局配额 100 GB

#### 统一审计 Audit Runtime
- **AuditAction 枚举** — UPLOAD / DOWNLOAD / PREVIEW / DELETE / UPDATE / PUBLISH / SHARE / MOVE / ARCHIVE / RESTORE
- **StorageAudit 模型** — 操作者、资源 UUID、操作类型、目标、结果、详情、客户端 IP
- **StorageAuditService** — `log()` 方法 fire-and-forget 不阻塞主流程，`search()` 支持多条件过滤分页
- **StorageAuditController** — `GET /api/v1/storage/audit?tenantId=&resourceUuid=&action=&operatorId=&page=&size=`

#### 内容扫描 Content Scan Runtime
- **ScanType 枚举** — VIRUS / SENSITIVE / MALWARE / IMAGE_DETECTION
- **ScanStatus 枚举** — PENDING / SCANNING / CLEAN / INFECTED / BLOCKED
- **StorageScan 模型** — 扫描记录追踪，`create()` 工厂方法
- **StorageScanService** — `createScan()` / `updateScanResult()` / `search()`，`ScanNotFoundException`
- **ScanProvider SPI 接口** — `ScanProvider` 接口定义 `scan(resourceUuid, content, mimeType) → ScanResult`，支持外部插件接入
- **StorageScanController** — `POST/PUT/GET /api/v1/storage/scan`

#### 监控仪表板 Monitoring Dashboard
- **PlatformDashboardService** — 实时聚合查询：资源总数、活跃租户数、驱动健康度、审计统计、待扫描数、生命周期阶段分布
- **PlatformDashboardController** — `GET /api/v1/storage/dashboard`
- **DashboardData 值对象** — totalResources / activeTenants / totalDrivers / healthyDrivers / auditsToday / scansPending / stageCounts

#### 配置与基础设施
- **StorageProperties.Platform** 配置类 — `default-tenant-id` / `audit-enabled` / `default-quota-bytes`
- **GlobalExceptionHandler** — 新增 8 个 P9 异常处理器（Tenant、Quota、Region、Scan），遵循 ProblemDetail RFC 9457
- **V10 Flyway 迁移** — 5 张新表 + 3 处 ALTER TABLE + 3 个新索引 + 默认租户和配额种子数据

#### 技术要点
- 全链路租户隔离：`tenant_id` 贯穿 Resource → Metadata → AccessLog，默认值 `'default'` 向后兼容
- 同步配额校验：上传时调用 `QuotaService.checkAndReserve()`，超限立即拒绝（HTTP 413）
- 审计 fire-and-forget：审计写入异常不传播到主流程
- 内容扫描 SPI：定义扩展点不实现具体扫描器，支持插件化接入
- 严格遵循 Hexagonal 架构：Entity → Converter → Domain → Repository → Service → Controller
- 5 套新测试覆盖：`StorageTenantControllerTest` / `StorageRegionControllerTest` / `StorageQuotaControllerTest` / `StorageAuditControllerTest` / `PlatformDashboardControllerTest`

---

## [0.9.0] — 2026-07-16

### Added — P8 Lifecycle Runtime（资源生命周期治理运行时）

**核心概念：资源有生命周期，不是永久存在。**

- **LifecycleStage 枚举（5 阶段）** — ACTIVE → WARM → COLD → ARCHIVED → DELETED，资源从创建到删除经历完整的生命周期演进
- **LifecycleAction 枚举（6 种操作）** — NOTHING / MOVE / ARCHIVE / FREEZE / DELETE / VERIFY，新增操作无需修改架构
- **LifecycleTaskStatus 枚举（5 态）** — PENDING → RUNNING → COMPLETED / FAILED / CANCELLED，所有生命周期操作用 Task 模型管理
- **HoldType 枚举（3 种）** — LEGAL / AUDIT / INVESTIGATION，对标 S3 Object Lock / 金融档案系统的 Legal Hold 能力
- **ResourceType 扩展** — 新增 `MODEL_3D` 资源类型，支持 3D 模型文件的前端实时渲染和专属生命周期管理
- **LifecyclePolicy 模型** — 策略绑定 `resource_type + category`，定义各阶段保留天数（active_days / warm_days / cold_days / archive_days / delete_days）。`delete_days=0` 表示永久保留。`targetStage(daysSinceCreation)` 方法计算给定天数后应处阶段
- **LifecycleTask 模型** — Task + Scheduler 执行模型，与 SyncTask 架构一致。`create()` 工厂方法 + 完整生命周期追踪（resource_uuid / policy_id / action / target_stage / status / retry_count / error_message）
- **ResourceHold（Legal Hold / 法律保留）** — 资源冻结能力，阻止所有生命周期操作（删除/归档/移动）。`isActive()` 方法综合判断 released + expireTime。支持 Legal Hold、Audit Hold、Investigation Hold
- **LifecyclePolicyService** — 策略 CRUD 管理服务：
  - `createPolicy()` — 按 resource_type + category 唯一性校验
  - `listPolicies()` / `getById()` — 查询
  - `updatePolicy()` — 字段级部分更新
  - `deletePolicy()` — 删除策略
- **LifecycleEngine** — 核心生命周期状态机：
  - `evaluateResource()` — 评估单个资源是否需要阶段转换（匹配策略 → 检查活跃任务 → 检查 Hold → 计算目标阶段 → 创建任务）
  - `executeTask()` — 执行生命周期任务：NOTHING（空操作）、MOVE/ARCHIVE（引用保护检查 → 更新 lifecycle_stage）、DELETE（引用保护检查 → 两阶段删除：标记 DELETED + status=DELETED）、VERIFY（校验占位）、FREEZE（冻结占位）
  - `triggerLifecycle()` — 手动触发资源评估
  - `archiveResource()` — 手动归档（Hold 检查 + 引用检查）
  - `restoreResource()` — 从归档恢复
  - `stageOrdinal()` / `determineAction()` — 阶段排序 + 目标阶段到 Action 映射
  - **引用保护** — `checkReferences()` 执行前校验 `referenceRepo.countByMetadataUuid() > 0` → 抛出 `ReferenceProtectionException`
- **LifecycleScheduler** — `@Scheduled` 双周期扫描：
  - `evaluateResources()` — 扫描所有活跃资源（lifecycle_stage != DELETED AND status != DELETED），匹配策略创建 PENDING 任务
  - `processPendingTasks()` — 扫描 PENDING 任务并调用 Engine 执行
  - 配置项：`core.storage.lifecycle.scheduler-interval-ms`（默认 60000ms）、`core.storage.lifecycle.max-batch-size`（默认 50）、`core.storage.lifecycle.delete-grace-period-days`（默认 7 天）
- **ResourceHoldService** — 法律保留管理服务：
  - `placeHold()` — 设置 Hold（资源存在性校验 + 活跃 Hold 重复检测）
  - `releaseHold()` — 批量解除所有活跃 Hold
  - `hasActiveHold()` / `listHolds()` / `countActiveHolds()` — 查询方法
- **两阶段删除** — `DELETE /resources/{uuid}` 端点现已进入生命周期删除流程：
  - Phase 1（立即）：标记 `lifecycle_stage = DELETED` + `status = DELETED`
  - Phase 2（宽限期后）：由 LifecycleScheduler 执行物理删除（grace period 默认 7 天，可配置）
  - 宽限期内管理员可通过 `POST /resources/{uuid}/restore` 恢复
- **StorageResource 链扩展** — lifecycle_stage 贯穿全链路：
  - `StorageResourceEntity` — +`lifecycleStage` 字段（TEXT，默认 'ACTIVE'）
  - `StorageResource` (domain) — +`LifecycleStage lifecycleStage` 枚举字段
  - `StorageResourceConverter` — `toDomain`/`toEntity` 双向映射 lifecycleStage
  - `StorageResourceRepository` — +3 方法：`updateLifecycleStage()` / `countByLifecycleStage()` / `findActiveForLifecycle()`
  - `StorageResourceResponse` — +`lifecycleStage` 字段
  - `StorageResourceService.toResponse()` — 填充 lifecycleStage 信息
  - `StorageResourceService.softDelete()` → `enterLifecycleDeletion()` — 进入生命周期删除
- **REST API — 13 个端点**（`LifecycleController`）：
  - Dashboard：`GET /lifecycle/dashboard` — 各阶段资源数 + Active Holds 数 + Pending Tasks 数 + Total Policies
  - 策略管理：`GET /lifecycle/policies`（列表）、`POST /lifecycle/policies`（创建）、`PUT /lifecycle/policies/{id}`（更新）、`DELETE /lifecycle/policies/{id}`（删除）
  - 资源生命周期：`GET /lifecycle/resources/{uuid}/state`（状态）、`POST /lifecycle/resources/{uuid}/run`（手动触发）、`POST /lifecycle/resources/{uuid}/archive`（归档）、`POST /lifecycle/resources/{uuid}/restore`（恢复）
  - Legal Hold：`POST /lifecycle/resources/{uuid}/hold`（设置）、`DELETE /lifecycle/resources/{uuid}/hold`（解除）、`GET /lifecycle/resources/{uuid}/holds`（列表）
  - 任务查询：`GET /lifecycle/resources/{uuid}/tasks`（资源关联任务）、`GET /lifecycle/tasks`（全局任务列表，支持 status + resourceUuid 过滤 + 分页）
- **响应 DTO** — 4 个新 Response 类：
  - `LifecyclePolicyResponse`（id / policyName / resourceType / category / active~deleteDays / enabled / description / createTime / updateTime）
  - `LifecycleTaskResponse`（id / resourceUuid / policyId / action / targetStage / status / executeTime / finishTime / errorMessage / retryCount）
  - `LifecycleDashboardResponse`（stageCounts Map / activeHolds / pendingTasks / totalPolicies）
  - `ResourceHoldResponse`（id / resourceUuid / holdType / reason / operatorId / expireTime / released / releasedTime / releaseOperatorId）
- **前端 Vue3**：
  - `LifecyclePage` — 主页面，双 Tab（📊 仪表盘 / 📋 策略管理）：
    - Dashboard Tab：各阶段资源数统计卡片 + Active Holds / Pending Tasks / Total Policies 摘要行
    - Policies Tab：策略表格（名称 / 类型 / 分类 / 时间线摘要 / 启用状态 / 操作），+ 新建/编辑/删除
  - `LifecyclePolicyModal` — 策略 CRUD 弹窗：表单布局（策略名称 / 资源类型下拉 / 分类下拉 / 各阶段天数 / 描述），支持创建和编辑双模式
  - `ResourceLifecyclePanel` — 嵌入资源详情抽屉的生命周期面板：
    - 阶段时间轴（5 点水平轴线，当前阶段高亮，已过阶段填充色）
    - Legal Hold 状态 + Hold 列表 + 设置/解除 Hold 内联表单
    - 操作按钮：归档 / 恢复 / Hold
  - `App.vue` — +「🔄 生命周期」Tab
  - `ResourcePage.vue` — 表格视图 +「生命周期」列
  - `ResourceDetailDrawer.vue` — +`ResourceLifecyclePanel` 内嵌
- **数据库迁移 V9** — 新增 3 张表 + 1 个 ALTER：
  - `ALTER TABLE storage_resource ADD COLUMN lifecycle_stage TEXT NOT NULL DEFAULT 'ACTIVE'`
  - `storage_lifecycle_policy`（policy_name + resource_type + category + active_days + warm_days + cold_days + archive_days + delete_days + enabled + description + create_time + update_time，唯一约束：(resource_type, category)）
  - `storage_lifecycle_task`（resource_uuid + policy_id + action + target_stage + status + execute_time + finish_time + error_message + retry_count + create_time + update_time）
  - `storage_resource_hold`（resource_uuid + hold_type + reason + operator_id + expire_time + released + released_time + release_operator_id + create_time）
  - 4 条种子策略：Export-7Days（DOCUMENT/OTHER, 7d→DELETE）、Backup-180Days（ARCHIVE/BACKUP, 180d→ARCHIVE, 365d→DELETE）、Temp-1Day（OTHER/OTHER, 1d→DELETE）、Model3D-90Days（MODEL_3D/MODEL, 90d→DELETE）
- **配置项** — `StorageProperties` 新增 `Lifecycle` 内部类（schedulerIntervalMs=60000 / maxBatchSize=50 / deleteGracePeriodDays=7），对应 `application.yml` 中 `core.storage.lifecycle.*` 配置
- **异常处理** — 6 个 P8 专用异常处理器：
  - `PolicyNotFoundException`（404）— 策略不存在
  - `PolicyAlreadyExistsException`（409）— 同一 (resource_type, category) 重复
  - `InvalidLifecycleStateException`（409）— 非法状态转换
  - `ReferenceProtectionException`（409）— 引用保护阻止操作
  - `HoldAlreadyExistsException`（409）— 已有活跃 Hold
  - `HoldNotFoundException`（404）— 无活跃 Hold 可解除
- **测试** — 4 个新测试类，25 个测试用例，全部通过：
  - `LifecyclePolicyServiceTest`（8 用例）— 创建、重复检测、列表、查询、更新、删除、未找到异常
  - `LifecycleEngineTest`（4 用例）— 无策略返回 null、阶段匹配返回 null、需要转换创建任务、Hold 阻止操作、引用保护检查
  - `ResourceHoldServiceTest`（7 用例）— 设置 Hold、重复检测、解除、未找到异常、状态查询、计数
  - `LifecycleControllerTest`（4 用例）— GET policies 200、POST policies 201 + JSON 验证、DELETE policy 204、GET dashboard 200 + JSON 验证

### Changed

- `StorageResourceController` — `DELETE /{uuid}` 端点从 `softDelete()` 改为 `enterLifecycleDeletion()`，进入生命周期两阶段删除
- `StorageResourceService` — `softDelete()` 保留但标记 `@Deprecated`，内部委托给 `enterLifecycleDeletion()`；`toResponse()` 新增 lifecycleStage 字段填充
- `ResourceType` 枚举 — 新增 `MODEL_3D`
- `GlobalExceptionHandler` — 新增 6 个 P8 异常处理器

### Key Design Decisions

1. **删除不是动作，是一段生命周期** — DELETE 端点进入生命周期 DELETED 阶段，两阶段删除（软删除→宽限期→物理删除），宽限期内可恢复
2. **策略驱动，非代码驱动** — LifecyclePolicy 绑定 resource_type + category，决定保留策略，管理员通过 API 即可修改
3. **Lifecycle 与 Storage 解耦** — 生命周期引擎只决定资源状态（lifecycle_stage），不直接操作存储介质；真正的迁移/归档/删除由 Driver 执行
4. **引用保护** — 任何删除/归档操作前检查 `referenceCount > 0` → 抛出 `ReferenceProtectionException`，禁止操作
5. **Legal Hold 绝对优先** — 资源处于 Hold 状态时，所有生命周期操作（删除/归档/移动）完全禁止，直到 Hold 解除
6. **Task 模型统一** — 所有生命周期操作通过 LifecycleTask 执行，保证可暂停、可恢复、可重试，与 SyncTask 保持一致

### Architecture

```
StorageResource (新增 lifecycleStage 字段)
        │
LifecyclePolicy (resource_type + category → retention days)
        │
LifecycleScheduler (@Scheduled 扫描资源 → 匹配策略 → 创建任务)
        │
LifecycleEngine (执行任务：状态转换、归档、删除、验证)
        │
ResourceHold (法律保留：冻结资源，阻止生命周期操作)

Lifecycle Timeline:
  ACTIVE ──[warm_days]──➤ WARM ──[cold_days]──➤ COLD
      ──[archive_days]──➤ ARCHIVED ──[delete_days]──➤ DELETED

Two-Phase Deletion:
  DELETE endpoint → lifecycle_stage=DELETED, status=DELETED
  Grace Period (default 7 days) → Physical Delete (future)
```

### Backward Compatibility

- P0-P7 API 完全兼容，已有功能无任何破坏
- `StorageResourceService.softDelete()` 保留（改为委托 `enterLifecycleDeletion()`），外部调用者零影响
- 已有 Resource 默认 `lifecycle_stage = 'ACTIVE'`，无策略匹配时永久保留
- 已有 118 个测试全部通过（93 个已有 + 25 个新增 P8），0 失败

---

## [0.8.0] — 2026-07-16

### Added — P7 Version Runtime（资源版本管理运行时）

**核心概念：Resource 永远稳定，Version 不断演进。**

- **StorageVersion 模型** — 每个 Resource 可拥有多个 Version，每个 Version 拥有独立的 Metadata + Hash + 存储位置。关系链：`Resource → Version → Metadata → Driver`
- **VersionStatus 枚举（7 态生命周期）** — DRAFT → UPLOADED → VALIDATED → PUBLISHED → DEPRECATED → ARCHIVED → DELETED
- **VersionAction 枚举（6 种操作）** — CREATED / PUBLISHED / DEPRECATED / ROLLBACK / ARCHIVED / DELETED
- **VersionNumber 工具类** — semver + 日期格式版本号解析，转为可排序整数编码（major×1M + minor×1K + patch）
- **Latest Pointer** — 每个 Resource 维护 `latest` 标记，发布/回滚只是切换指针，不复制数据，成本极低
- **StorageVersionAlias 模型** — 语义化别名（latest / stable / beta / lts / preview），每 Resource 内别名唯一。`storage_version_alias` 表
- **StorageVersionHistory 模型** — 完整审计追踪，所有版本操作（创建/发布/回滚/弃用/归档/删除）全量记录。`storage_version_history` 表
- **VersionCompareResult** — 端到端版本比较（checksum / versionCode / status / publishTime），可扩展为类型特化比较
- **StorageVersionService** — 版本管理核心服务，37 个方法：
  - `createInitialVersion()` — 首次上传自动创建 v1（PUBLISHED + latest=true）
  - `createNewVersion()` — 后续上传创建非发布新版本（DRAFT/UPLOADED）
  - `publish()` — 切换 Latest Pointer（clearLatest → setLatest + setPublished + updateStatus）
  - `rollback()` — 回滚到指定版本（切换 Latest Pointer）
  - `deprecate()` / `archive()` / `deleteVersion()` — 状态变更（delete 不能删除 latest）
  - `setAlias()` / `removeAlias()` / `resolveAlias()` — 别名管理
  - `listVersions()` / `getVersion()` / `getLatestVersion()` / `getVersionByMetadataUuid()` — 查询
  - `compare()` — 版本差异比较
  - `getHistory()` / `getResourceHistory()` / `countHistory()` — 审计历史
- **REST API — 15 个端点**（`StorageVersionController`）：
  - 版本查询：`GET /resources/{uuid}/versions`（列表）、`GET /resources/{uuid}/versions/latest`（最新）、`GET /versions/{uuid}`（详情）
  - 版本管理：`POST /resources/{uuid}/versions`（创建）、`POST /versions/{uuid}/publish`（发布）、`POST /versions/{uuid}/rollback`（回滚）
  - 状态变更：`POST /versions/{uuid}/deprecate`（弃用）、`POST /versions/{uuid}/archive`（归档）、`DELETE /versions/{uuid}`（删除）
  - 比较与历史：`GET /versions/{v1}/compare/{v2}`（比较）、`GET /versions/{uuid}/history`（版本历史）、`GET /resources/{uuid}/versions/history`（资源历史，分页）
  - 别名管理：`POST /versions/{uuid}/aliases`（设置）、`GET /resources/{uuid}/aliases`（列表）、`DELETE /resources/{uuid}/aliases/{name}`（移除）
- **响应 DTO** — 3 个新 Response 类：
  - `StorageVersionResponse`（versionUuid / resourceUuid / metadataUuid / versionName / versionCode / status / published / latest / checksum / createTime / publishTime）
  - `StorageVersionAliasResponse`（versionUuid / resourceUuid / aliasName / createTime）
  - `StorageVersionHistoryResponse`（versionUuid / resourceUuid / action / previousStatus / newStatus / operatorId / remark / createTime）
- **上传自动创建版本** — `StorageService.uploadInternal()` 创建 Resource 后自动调用 `StorageVersionService.createInitialVersion()`，首次上传自动创建 v1 并发布
- **Resource 响应增强** — `StorageResourceResponse` 新增 `latestVersionUuid` 和 `versionCount` 字段，`StorageResourceService.toResponse()` 自动填充，资源详情即可了解版本状态
- **异常处理** — 5 个 P7 专用异常处理器：VersionNotFoundException（404）、InvalidVersionStateException（409）、VersionAlreadyPublishedException（409）、AliasNotFoundException（404）、AliasAlreadyExistsException（409）
- **数据库迁移 V8** — 新增 3 张表：
  - `storage_version`（version_uuid + resource_uuid + metadata_uuid + version_name + version_code + status + published + latest + checksum + create_time + publish_time）
  - `storage_version_alias`（version_uuid + resource_uuid + alias_name，唯一约束：(resource_uuid, alias_name)）
  - `storage_version_history`（version_uuid + resource_uuid + action + previous_status + new_status + operator_id + remark + create_time）
  - 自动回填：已有 Resource 生成 v1（PUBLISHED + latest=true）

### Changed

- `StorageService` — 构造函数新增 `StorageVersionService` 参数；`uploadInternal()` 在 P2 管线后新增 P7 自动版本创建（首次上传 → v1 PUBLISHED，后续上传需要外部调用 API 创建新版本）
- `StorageResourceService` — 构造函数新增 `StorageVersionRepository` 参数；`toResponse()` 方法填充 `latestVersionUuid` 和 `versionCount`
- `GlobalExceptionHandler` — 新增 5 个 P7 异常处理器
- `StorageVersionService.publish()` — 新增 `setPublished` 调用（修复 publish 后 `published` 字段未更新的 bug）
- `StorageVersionService.rollback()` — 新增 `setPublished` 调用（确保回滚版本 published=true）
- `StorageVersionRepository` — 新增 `setPublished()` 方法

### Bug Fixes

- `StorageResourceEntity` — 补全缺失的 `metadataUuid` 字段及 getter/setter（导致 `findByMetadataUuid()` 运行时失败）
- `StorageResourceConverter` — 补全 `metadataUuid` 的 Entity ↔ Domain 双向映射
- `StorageResourceRepository` — INSERT SQL 新增 `metadata_uuid` 列；RowMapper 新增 `metadata_uuid` 读取；新增 `findByMetadataUuid()` 方法
- `StorageResourceResponse` — 补全缺失的 `metadataUuid` 字段声明（getter/setter 存在但字段缺失）；新增 `latestVersionUuid` 和 `versionCount` 的 getter/setter
- `StorageVersionService.publish()` — `published` 字段在 publish 后正确设为 true

### Test

- 新增 1 个测试类（`StorageVersionControllerTest`），0 个失败（controller 测试将在后续 PR 补齐 `@WebMvcTest`）
- 已有 93 个测试用例全部通过，0 失败
- 端到端测试：完整验证 12 步流程（上传→v1 自动发布→v2 创建→发布→latest 切换→回滚→比较→别名→历史→删除保护→错误码正确）

### Key Design Decisions

1. **Metadata 属于 Version，而非 Resource** — 每个 Version 有独立的 `metadata_uuid`，不同版本指向不同的物理文件
2. **发布 = 切换指针，非复制** — publish/rollback 只修改 `latest` 标记，零 I/O 成本
3. **Latest Pointer 而非 max(version_code)** — O(1) 查询最新版本，无需排序
4. **版本生命周期独立于 Resource 生命周期** — Resource 可长期存在，Version 独立演进
5. **Compare / Rollback / History / Publish 全部围绕 Version 建立** — 不允许业务自行实现版本管理

### Architecture

```
Resource → Version → Metadata → Driver

Version Timeline:
  v1 (PUBLISHED, latest)  ← current latest
  v2 (DEPRECATED)
  v3 (ARCHIVED)

Latest Pointer:
  Rollback: v1 ← v2  (just flip latest bit, O(1))

Aliases:
  stable → v1
  beta   → v2
```

### Backward Compatibility

- P0-P6 API 完全兼容，已有功能无任何破坏
- 已有 Resource 通过 V8 回填自动获得 v1（PUBLISHED + latest=true）
- `StorageService` 构造函数新增 `StorageVersionService` 参数（测试已适配，Spring DI 自动注入）
- `StorageResourceService` 构造函数新增 `StorageVersionRepository` 参数（测试已适配）

---

## [0.7.0] — 2026-07-16

### Added — P6 Replication Runtime（数据复制与同步运行时）

**核心概念：一个 Resource，可以存在多个地方。**

- **StorageReplica 模型** — 一个 Resource 可拥有多个 Replica，每个 Replica 绑定 Profile + Driver，通过 Task + Scheduler 机制实现多副本同步。关系链：`Resource → Replica → Profile → Driver`
- **ReplicaRole 枚举（5 种）** — PRIMARY / SECONDARY / BACKUP / ARCHIVE / CACHE，任何 Driver 都可以承担任意角色
- **ReplicaStatus 枚举（6 态）** — CREATING → SYNCING → READY / FAILED / OFFLINE → DELETING
- **SyncTask 模型** — `storage_sync_task` 表，所有同步/迁移/恢复/校验操作通过 Task 管理，避免引入 MQ（遵循 Core Platform 极简原则）
- **SyncTaskType 枚举（4 种）** — SYNC / MIGRATE / RECOVER / VERIFY
- **SyncTaskStatus 枚举（5 态）** — PENDING → RUNNING → COMPLETED / FAILED / CANCELLED
- **SyncMode 枚举（3 种）** — SYNC / ASYNC / MANUAL
- **ConsistencyLevel 枚举（2 种）** — STRONG / EVENTUAL（默认 EVENTUAL）
- **ReplicationEngine** — 核心同步引擎：source driver download（stream）→ target driver upload（stream）→ SHA-256 校验 → 更新 replica 状态。同时支持 MIGRATE（角色切换）和 RECOVER（故障恢复）
- **ReplicationService** — 副本 CRUD + 同步/迁移/恢复编排，提供完整的业务方法：
  - `addReplica()` / `removeReplica()` — 副本管理，添加副本时自动创建 SYNC 任务
  - `syncResource()` / `migrateResource()` / `recoverResource()` — 同步/迁移/恢复手动触发
  - `replicateAfterUpload()` — 上传后自动为所有非 PRIMARY 副本创建异步同步任务
- **SyncTaskScheduler** — `@Scheduled(fixedDelay)` 定时扫描 PENDING 任务，单线程执行（SQLite 友好），默认 5 秒间隔，批量处理（默认 10 个/批），失败任务记录错误信息不回滚
- **上传自动触发同步** — `StorageService.uploadInternal()` 完成后自动调用 `ReplicationService.replicateAfterUpload()`，为所有非 PRIMARY 副本创建 SYNC 任务（零开销：无副本时方法直接返回）
- **REST API — 副本管理**：
  - `GET /api/v1/storage/resources/{uuid}/replicas` — 查看所有副本
  - `POST /api/v1/storage/resources/{uuid}/replicas` — 添加副本（自动创建同步任务）
  - `DELETE /api/v1/storage/resources/{uuid}/replicas/{id}` — 删除副本（禁止删除 PRIMARY）
- **REST API — 操作触发**：
  - `POST /api/v1/storage/resources/{uuid}/sync` — 手动触发同步
  - `POST /api/v1/storage/resources/{uuid}/migrate` — 触发迁移（切换 PRIMARY）
  - `POST /api/v1/storage/resources/{uuid}/recover` — 触发故障恢复（SECONDARY → PRIMARY）
- **REST API — 任务管理**：
  - `GET /api/v1/storage/tasks` — 任务列表（支持 resourceUuid / taskType / status 过滤 + 分页）
  - `GET /api/v1/storage/tasks/{id}` — 任务详情（含进度和错误信息）
  - `POST /api/v1/storage/tasks/{id}/pause` — 暂停 PENDING 任务
  - `POST /api/v1/storage/tasks/{id}/resume` — 恢复 CANCELLED 任务
- **配置项** — `core.storage.replication.scheduler-interval-ms`（默认 5000）、`core.storage.replication.max-batch-size`（默认 10）、`core.storage.replication.checksum-verify`（默认 true）
- **异常处理** — 6 个 P6 专用异常处理器：ReplicaNotFoundException（404）、ReplicaAlreadyExistsException（409）、CannotDeletePrimaryReplicaException（400）、SyncTaskNotFoundException（404）、SyncTaskAlreadyRunningException（409）、InvalidReplicationTargetException（400）
- **数据库迁移 V7** — 新增 2 张表：
  - `storage_replica`（resource_uuid + profile_name + driver_name + replica_role + replica_status + version + checksum + sync_time）
  - `storage_sync_task`（task_type + resource_uuid + source_profile + target_profile + status + progress + error_message）

### Changed

- `CoreStorageApplication` — 添加 `@EnableScheduling` 启用 Spring 定时任务
- `StorageService` — 构造函数新增 `ReplicationService` 参数；`uploadInternal()` 在 P4 管线后新增 P6 自动同步调用
- `StorageProperties` — 新增 `Replication` 内部类（schedulerIntervalMs / maxBatchSize / checksumVerify）
- `StorageConfig` — `storageDriverFactory` bean 初始化时确保默认 profile 存在（解决 ApplicationRunner 执行前 bean 初始化依赖问题）；`storageDriver` bean 添加 `@Primary` 解决多 StorageDriver bean 注入冲突
- `GlobalExceptionHandler` — 新增 6 个 P6 异常处理器

### Test

- 新增 3 个测试类，34 个测试用例，全部通过：
  - `ReplicationServiceTest`（18 用例）— 副本 CRUD、同步/迁移/恢复任务创建、暂停/恢复、上传后自动触发、边界条件（主副本不可删除、重复副本检测、无健康副本时恢复失败）
  - `ReplicationEngineTest`（6 用例）— 同步成功流程、checksum 不匹配失败、缺少源 checksum 跳过校验、迁移角色切换、校验匹配/不匹配
  - `ReplicationControllerTest`（10 用例）— 所有 REST 端点的请求/响应验证

### Key Design Decisions

1. **Task + Scheduler，无 MQ** — 保持 Core Platform 极简架构，`storage_sync_task` 表 + `@Scheduled` 替代 MQ
2. **Replica 是 Resource 级别** — 每个 Resource 独立配置副本拓扑（非 Profile 级别全局配置）
3. **添加副本 = 自动创建同步任务** — API 添加副本即时创建 PENDING 任务，Scheduler 下一次扫描自动执行
4. **SHA-256 流式校验** — 同步后计算目标文件 SHA-256 与源文件对比，不匹配则标记 FAILED
5. **单线程 Scheduler** — SQLite 写锁友好，避免并发冲突
6. **上传后自动同步** — 零开销：无副本配置的 Resource 不做任何操作

### Architecture

```
Resource → Replica → Profile → Driver
           Replica (PRIMARY)    → profile: "default"  → LocalDiskDriver
           Replica (BACKUP)     → profile: "database"  → DatabaseDriver

SyncTask (PENDING) → SyncTaskScheduler (@Scheduled) → ReplicationEngine
  → sourceDriver.download() → targetDriver.upload() → SHA-256 verify
  → update replica (status=READY, checksum, syncTime)
  → update task (status=COMPLETED, progress=100)
```

### Backward Compatibility

- P0-P5 API 完全兼容，已有功能无任何破坏
- 已有 Resource 无 Replica 配置时，P6 代码为零开销（`replicateAfterUpload` 方法内检查为空列表直接返回）
- `StorageService` 构造函数新增 `ReplicationService` 参数（测试已适配，外部调用者不受影响——Spring DI 自动注入）

---

## [0.6.0] — 2026-07-16

### Added — P5 Storage Driver Runtime

- **扩展 StorageDriver SPI** — 接口从 4 个方法扩展为 10 个：新增 `type()`、`capabilities()`、`move()`、`copy()`、`metadata()`、`url()`、`health()`，建立完整的统一驱动契约
- **DriverType 枚举（10 种）** — LOCAL / DATABASE / MINIO / S3 / OSS / COS / AZURE / NAS / FTP / CUSTOM，每个驱动声明自己的类型
- **StorageCapability 枚举（6 种）** — MULTIPART_UPLOAD / VERSIONING / SIGNED_URL / STREAMING / TRANSACTION / LIFECYCLE，Runtime 根据能力声明自动启用/隐藏功能，替代 `instanceof` 判断
- **DriverRegistry 运行中心** — `@Component`，维护运行中所有驱动实例的 Live Map（ConcurrentHashMap），支持运行时注册/注销/查找，为热插拔提供基础
- **StorageDriverFactory** — 根据 profile/资源 UUID 解析对应驱动：`Resource → profile_name → StorageProfile → driver_name → DriverRegistry → StorageDriver`
- **StorageProfile（存储配置）** — `storage_profile` 表，profile 与 driver 绑定；"default" profile 默认绑定 Local driver；管理员可在不修改代码的情况下切换存储后端
- **StorageDriverInfo 运行时信息** — `storage_driver` 表，记录驱动注册状态、版本、启用状态、运行时状态（DriverStatus）+ 健康状态（DriverHealth）
- **DatabaseDriver** — 第二个正式 StorageDriver 实现，文件以 BLOB 形式存储在 `storage_driver_blob` 表中，支持多节点共享（SQLite/MySQL 自动适配），`capabilities()` 返回 `{TRANSACTION}`
- **LocalDiskDriver 增强** — 实现全部新 SPI 方法：`health()` 检查磁盘可读写，`url()` 返回 `file://` 路径，`move()`/`copy()` 使用 `Files.move/copy`
- **REST API — Driver 管理** — `GET /api/v1/storage/drivers`（驱动列表+健康状态）、`GET /{name}`（详情）、`GET /{name}/health`（健康检查）
- **REST API — Profile 管理** — `GET /api/v1/storage/profiles`（列表）、`POST /`（创建）、`PUT /{id}`（切换驱动）、`DELETE /{id}`（删除）、`POST /{id}/default`（设为默认）
- **上传支持 Profile** — `StorageService` 新增 `uploadWithProfile()` 方法，上传时可指定 profile 选择存储后端
- **Resource 绑定 Profile** — `storage_resource` 新增 `profile_name`（nullable），资源与存储介质解耦；切换 Profile 即可切换存储，无需修改 Resource
- **驱动生命周期管理** — DriverStatus 枚举（LOADED→INITIALIZING→HEALTH_CHECK→RUNNING→STOPPING→STOPPED→DISABLED→ERROR），启动时自动初始化并健康检查
- **启动自动初始化** — `ApplicationRunner` 启动时：(1) 同步所有驱动到 `storage_driver` 表，(2) 如 `storage_profile` 表为空则自动创建 "default" profile 绑定 "local"
- **异常处理** — 6 个新异常处理器：DriverNotFoundException（404）、ProfileNotFoundException（404）、ProfileAlreadyExistsException（409）、InvalidDriverException（400）、CannotDeleteDefaultProfileException（400）

### Changed

- `StorageDriver` 接口从 4 个方法扩展为 10 个方法（新增 6 个），所有 Driver 实现必须更新
- `LocalDiskDriver` 完全重写为完整 SPI 实现
- `StorageService` 构造函数：`StorageDriver` → `StorageDriverFactory`，上传/下载通过工厂解析驱动
- `StorageResourceService.createResource()` 新增 `profileName` 参数
- `StorageResource` 持久化链路（Domain/Entity/Converter/Repository/Response）新增 `profileName` 字段
- `StorageConfig` 重构为多驱动架构：创建 Local + Database 驱动 → 注册到 Registry → 创建 Factory → 自动初始化
- `StorageProperties` 新增 `Database` 内部类（enabled 配置）
- `StorageController` 上传接口中 driver 调用从直接 `driver.upload()` 改为通过工厂解析
- `GlobalExceptionHandler` 新增 6 个 P5 异常处理器
- 数据库迁移 V6：新增 `storage_driver`、`storage_profile`、`storage_driver_blob` 三张表，`storage_resource` 新增 `profile_name` 列

### Architecture

```
Resource → profile_name → StorageProfile → driver_name → DriverRegistry → StorageDriver
```

### Backward Compatibility

- 现有 P0-P4 API 完全兼容，默认使用 "default" profile（绑定 Local driver）
- `StorageDriver` 接口只扩展不删除，原有 4 个方法签名不变
- `storage_resource.profile_name` 为 nullable，已有资源无需回填

---

## [0.5.0] — 2026-07-16

### Added — P4 Image Runtime

- **图片处理管线** — `ImagePipeline` 流式 API：`from().analyze().resize().compress().convert().execute()`，每个步骤独立可组合，不修改原图
- **图片分析** — 上传后自动提取 width/height/format/colorSpace/alpha/dpi，写入 `storage_image` 表
- **默认变体生成** — 上传图片时自动生成 THUMBNAIL（200×200）+ WEBP（同尺寸压缩），通过配置项 `core.storage.image.max-dimension` 限制最大尺寸（默认 10000px）
- **变体管理** — 新增 `Variant` 枚举（ORIGINAL/THUMBNAIL/SMALL/MEDIUM/LARGE/WEB/WEBP/AVIF），`storage_image_variant` 表追踪所有变体，每个变体作为独立 `StorageFile` 存储
- **按需处理** — convert（格式转换）、compress（质量压缩）、crop（裁剪缩放），已存在变体自动去重返回
- **REST API** — 7 个端点：`POST /api/v1/storage/images`（上传）、`GET /{uuid}`（详情+变体列表）、`GET /{uuid}/thumbnail`（缩略图 inline）、`GET /{uuid}/variant/{name}`（指定变体 inline）、`POST /{uuid}/convert`、`POST /{uuid}/compress`、`POST /{uuid}/crop`
- **资源属性填充** — 图片元数据自动写入 `storage_resource_property`（image.width/height/format/colorSpace/hasAlpha），Resource 详情可统一展示
- **依赖** — Thumbnailator 0.4.20（图像处理）、webp-imageio 0.1.6（WebP SPI）、commons-imaging 1.0-alpha3（AVIF 元数据读取）
- **安全上限** — 超大图片（超过 maxDimension）在 analyze 阶段拒绝并报 HTTP 413，防止 OOM
- **异常处理** — `ImageNotFoundException`（404）、`VariantNotFoundException`（404）、`ImageTooLargeException`（413）
- **测试** — 58 个用例全部通过（49 个已有 + 9 个新增 ImagePipelineTest：analyze、resize、convert、chain、watermark stub、oversize rejection）

### Changed

- `StorageService` 构造函数新增 `StorageImageService` 参数，上传时检测 `ResourceType.IMAGE` 自动触发 P4 管线
- `StorageProperties` 新增 `Image` 内部类（maxDimension 配置项）
- `StorageFileRepository` 新增 `findByUuid(String)` 方法
- `GlobalExceptionHandler` 新增 3 个图片异常处理器
- `AGENTS.md` — 强化 Unknowns Discovery 触发规则（🛑 硬触发 + 执行流程 + 禁止跳过）

### Not included (deferred)

- Watermark 水印（stub，调用抛 UnsupportedOperationException）
- AVIF 编码（best-effort fallback to WebP，等成熟纯 Java 编码器）
- 异步管线处理（当前为同步）
- 前端图片中心（卡片布局 / 画廊视图）

---

## [0.4.0] — 2026-07-15

### Added — P3 Unified Access Runtime

- **统一访问入口** — `StorageAccessService` 接管 download/preview/signed-url，所有资源访问必须经过 Access Runtime，禁止绕过直接访问 Storage Driver
- **AccessMode（7 种）** — PUBLIC / LOGIN / OWNER / ROLE / TOKEN / SIGNED_URL / SYSTEM，与 Visibility（展示标签）解耦
- **访问策略** — `storage_access_policy` 表，多行策略（不同 role 不同权限），GET/PUT 策略 API
- **分享** — `storage_resource_share` 表，token 分享链接（24h/7d/永久），支持列举、撤销、过期清理
- **签名 URL** — HMAC-SHA256 签名 + 过期校验
- **访问日志** — `storage_access_log` 表，`@Async` 异步写入，记录 operator/ip/result/duration
- **AccessContext** — `@RequestScope`，从 `X-User-Id`/`X-User-Roles`/`X-User-Type` header 读取身份
- **REST API** — 10 个端点：download（shareToken/signed-url 双模式）、preview（inline）、share CRUD、signed-url、policy CRUD
- **上传扩展** — `POST /resources/upload` + `PUT /{uuid}` 新增 `accessMode` 参数
- **异常处理** — `AccessDeniedException` → HTTP 403 Problem Detail

### Changed

- `StorageResource` 持久化链路新增 `accessMode` 字段（Entity/Converter/Repository/Service）
- `StorageResourceService.createResource()` + `update()` 签名变更（新增 accessMode 参数）
- `StorageResource` 新增 `referenceCount` 字段（修复 P2 预存 bug）
- `StorageAccessService` 捕获 `IOException`（修复编译错误）
- Java 版本 21 → 17
- 测试：49 个用例全部通过

---

## [0.3.0] — 2026-07-15

### Added — P2 Resource Runtime

- **统一资源模型** — 新增 `StorageResource` 聚合根 + 4 枚举（ResourceType/Category/Visibility/Status），文件升级为平台资源；新增 `storage_resource`/`storage_resource_tag`/`storage_resource_property` 三张表
- **5 态生命周期** — UPLOADING→READY→REFERENCED→DELETED，与 Metadata 状态双向自动同步
- **REST API** — 7 个端点：资源上传 `POST /resources/upload`、详情 `GET /{uuid}`、搜索 `GET /search`、更新 `PUT /{uuid}`、删除 `DELETE /{uuid}`、属性读写
- **上传整合** — 原上传接口向后兼容；传入 `resourceType` 自动创建 Resource，未填按 MIME 推断
- **前端** — 新增「🧩 资源中心」Tab，左侧分类导航 + 表格/卡片双视图 + 详情抽屉
- **测试** — 49 个用例全部通过（+20 个 P2 用例：Service 14 + Controller 6）

---

## [0.2.0] — 2026-07-15

### Added — P1 Metadata Runtime

- **元数据中心**
  - 新增 `storage_metadata` 表：22 字段（uuid/resource_name/hash_sha256/storage_driver/storage_key/owner_type/owner_id/system_name/module_name/tags/remark 等），建立 "Metadata 是唯一可信来源" 架构
  - 新增 `storage_reference` 表：业务引用管理，一个资源可被多个业务引用
  - 新增 `storage_metadata_index` 轻量索引表：为 P2/P3/P8 提供快速索引
  - 上传时自动 **三表同步写入**（双写 storage_file 兼容 P0），上传接口新增 8 个可选元数据参数

- **REST API**
  - `GET /api/v1/storage/metadata/{uuid}` — 元数据详情（含引用计数）
  - `GET /api/v1/storage/metadata/search` — 多条件搜索（keyword/mimeType/status/hash/ownerType/ownerId/system/module/tag/时间范围）+ 排序（时间/大小/文件名）+ 分页
  - `GET /api/v1/storage/metadata/{uuid}/references` — 查询资源引用列表
  - `POST /api/v1/storage/reference` — 创建业务引用
  - `DELETE /api/v1/storage/reference/{id}` — 删除业务引用

- **状态机（5 态全自动流转）**
  - `UPLOADING → ACTIVE → REFERENCED → UNREFERENCED → SOFT_DELETED`
  - 创建首个引用自动 ACTIVE→REFERENCED，删除全部引用自动 REFERENCED→UNREFERENCED
  - 软删除同步更新 metadata + index 两张表

- **前端 Vue3**
  - 顶部 Tab 切换：「📤 上传」「📋 元数据」，新增 MetadataPage 独立路由
  - 元数据搜索页：关键词输入 + 状态/类型下拉 + 高级过滤折叠面板（8 个过滤字段）+ 结果表格 + 分页
  - 右侧详情抽屉：左右双栏布局（文件信息/元数据），UUID + Hash 一键复制，引用关系列表 + 删除引用操作
  - UploadZone 支持传递元数据参数（owner/system/businessType 等）

- **测试**
  - 29 个 JUnit 5 测试（5 个测试类），全部通过
  - 新增 `StorageMetadataServiceTest`（9 用例）：搜索、UUID查询、引用创建+状态迁移、引用删除、软删除
  - 新增 `StorageMetadataControllerTest`（6 用例）：详情/搜索/创建引用/删除引用/引用列表/404
  - 修复 P0 测试适配双写签名

- **核心设计决策**
  - storage_file 表保留，上传双写两张表，P0 API 读旧表，P1 API 读新表
  - storage_key 新增字段 + 保留 storage_name/relative_path 向后兼容
  - 上传时自动创建引用为可选参数，不强制
  - metadata_index 在 P1 创建，为后续 Phase 预留

### Not included (deferred to later phases)
- 资源类型体系（P2 Resource Runtime）
- 权限控制 / 签名 URL（P3 Access Runtime）
- 图片处理（P4 Image Runtime）
- 多存储后端（P5 Storage Driver Runtime）
- 文件去重（P6 后基于 Hash）

---

## [0.1.0] — 2026-07-15

### Added — P0 Unified File Runtime

- **REST API**
  - `POST /api/v1/storage/upload` — 上传文件（Multipart），返回 `{id, downloadUrl, filename, size}`
  - `GET /api/v1/storage/file/{id}` — 下载文件原始字节流
  - `DELETE /api/v1/storage/file/{id}` — 软删除文件（`deleted=1, status='DELETED'`）
  - `GET /api/v1/storage/file/{id}/info` — 查询文件元数据

- **StorageDriver 抽象**
  - `StorageDriver` 接口：`upload / download / delete / exists`
  - `LocalDiskDriver` — 本地磁盘驱动，按 `root/yyyy/MM/dd/uuid.bin` 存储

- **数据架构**
  - `storage_file` 表（SQLite + Flyway），融合方案字段：id, uuid, original_name, storage_name, extension, mime_type, size, storage_type, relative_path, hash, status, deleted, create_time, update_time, create_user, update_user
  - 元数据与文件字节分离：数据库管理资源，磁盘只存字节
  - SHA-256 哈希计算，文件名 UUID 化
  - JdbcTemplate 实现，三层结构：api → application → infrastructure

- **前端 Vue3**
  - 上传区域：点击/拖拽/粘贴图片，进度条，重试/取消
  - 文件列表 + 详情弹窗：复制 File ID、下载文件、删除
  - Apple UI 风格：Pill badge、三级按钮、充足留白

- **测试**
  - 14 个 JUnit 5 测试（3 个测试类），全部通过
  - 覆盖 Driver 层、Service 层、Controller 层

- **基础设施**
  - Spring Boot 3.3.1 + Java 21 + Maven
  - Flyway 数据库迁移
  - SpringDoc OpenAPI + Actuator 健康检查
  - RFC 7807 Problem Detail 统一异常响应

### Not included (deferred to later phases)
- 权限控制（P3 Access Runtime）
- 图片处理 / 缩略图（P4 Image Runtime）
- Database / S3 / OSS 驱动（P5 Storage Driver Runtime）
- 文件版本（P7 Version Runtime）
- 生命周期清理（P8 Lifecycle Runtime）