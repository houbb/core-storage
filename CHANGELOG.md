# CHANGELOG

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