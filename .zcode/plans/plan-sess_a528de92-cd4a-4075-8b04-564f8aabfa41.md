# P0 Unified File Runtime — 实现计划

## 架构摘要

Java 21 + Spring Boot + Maven + JdbcTemplate + SQLite + Flyway + Vue3 + Vite  
三层结构：**api** (Controller/Response/Exception) → **application** (Service/Domain/Port) → **infrastructure** (Persistence/Driver/Config)

## 后端（16 个文件）

### 基础设施
1. `pom.xml` — 依赖：spring-boot-starter-web, jdbc, sqlite-jdbc, flyway, springdoc, actuator
2. `application.yml` — 端口 8105，SQLite 数据库 `./data/core-storage.db`，上传上限 100MB
3. `V1__create_storage_file.sql` — Flyway 迁移，融合方案字段（含 create_user, update_user, deleted）
4. `CoreStorageApplication.java` — Spring Boot 启动类
5. `StorageProperties.java` — `@ConfigurationProperties("core.storage")` 配置绑定
6. `StorageConfig.java` — Bean 装配（JdbcTemplate, StorageDriver 等）

### Driver 层
7. `StorageDriver.java` — 接口：upload / download / delete / exists
8. `LocalDiskDriver.java` — 本地磁盘实现，按 `root/yyyy/MM/dd/uuid.bin` 存储

### 数据层
9. `StorageFile.java` — 领域对象（application/domain）
10. `StorageFileEntity.java` — 持久化实体（infrastructure/persistence/entity）
11. `StorageFileConverter.java` — Entity ↔ Domain 互转
12. `StorageFileRepository.java` — JdbcTemplate 数据访问

### 业务+API
13. `StorageService.java` — 上传流程：生成UUID → SHA-256 → INSERT metadata → Driver.upload() → 失败回滚；下载/删除/getInfo
14. `StorageFileResponse.java` — API 返回 DTO（id, downloadUrl, filename, size）
15. `StorageController.java` — 4 个 REST API：POST upload / GET download / DELETE / GET info
16. `GlobalExceptionHandler.java` — RFC 7807 Problem Detail 统一错误

## 前端 Vue3（7 个文件）

17. `web/package.json` — vue 3, vite, typescript, pinia, axios
18. `web/vite.config.ts` — proxy /api → localhost:8105
19. `web/src/api/storage.ts` — uploadFile / getFileInfo / getDownloadUrl / deleteFile
20. `web/src/stores/storage.ts` — Pinia 状态管理
21. `web/src/components/UploadZone.vue` — 点击/拖拽/粘贴上传 + 进度条 + 重试/取消，Apple UI 风格
22. `web/src/components/FileDetailModal.vue` — 文件详情（复制ID/下载/删除），三级按钮
23. `web/src/pages/StoragePage.vue` — 主页面组合

## 测试（3 个测试类）

24. `StorageServiceTest.java` — Mock Driver，验证上传/下载/删除/查询逻辑
25. `LocalDiskDriverTest.java` — @TempDir 验证文件物理读写
26. `StorageControllerTest.java` — MockMvc 端到端验证 4 个 API + 404 场景

## 创建顺序

先全部代码文件（1-23），后统一跑验证：
1. `mvn compile` → 编译通过
2. `mvn test` → 单元+集成测试全部通过
3. `mvn spring-boot:run` → 手动 curl 验证
4. 前端 `npm run dev` → 上传/下载/删除/详情 全流程验证

## 不做的

权限/图片处理/数据库Driver/S3/MinIO/版本/生命周期/CDN/加密/分片上传/文件类型过滤