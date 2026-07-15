# P2 Resource Runtime 实现方案

## 一、架构定位

在现有 P0（File → Disk）+ P1（Metadata → Driver → Binary）之上，新增 Resource 抽象层：

```
StorageResource（业务对象）  ← NEW P2
    │
StorageMetadata（存储对象）  ← P1 已有
    │
StorageDriver（存储实现）    ← P0 已有
```

## 二、数据模型新增

### 2.1 新枚举类（`application/domain/enums/`）

| 枚举 | 值 |
|------|-----|
| `ResourceType` | IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, PLUGIN, TEMPLATE, MODEL, BACKUP, EXPORT, ICON, FONT, DATASET, OTHER |
| `ResourceCategory` | AVATAR, ATTACHMENT, PLUGIN, TEMPLATE, LOGO, BANNER, BACKUP, DATASET, PROMPT, MODEL, OTHER |
| `Visibility` | PUBLIC, LOGIN, PRIVATE, SYSTEM |
| `ResourceStatus` | CREATED, UPLOADING, READY, REFERENCED, DELETED |

### 2.2 数据库新表（V3__create_resource_tables.sql）

**`storage_resource`**（核心资源表，11 业务字段 + 5 标准字段）：
```sql
id, resource_uuid, metadata_uuid, resource_name, resource_type, 
category, description, owner_type, owner_id, visibility, status,
create_time, update_time, create_user, update_user
```

**`storage_resource_tag`**（标签表）：
```sql
id, resource_uuid, tag_name, create_time, update_time, create_user, update_user
```

**`storage_resource_property`**（属性扩展表）：
```sql
id, resource_uuid, prop_key, prop_value, create_time, update_time, create_user, update_user
```

### 2.3 新 Domain 对象

`StorageResource` — 聚合根，持有 `resource_uuid`、`metadata_uuid`、类型/分类/可见性/状态/标签列表/属性列表

## 三、生命周期（5 态）

```
UPLOADING → READY → REFERENCED → DELETED
```

与 Metadata 状态同步映射：
- Resource UPLOADING ↔ Metadata UPLOADING
- Resource READY ↔ Metadata ACTIVE
- Resource REFERENCED ↔ Metadata REFERENCED
- Resource DELETED ↔ Metadata SOFT_DELETED

（CREATED 预留用于后续 Wizard 模式，本期不走此状态）

## 四、API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/storage/resources` | 上传文件并创建资源（Multipart + 资源元数据参数） |
| `GET` | `/api/v1/storage/resources/{uuid}` | 查询资源详情 |
| `GET` | `/api/v1/storage/resources/search` | 多条件搜索（type/category/visibility/owner/keyword/tag + 分页） |
| `PUT` | `/api/v1/storage/resources/{uuid}` | 更新资源信息（名称/描述/分类/可见性/标签） |
| `DELETE` | `/api/v1/storage/resources/{uuid}` | 软删除资源（→ DELETED） |
| `GET` | `/api/v1/storage/resources/{uuid}/properties` | 获取扩展属性 |
| `PUT` | `/api/v1/storage/resources/{uuid}/properties` | 设置/更新扩展属性 |

**关键设计**：上传接口保持 `POST /api/v1/storage/upload` 不变（向后兼容），新增 `POST /api/v1/storage/resources` 创建资源感知上传。

## 五、上传流程（扩展现有 upload）

在 `StorageService.upload()` 中增加逻辑块：
1. 当请求携带 `resourceType` 参数时，自动创建 StorageResource 记录
2. 自动推断：未填 resourceType 时按 MIME 自动映射（image/png→IMAGE）
3. 自动推断：未填 category 时默认 OTHER
4. 同步创建 storage_resource + storage_resource_tag（tags 按逗号分割入库）
5. 上传完成：Resource status → READY，Metadata status → ACTIVE

## 六、后端新建文件清单（共 18 个文件）

### 枚举（4 个）
- `ResourceType.java`
- `ResourceCategory.java`
- `Visibility.java`
- `ResourceStatus.java`

### Domain（1 个）
- `StorageResource.java`

### Entity（3 个）
- `StorageResourceEntity.java`
- `StorageResourceTagEntity.java`
- `StorageResourcePropertyEntity.java`

### Converter（1 个）
- `StorageResourceConverter.java`

### Repository（3 个）
- `StorageResourceRepository.java`
- `StorageResourceTagRepository.java`
- `StorageResourcePropertyRepository.java`

### Service（1 个）
- `StorageResourceService.java`

### Controller（1 个）
- `StorageResourceController.java`

### Response DTO（1 个）
- `StorageResourceResponse.java`

### Migration（1 个）
- `V3__create_resource_tables.sql`

### 修改文件（2 个）
- `StorageService.java` — upload 方法增加 Resource 创建逻辑
- `StorageMetadataService.java` — 软删除时同步更新 Resource 状态

## 七、前端新建/修改

### 新建文件（3 个）
- `web/src/stores/resource.ts` — 资源状态管理
- `web/src/pages/ResourcePage.vue` — 资源中心页面（左侧分类导航 + 右侧资源列表，支持卡片/表格双视图）
- `web/src/components/ResourceDetailDrawer.vue` — 资源详情抽屉（预览区/资源信息/标签/属性/Metadata/引用关系/操作按钮）

### 修改文件（2 个）
- `web/src/App.vue` — 添加第三个 Tab「🧩 资源中心」
- `web/src/api/storage.ts` — 新增 resource API 函数

## 八、测试（2 个新测试类）

- `StorageResourceServiceTest.java` — 含：创建资源、MIME→ResourceType 推断、搜索、状态迁移同步、标签/属性 CRUD、软删除
- `StorageResourceControllerTest.java` — 含：上传创建资源、查询资源、搜索、更新、删除、属性操作

## 九、不包含（明确 defer）

- ❌ 全量历史数据回迁（仅新数据创建 Resource）
- ❌ 完整 Wizard 式资源创建流程（CREATED 状态预留）
- ❌ Published/Deprecated/Archived 状态（留给 P3+）
- ❌ 资源版本（P7）
- ❌ 权限控制（P3 Access Runtime）
- ❌ 图片缩略图生成（P4 Image Runtime）
