# core-storage

Unified File Runtime for Core Platform — 统一文件上传、下载、删除、查询。

## 定位

> **让整个 Core Platform 只有一个地方负责文件。**

所有 `core-*` 服务统一通过 `core-storage` 进行文件存取，禁止业务直接读写磁盘。

## 快速开始

```bash
# 启动后端（端口 8105）
mvn spring-boot:run

# 启动前端（端口 5173，自动代理到后端）
cd web && npm install && npm run dev
```

默认 SQLite 数据库：`./data/core-storage.db`

## API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/storage/upload` | 上传文件（multipart `file`） |
| GET | `/api/v1/storage/file/{id}` | 下载文件 |
| DELETE | `/api/v1/storage/file/{id}` | 软删除文件 |
| GET | `/api/v1/storage/file/{id}/info` | 获取文件元数据 |

Swagger：`http://localhost:8105/swagger-ui.html`

## 架构

```
Browser / core-* services
        │
  StorageController (api)
        │
  StorageService (application)
   ┌────┴────┐
 Metadata    StorageDriver (port)
 (SQLite)         │
            LocalDiskDriver (infrastructure)
                  │
            ./data/storage/
```

三层结构：`api` → `application` → `infrastructure`

业务只保存 `fileId`，通过 `downloadUrl` 访问文件，不直接操作磁盘路径。

## 配置

```yaml
core:
  storage:
    driver: local          # 存储驱动（P0 仅 local）
    local:
      root: ./data/storage # 存储根目录
      date-path: true       # 按日期分目录：yyyy/MM/dd/
```

## 运行环境

- Java 21+
- Maven 3.5+
- SQLite（自动创建，无需额外安装）
- 前端：Node.js 18+

## 数据库

| 表 | 说明 |
|---|---|
| `storage_file` | 文件元数据（UUID、原始文件名、存储路径、哈希、状态） |

迁移工具：Flyway（`src/main/resources/db/migration/`）

## 不负责

- 权限控制
- 图片处理 / 缩略图
- 对象存储（S3 / OSS / MinIO）
- 文件版本管理
- 生命周期清理
- CDN / 加密 / 多租户

以上能力在后续 Phase 中逐步加入。

## 依赖服务

- 无（独立运行）

## 被依赖

所有 `core-*` 服务通过 HTTP REST 调用本服务。