我认为 **P1 Metadata Runtime** 才是整个 `core-storage` 的真正核心。

**P0 解决的是："文件放在哪里？"**

**P1 解决的是："这个文件到底是什么？属于谁？能干什么？"**

从这一阶段开始，`core-storage` 就不再是一个 Upload Service，而开始成为 **Resource Catalog（资源目录）**。

---

# Phase 1：Metadata Runtime ⭐⭐⭐⭐⭐

> **目标：建立统一的文件元数据中心（Metadata Center），所有资源统一通过 Metadata 管理，而不是通过磁盘管理。**

一句话：

> **磁盘负责存字节，数据库负责管理资源。**

以后真正查询的是：

```text
StorageMetadata
```

而不是：

```text
./storage/2026/07/15/xxxx.bin
```

---

# 一、为什么需要 Metadata？

很多系统一开始都是：

```text
upload/

avatar.png

abc.pdf

plugin.zip
```

时间久了以后：

没人知道：

这是哪个系统？

谁上传？

什么时候上传？

还能不能删？

有没有被引用？

最终：

磁盘越来越大。

没人敢删。

---

Metadata Runtime 就是解决这个问题。

以后任何文件都有身份。

例如：

```text
ID

属于谁

哪个系统

哪个模块

上传人

上传时间

是否引用

是否删除

Hash

Mime

标签

备注
```

整个系统：

先查 Metadata。

再查 Driver。

---

# 二、整体架构

```text
                   Storage Runtime

                          │

               StorageMetadataService

                          │

      ┌───────────────────┴──────────────────┐

 Metadata Repository                  Storage Driver

(SQLite/MySQL)                         LocalDisk

      │                                       │

 Storage Metadata                  Binary Content
```

最重要的一点：

> **Metadata 永远是真实来源（Source of Truth）。**

Driver 只是保存：

Byte。

---

# 三、统一对象模型

P1 新增：

```java
StorageMetadata
```

以后：

任何资源：

统一：

```text
StorageMetadata

↓

Driver

↓

Binary
```

而不是：

```text
Binary

↓

Metadata
```

主次一定不能反。

---

# 四、Metadata 生命周期

```text
Upload

↓

生成Metadata

↓

Driver上传

↓

Metadata状态=ACTIVE

↓

业务引用

↓

取消引用

↓

Soft Delete

↓

Lifecycle Runtime

↓

Physical Delete
```

所以：

Metadata：

比文件生命周期更长。

---

# 五、Metadata 表结构

## storage_metadata

建议：

```sql
CREATE TABLE storage_metadata
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,

    uuid                VARCHAR(64) NOT NULL UNIQUE,

    resource_name       VARCHAR(255),

    original_name       VARCHAR(255),

    extension           VARCHAR(32),

    mime_type           VARCHAR(128),

    file_size           BIGINT,

    hash_sha256         VARCHAR(64),

    storage_driver      VARCHAR(32),

    storage_key         VARCHAR(512),

    relative_path       VARCHAR(512),

    status              VARCHAR(32),

    deleted             BOOLEAN,

    create_time         DATETIME,

    update_time         DATETIME
);
```

---

这里只保存：

Metadata。

真正文件：

仍然：

```text
Driver
```

保存。

---

# 六、为什么要有 storage_key？

以后：

Local：

```text
storage_key

↓

2026/07/15/uuid.bin
```

S3：

```text
storage_key

↓

avatar/1001.png
```

OSS：

```text
storage_key

↓

plugin/a.zip
```

Driver：

自己解释。

业务：

不知道。

---

# 七、Resource Identity

新增：

```text
Resource Identity
```

例如：

```text
UUID

Hash

StorageKey

```

其中：

真正业务引用：

永远：

```text
UUID
```

数据库：

```text
ID
```

内部：

Driver：

```text
StorageKey
```

职责：

分离。

---

# 八、引用管理（Reference）

这是P1最重要的新增。

增加：

```sql
storage_reference
```

```sql
CREATE TABLE storage_reference
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    file_uuid VARCHAR(64),

    system_name VARCHAR(64),

    module_name VARCHAR(64),

    business_type VARCHAR(64),

    business_id VARCHAR(128),

    create_time DATETIME
);
```

例如：

头像：

```text
file

↓

user

↓

1001
```

知识库：

```text
file

↓

knowledge

↓

article123
```

插件：

```text
plugin

↓

plugin-market

↓

plugin001
```

以后：

一个文件：

可以：

多个引用。

---

为什么？

以后：

删除：

先查：

```text
Reference Count
```

如果：

```text
引用=0
```

才能：

真正删除。

---

# 九、Metadata 状态机

