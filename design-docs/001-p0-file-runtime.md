我建议 **P0 不要设计成 File Runtime，而应该设计成 Unified File Runtime（统一文件运行时）**。

因为这个模块以后不会只是「上传文件」，而是整个平台的**文件入口**。P0 就应该把整个架构定死，后面所有能力都只是往里面加，而不是推翻重来。

---

# Phase 0：Unified File Runtime ⭐⭐⭐⭐⭐

> **目标：提供一个统一、稳定、可扩展的文件上传、下载、删除、查询能力，所有业务统一接入，不允许直接操作磁盘。**

**阶段目标**

一句话：

> **让整个 Core Platform 只有一个地方负责文件。**

以后：

```text
core-user
core-config
core-plugin
core-template
core-ai
core-notify
core-audit
...
```

全部调用：

```text
core-storage
```

而不是：

```java
new File(...)
Files.copy(...)
MultipartFile.transferTo(...)
```

整个系统禁止业务直接写磁盘。

---

# 一、设计原则（第一性原理）

P0 只有四个原则。

## ① 文件不是路径，而是对象（File Object）

错误设计：

```text
D:\upload\avatar\a.png
```

正确设计：

```text
FileObject

↓

id

↓

Driver

↓

Disk
```

业务只认识：

```text
fileId
```

例如：

```text
avatarId

pluginId

attachmentId
```

永远不要保存：

```text
D:\xxx
```

---

## ② Storage Driver 解耦

所有上传：

```text
Upload

↓

StorageService

↓

StorageDriver

↓

LocalDisk
```

以后：

```text
StorageDriver

↓

Database

↓

OSS

↓

S3
```

业务零修改。

---

## ③ Metadata 和文件分离

文件：

```text
D:\storage
```

数据库：

```text
FileMetadata
```

永远：

数据库管理文件。

不是磁盘。

---

## ④ UUID 永远不是文件名

用户：

```text
头像.png
```

保存：

```text
d8b4d6f9....

```

否则：

以后：

```text
头像.png

头像.png

头像.png
```

一定冲突。

---

# 二、整体架构

```text
                    Browser

                       │

            POST /storage/upload

                       │

              StorageController

                       │

              StorageService

          ┌────────────┴────────────┐
          │                         │
 Metadata Repository          StorageDriver
          │                         │
      SQLite                LocalDiskDriver
          │                         │
          └────────────┬────────────┘
                       │
               ./data/storage/
```

注意：

Controller 不碰磁盘。

Service 不拼路径。

只有 Driver 知道：

```text
./storage/
```

---

# 三、目录结构

建议：

```text
core-storage

│
├── api
│
├── controller
│
├── service
│
├── driver
│
│      StorageDriver
│
│      LocalDiskDriver
│
├── repository
│
├── entity
│
├── dto
│
├── config
│
└── util
```

以后新增：

```text
DatabaseDriver

S3Driver

OSSDriver
```

直接：

```text
driver/
```

即可。

---

# 四、上传流程

```text
用户

↓

选择文件

↓

POST /upload

↓

StorageController

↓

StorageService

↓

生成UUID

↓

Metadata

↓

Driver.upload()

↓

磁盘

↓

返回FileId
```

重点：

数据库保存成功以后，

Driver才真正上传。

失败：

自动回滚。

---

# 五、下载流程

```text
GET /storage/{id}

↓

Metadata

↓

Driver

↓

InputStream

↓

Response
```

Controller：

不知道：

文件在哪。

---

# 六、删除流程

```text
DELETE

↓

Metadata

↓

Driver.delete()

↓

删除磁盘

↓

删除Metadata
```

建议：

P0：

先：

软删除。

```text
deleted=true
```

以后：

Lifecycle Runtime

统一清理。

---

# 七、数据库设计

## storage_file

这是唯一核心表。

```sql
CREATE TABLE storage_file
(
    id                INTEGER PRIMARY KEY AUTOINCREMENT,

    uuid              TEXT NOT NULL UNIQUE,

    original_name     TEXT NOT NULL,

    storage_name      TEXT NOT NULL,

    extension         TEXT,

    mime_type         TEXT,

    size              BIGINT,

    storage_type      TEXT,

    relative_path     TEXT,

    hash              TEXT,

    status            TEXT,

    deleted           INTEGER DEFAULT 0,

    create_time       DATETIME,

    update_time       DATETIME
);
```

---

为什么：

不用：

```text
file_path
```

因为：

以后：

```text
S3

OSS
```

没有：

file_path。

只有：

object key。

