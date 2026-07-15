这是整个 **core-storage** 的一个分水岭。

如果说：

* **P0 = 能存文件**
* **P1 = 能管理文件**

那么：

> **P2 = 文件升级为平台资源（Resource）**

这是整个架构最重要的一次升级。

很多开源项目（包括 MinIO、OSS SDK、Spring Resource）都停留在 **File/Object** 这一层。

而真正的平台（Google、AWS、GitHub、Notion、Figma）已经进入 **Resource** 思维。

以后平台里的任何东西，都应该是 Resource。

---

# Phase 2：Resource Runtime ⭐⭐⭐⭐⭐

> **目标：建立统一资源模型（Unified Resource Model），让图片、附件、模板、插件、AI模型、备份等全部成为平台资源，而不是文件。**

一句话：

> **Storage 不管理 File，而是管理 Resource。**

以后：

整个 Core Platform 认识的对象只有：

```
Resource
```

而不是：

```
File
```

---

# 一、为什么需要 Resource？

举个例子。

现在：

```
avatar.png
```

它只是：

```
File
```

但是实际上：

它还是：

```
用户头像

↓

公开资源

↓

Image

↓

可裁剪

↓

可缩略

↓

被User引用
```

再例如：

```
plugin.zip
```

不仅仅是：

ZIP。

它还是：

```
Plugin

↓

Version

↓

Install

↓

Marketplace
```

AI模型：

```
llama3.gguf
```

不仅仅：

文件。

而是：

```
AI Model

↓

Version

↓

Quantization

↓

Runtime
```

所以：

Metadata：

已经不够了。

需要：

```
Resource
```

---

# 二、整体架构

```
                    Storage Runtime

                           │

                  Resource Service

                           │

        ┌──────────────────┴─────────────────┐

 Resource Repository                Metadata Runtime

        │                                   │

 Resource Model                     Storage Metadata

        │                                   │

        └──────────────────┬────────────────┘

                           │

                     Storage Driver
```

以后：

业务：

全部：

查：

```
Resource
```

不是：

Metadata。

---

# 三、统一对象模型

新增：

```java
StorageResource
```

这是：

以后：

整个Storage：

最大的对象。

建议：

```
StorageResource

↓

Metadata

↓

Binary
```

关系：

```
StorageResource

↓

StorageMetadata

↓

Driver
```

而不是：

Metadata：

最大。

---

# 四、统一 Resource 类型

新增：

```java
enum ResourceType
```

建议：

```
IMAGE

VIDEO

AUDIO

DOCUMENT

ARCHIVE

PLUGIN

TEMPLATE

MODEL

BACKUP

EXPORT

ICON

FONT

DATASET

OTHER
```

以后：

业务：

不用：

```
Mime
```

判断。

直接：

```
ResourceType
```

即可。

例如：

```
application/pdf
```

属于：

```
DOCUMENT
```

---

# 五、Resource 生命周期

```
Create

↓

Upload

↓

Ready

↓

Referenced

↓

Published

↓

Deprecated

↓

Archived

↓

Deleted
```

以后：

插件：

模板：

AI模型：

全部：

一致。

---

# 六、数据库设计

## storage_resource

建议新增：

```sql
CREATE TABLE storage_resource
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,

    resource_uuid       VARCHAR(64) UNIQUE,

    metadata_uuid       VARCHAR(64),

    resource_name       VARCHAR(255),

    resource_type       VARCHAR(64),

    category            VARCHAR(64),

    description         TEXT,

    owner_type          VARCHAR(64),

    owner_id            VARCHAR(128),

    visibility          VARCHAR(32),

    status              VARCHAR(32),

    create_time         DATETIME,

    update_time         DATETIME
);
```

注意：

Metadata：

不再：

直接：

给业务。

Resource：

才是：

业务对象。

---

# 七、Resource 与 Metadata

关系：

```
StorageResource

↓

StorageMetadata

↓

Binary
```

例如：

```
StorageResource

↓

头像

↓

Metadata

↓

UUID

↓

Driver

↓

avatar.bin
```

所以：

以后：

删除：

先删：

```
Resource
```

不是：

Metadata。

---

# 八、分类（Category）

新增：

```
Avatar

Attachment

Plugin

Template

Logo

Banner

Backup

Dataset

Prompt

Model
```

ResourceType：

告诉：

是什么。

Category：

告诉：

干什么。

例如：

```
IMAGE

↓

Avatar
```

```
IMAGE

↓

Banner
```

都是：

Image。

但是：

用途：

不同。

---

# 九、统一 Owner

P2：

开始：

真正支持：

Owner。

例如：

```
OwnerType

↓

USER

SYSTEM

PROJECT

TEAM

PLUGIN
```

OwnerId：

```
1001
```

以后：

资源：

知道：

属于谁。

---

# 十、Visibility

新增：

