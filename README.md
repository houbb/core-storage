# core-storage

Unified File Runtime for Core Platform — 统一文件上传、下载、删除、查询。

## 定位

> **让整个 Core Platform 只有一个地方负责文件。**

所有 `core-*` 服务统一通过 `core-storage` 进行文件存取，禁止业务直接读写磁盘。

## 快速开始

```bash
# 启动后端（端口 8105）
cd core-storage-backend && mvn spring-boot:run

# 启动前端（端口 5173，自动代理到后端）
cd core-storage-frontend && npm install && npm run dev
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

## 结构

```
core-storage/
├── README.md
├── LICENSE
├── design-docs/              # 设计文档
├── core-storage-backend/     # Spring Boot 3.3.1 后端服务
│   ├── pom.xml
│   └── src/
│       ├── main/java/...     # Controller / Service / Domain / Infrastructure
│       └── main/resources/   # application.yml / Flyway 迁移
└── core-storage-frontend/    # Vue 3 + Vite + TypeScript 前端
    ├── package.json
    └── src/                  # Components / Pages / Stores / API
```

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
| `storage_metadata` | 统一元数据中心 |
| `storage_resource` | 统一资源模型 |
| `storage_access_policy` | 访问策略 |
| `storage_resource_share` | 资源分享 |
| `storage_access_log` | 访问日志 |
| `storage_image` | 图片元数据 |
| `storage_driver` | 存储驱动注册 |
| `storage_profile` | 存储配置 |
| `storage_replica` | 数据副本 |
| `storage_version` | 资源版本 |
| `storage_lifecycle_policy` | 生命周期策略 |
| `storage_tenant` | 多租户 |
| `storage_quota` | 配额管理 |

迁移工具：Flyway（`core-storage-backend/src/main/resources/db/migration/`）

## 能力矩阵

| Phase | 能力 | 状态 |
|---|---|---|
| P0 | 文件上传/下载/删除 | ✅ |
| P1 | 元数据中心 | ✅ |
| P2 | 统一资源模型 | ✅ |
| P3 | 访问控制（策略/分享/签名URL） | ✅ |
| P4 | 图片处理（缩略图/压缩/格式转换） | ✅ |
| P5 | 多存储驱动（Local/Database/S3/MinIO） | ✅ |
| P6 | 数据复制与同步 | ✅ |
| P7 | 资源版本管理 | ✅ |
| P8 | 生命周期治理（冷热分层/归档/删除） | ✅ |
| P9 | 企业资源平台（多租户/配额/审计/扫描/仪表盘） | ✅ |

## 依赖服务

- 无（独立运行）

## 被依赖

所有 `core-*` 服务通过 HTTP REST 调用本服务。