所以：

统一：

```text
relative_path
```

例如：

```text
2026/07/15/

```

storage_name：

```text
UUID.bin
```

Driver：

自己拼。

---

# 八、对象设计

```java
StorageFile
```

```text
id

uuid

originalName

storageName

extension

mimeType

size

relativePath

storageType

status

deleted
```

---

返回：

```java
StorageFileResponse
```

```text
id

downloadUrl

filename

size
```

以后：

前端：

永远：

拿：

```text
downloadUrl
```

不用拼路径。

---

# 九、REST API

## 上传

```http
POST /storage/upload
```

Multipart：

```text
file
```

返回：

```json
{
    "id": 101,
    "url": "/storage/file/101"
}
```

---

下载：

```http
GET /storage/file/{id}
```

---

删除：

```http
DELETE /storage/file/{id}
```

---

查询：

```http
GET /storage/file/{id}/info
```

返回：

Metadata。

---

# 十、Storage Driver

接口：

```java
public interface StorageDriver {

    UploadResult upload(...);

    InputStream download(...);

    boolean delete(...);

    boolean exists(...);

}
```

P0：

只有：

```text
LocalDiskDriver
```

以后：

```text
DatabaseDriver

S3Driver

OSSDriver

MinIODriver
```

直接实现。

---

# 十一、前端 UX 设计

P0 不需要做复杂的「网盘」。

重点是统一体验。

## 上传窗口

```text
┌────────────────────────────┐

 上传文件

─────────────────────────────

[拖拽到这里]

或者

[选择文件]

─────────────────────────────

example.pdf

█████████░░░ 92%

5.6MB

取消

─────────────────────────────

完成

└────────────────────────────┘
```

---

支持：

* 点击上传
* 拖拽上传
* 粘贴上传（图片）
* 上传进度
* 上传失败重试
* 上传取消

---

## 文件详情

点击：

```text
文件
```

弹窗：

```text
文件名

UUID

大小

类型

创建时间

下载

复制ID

删除
```

这里最重要的是 **复制 File ID**，因为平台内部所有业务引用的都是资源 ID，而不是路径。

---

# 十二、配置设计

```yaml
core:
  storage:
    driver: local

    local:
      root: ./data/storage

      temp: ./data/temp

      auto-create: true

      date-path: true
```

目录：

```text
storage/

2026/

07/

15/

uuid.bin
```

这样：

单目录：

不会：

几十万文件。

---

# 十三、P0 暂时不要做的事情

下面这些能力全部故意放到后续 Runtime，不要提前实现：

| 能力                 | 放到哪个 Phase                      | 原因                  |
| ------------------ | ------------------------------- | ------------------- |
| 权限控制（公开/私有/签名 URL） | P3 Access Runtime               | 与认证、授权体系耦合，P0 保持简单  |
| 图片缩略图、压缩、水印        | P4 Image Runtime                | 图片处理属于独立能力，不应污染上传逻辑 |
| 数据库存储 Driver       | P5 Storage Driver Runtime       | 先稳定驱动接口，再增加实现       |
| MinIO / S3 / OSS   | P5 Storage Driver Runtime       | 与业务无关，只是 Driver 扩展  |
| 文件版本               | P7 Version Runtime              | 并非所有资源都需要版本         |
| 生命周期（自动清理、归档）      | P8 Lifecycle Runtime            | 依赖调度与策略引擎           |
| CDN、对象加密、跨区域复制     | P9 Enterprise Resource Platform | 属于企业级能力             |

---

# 十四、P0 最重要的几个架构约束（必须坚持）

这是后续能否平滑演进到企业级的关键：

1. **业务系统只能保存 `fileId`，禁止保存磁盘路径、URL 或对象存储 Key。**
2. **所有文件访问必须经过 `StorageService`，业务代码禁止直接读写磁盘。**
3. **只有 `StorageDriver` 知道底层存储介质，Controller 和 Service 不允许拼接真实路径。**
4. **元数据与文件内容彻底分离，数据库负责管理文件，磁盘只负责保存文件字节。**
5. **所有文件名统一 UUID 化，保留 `original_name` 仅用于展示。**
6. **所有下载地址统一由接口生成，而不是暴露真实存储位置，为后续接入签名 URL、CDN、对象存储和权限控制预留扩展点。**

坚持这几条约束，`core-storage` 就可以从一个只有本地磁盘的 MVP，自然演进到支持数据库共享、多节点部署、MinIO、S3、OSS、生命周期管理以及企业级资源平台，而无需修改上层业务代码。
