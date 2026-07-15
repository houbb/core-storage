# CHANGELOG

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