```
PUBLIC

LOGIN

PRIVATE

SYSTEM
```

虽然：

权限：

P3。

但是：

Visibility：

必须：

现在：

建模。

以后：

不用：

改表。

---

# 十一、标签（Tag）

建议：

新增：

```sql
storage_resource_tag
```

```
Resource

↓

Tag

↓

Avatar

Logo

Dark

AI

OpenSource
```

以后：

搜索：

非常方便。

---

# 十二、属性扩展（Property）

资源：

以后：

一定：

有：

不同属性。

例如：

图片：

```
Width

Height
```

视频：

```
Duration
```

AI模型：

```
Context

Parameter

Precision
```

所以：

新增：

```sql
storage_resource_property
```

```
resource_uuid

key

value
```

例如：

```
width

1024
```

```
height

768
```

不用：

以后：

不停：

改表。

---

# 十三、API

Resource：

成为：

第一入口。

上传：

```
POST

/resources
```

查询：

```
GET

/resources/{uuid}
```

搜索：

```
GET

/resources/search
```

删除：

```
DELETE

/resources/{uuid}
```

Metadata：

变成：

内部API。

业务：

不要：

直接：

调用。

---

# 十四、前端 UX

### Resource Center

左侧：

```
全部资源

图片

附件

模板

插件

AI模型

导出

备份
```

右侧：

```
┌─────────────────────────────────────┐

🔍 搜索

─────────────────────────────────────

Avatar.png

Image

Avatar

User

Public

Ready

─────────────────────────────────────

Plugin.zip

Plugin

Marketplace

Published

─────────────────────────────────────
```

点击：

进入：

详情。

---

### Resource Detail

```
┌────────────────────────────────────────────┐

资源预览

━━━━━━━━━━━━━━━━━━━━━━

名称

类型

分类

Owner

Visibility

Tag

Metadata

Hash

Storage Driver

引用关系

下载

复制UUID

删除

```

顶部：

以后：

还能：

```
版本

历史

引用

```

P7：

直接：

接。

---

# 十五、交互设计

资源创建采用"向导式（Wizard）"体验，而不是单纯上传文件。

```text
┌──────────────────────────────────────────────┐
① 选择资源

[图片] [附件] [模板] [插件] [模型]

↓

② 上传文件

拖拽 / 点击上传

↓

③ 填写资源信息

名称
分类
描述
标签
可见性

↓

④ 完成

生成 Resource
```

这样用户创建的是一个**资源**，上传文件只是创建资源过程中的一步。

资源列表支持两种视图：

* **卡片视图**：适合图片、图标、模板等可预览资源。
* **表格视图**：适合插件、备份、模型、导出文件等结构化资源。

每个资源卡片建议展示：

* 缩略图（若支持）
* Resource Name
* Resource Type
* Category
* Owner
* Status
* 创建时间

点击进入详情页，再查看 Metadata、引用关系和存储信息。

---

# 十六、P2 的核心设计原则（必须坚持）

从 P2 开始，`core-storage` 已经不是文件服务器，而是**平台资源中心**。因此必须坚持以下原则：

1. **业务系统永远面向 `StorageResource` 编程，而不是 `StorageMetadata` 或文件对象。**
2. **一个 Resource 可以拥有多个版本（P7）、多个引用（P1）、多个属性（Property）以及多个标签（Tag），因此 Resource 必须保持稳定 ID。**
3. **Metadata 描述"如何存储"，Resource 描述"业务语义"，二者职责严格分离。**
4. **任何新资源类型（例如 Prompt、Workflow、AI Agent、主题、图标包）都应通过新增 `ResourceType` 和扩展属性支持，而不是修改数据库结构。**
5. **Resource 必须成为整个 Core Platform 的统一资源入口，未来 `core-user`（头像）、`core-plugin`（插件包）、`core-template`（模板）、`core-ai`（模型与 Prompt）、`core-backup`（备份）、`core-config`（导入导出）全部依赖这一层，而不是直接依赖 Storage Driver。**

---

## 从 P0 → P2 的架构演进

```text
P0：File Runtime
File
    │
    ▼
Storage Driver
    │
    ▼
Disk

────────────────────────────

P1：Metadata Runtime
Resource Metadata
        │
        ▼
Storage Driver
        │
        ▼
Binary

────────────────────────────

P2：Resource Runtime
StorageResource（业务对象）
        │
        ▼
StorageMetadata（存储对象）
        │
        ▼
StorageDriver（存储实现）
        │
        ▼
Disk / Database / S3 / OSS
```

至此，`core-storage` 已经从一个简单的文件上传组件，演进为一个具有统一资源模型的平台能力。后续 **P3 Access Runtime**、**P4 Image Runtime**、**P5 Storage Driver Runtime** 等，都将在 `StorageResource` 这一稳定抽象之上继续扩展，而无需改变业务层接口。