建议：

```text
UPLOADING

↓

ACTIVE

↓

REFERENCED

↓

UNREFERENCED

↓

SOFT_DELETED

↓

PHYSICAL_DELETED
```

以后：

Lifecycle Runtime：

自动：

扫描：

```text
UNREFERENCED
```

统一清理。

---

# 十、Metadata 查询

以后：

查询：

不是：

```text
路径
```

而是：

Metadata。

例如：

```text
GET

/storage/search
```

支持：

```text
文件名

Hash

Mime

大小

时间

UUID
```

以后：

甚至：

```text
Owner

Tag

Module
```

都可以。

---

# 十一、Hash Runtime

上传：

自动：

计算：

```text
SHA256
```

Metadata：

保存：

```text
Hash
```

以后：

可以：

快速：

发现：

重复文件。

例如：

```text
A.png

↓

SHA

↓

存在

↓

直接引用
```

不用：

重复存储。

虽然：

真正：

去重：

P5以后。

但是：

Hash：

现在就必须保存。

---

# 十二、前端 UX

新增：

Metadata 页面。

```
┌────────────────────────────────────────────────────────────┐

Storage Metadata

──────────────────────────────────────────────────────────────

搜索：

[文件名________]

Mime：

[Image ▼]

状态：

[ACTIVE ▼]

──────────────────────────────────────────────────────────────

UUID

Original Name

Size

Mime

Hash

Storage

引用数

创建时间

──────────────────────────────────────────────────────────────

点击：

详情

└────────────────────────────────────────────────────────────┘
```

---

详情：

```
UUID

Hash

Storage Driver

Storage Key

Mime

Original Name

File Size

Reference Count

Create Time

Status

```

下面：

显示：

```text
引用：

core-user

Avatar

1001
```

用户：

第一次：

真正知道：

文件：

被哪里使用。

---

# 十三、API

新增：

```http
GET /storage/metadata/{uuid}
```

查询：

Metadata。

---

```http
GET /storage/search
```

Metadata 搜索。

---

```http
POST /storage/reference
```

新增：

引用。

---

```http
DELETE /storage/reference
```

删除：

引用。

---

```http
GET /storage/reference/{uuid}
```

查询：

引用列表。

---

# 十四、交互设计

整个交互遵循"资源优先、文件其次"的思路。

### 资源列表

```
┌─────────────────────────────────────────────────────────────┐
🔍 搜索文件名 / UUID / Hash

状态：[全部]  类型：[全部]  系统：[全部]

─────────────────────────────────────────────────────────────

📄 avatar.png

UUID：3fd8...

引用：2

ACTIVE

─────────────────────────────────────────────────────────────

📄 plugin.zip

引用：12

ACTIVE

─────────────────────────────────────────────────────────────
```

点击资源进入详情，而不是直接下载。

---

### 资源详情

采用左右布局：

```
┌──────────────────────────────────────────────┐

左侧

文件信息

右侧

Metadata

Hash

UUID

Storage Driver

Storage Key

Mime

引用关系

引用历史

操作记录

```

顶部操作：

```
下载

复制 UUID

复制 Hash

查看引用

删除（软删除）
```

---

# 十五、P1 的核心设计原则（必须坚持）

这一阶段要建立的是整个资源平台的"数字档案"体系，因此有几条原则必须长期保持：

1. **Metadata 是唯一可信来源（Source of Truth），文件系统只是数据载体。**
2. **业务系统只能通过 UUID（或未来统一的 Resource ID）引用资源，不允许引用磁盘路径或对象存储 Key。**
3. **所有资源必须具备完整元数据（名称、类型、大小、Hash、存储驱动、状态等），禁止出现"无身份文件"。**
4. **引用关系（Reference）独立管理，资源与业务解耦，一个资源可以被多个业务共享。**
5. **删除采用软删除 + 引用检查机制，为后续 Lifecycle Runtime、去重、版本管理、对象存储迁移等企业能力打下基础。**

---

## 我建议对 P1 再增加一个能力：Metadata Index（元数据索引）

这是很多企业级对象存储都会尽早引入的设计。

除了 `storage_metadata` 外，再增加一张轻量索引表：

```sql
storage_metadata_index
```

用于维护：

* resource_id（统一资源 ID）
* owner_type / owner_id
* resource_type
* module
* tag
* create_time
* status

这样以后 P2 的 **Resource Runtime**、P3 的 **Access Runtime**、P8 的 **Lifecycle Runtime** 都直接基于索引工作，而不需要频繁扫描完整元数据表。这也是对象存储平台（如 S3、OSS、企业内容管理系统）常见的演进方向。
