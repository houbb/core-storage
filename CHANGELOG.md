# CHANGELOG

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