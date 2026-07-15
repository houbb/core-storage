我非常赞同把 **core-storage** 放到 Core Platform 的前期。

因为真正的平台，**配置、用户、认证之后，第四个一定就是资源(Storage)**。

几乎所有业务都会依赖它：

* 用户头像
* Markdown图片
* 富文本附件
* 插件安装包
* AI生成图片
* 日志导出
* Excel导出
* PDF
* 视频
* 音频
* 模板
* 数据备份
* 数据快照
* AI模型
* Prompt模板
* 知识库附件

所以 Storage 不应该只是一个上传接口，而应该是：

> **统一资源(Runtime Resource)管理平台。**

---

# core-storage 的定位

> **Provide Unified Resource Storage Runtime**

一句话：

> **任何文件、任何资源，都通过 core-storage 管理。**

以后所有项目都不要直接操作磁盘。

统一：

```
Upload

↓

core-storage

↓

FileObject

↓

Storage Driver

↓

Disk
Database
S3
OSS
COS
MinIO
NAS
...
```

整个系统永远只认识：

```
Resource
```

而不是

```
D:\upload\a.png
```

或者

```
/tmp/a.jpg
```

---

# 整体RoadMap

```
P0
File Runtime ⭐⭐⭐⭐⭐

↓

P1
Metadata Runtime ⭐⭐⭐⭐⭐

↓

P2
Resource Runtime ⭐⭐⭐⭐⭐

↓

P3
Access Runtime ⭐⭐⭐⭐☆

↓

P4
Image Runtime ⭐⭐⭐⭐☆

↓

P5
Storage Driver Runtime ⭐⭐⭐⭐⭐

↓

P6
Replication Runtime ⭐⭐⭐⭐⭐

↓

P7
Version Runtime ⭐⭐⭐⭐☆

↓

P8
Lifecycle Runtime ⭐⭐⭐⭐⭐

↓

P9
Enterprise Resource Platform ⭐⭐⭐⭐⭐
```

下面逐步介绍。

---

# Phase 0：File Runtime（MVP）

目标：

**先让所有系统统一上传下载。**

目录：

```
upload/

avatar/

attachment/

plugin/

template/

export/

temp/

backup/
```

统一接口：

```
POST /upload

GET /file/{id}

DELETE /file/{id}
```

统一对象：

```
StorageFile

id

filename

size

contentType

path

storageType

createTime
```

Storage Driver：

```
interface StorageDriver{

upload()

download()

delete()

exists()

}
```

MVP只有：

```
LocalDiskDriver
```

真正保存：

```
D:/core-storage/

```

或者

```
./data/storage
```

即可。

---

为什么第一阶段不用S3？

因为：

> Storage接口先稳定。

不是Driver先稳定。

以后Driver怎么换，

业务完全不知道。

---

# Phase1 Metadata Runtime

MVP以后最大的痛点：

文件越来越多。

必须管理Metadata。

新增：

```
StorageObject
```

例如：

```
id

uuid

hash

filename

extension

mime

size

owner

system

module

tags

remark

createTime
```

以后真正查的是Metadata。

不是磁盘。

例如：

```
头像：

system=user

module=avatar

owner=1001
```

以后查询：

```
用户头像
```

不是

```
D:\avatar\a.png
```

---

为什么？

资源管理以后一定是：

Metadata驱动。

---

# Phase2 Resource Runtime

开始统一资源。

不仅仅文件。

统一：

```
Image

Video

Audio

PDF

Excel

Plugin

Template

Archive

Backup

AIModel

Prompt

```

全部：

```
StorageResource
```

增加：

```
ResourceType
```

例如：

```
IMAGE

VIDEO

AUDIO

ZIP

PLUGIN

MODEL

```

整个平台：

统一资源模型。

---

# Phase3 Access Runtime

资源开始需要权限。

例如：

头像：

公开。

插件：

管理员。

合同：

授权。

日志：

内部。

增加：

```
PUBLIC

LOGIN

ROLE

PRIVATE

SIGNED_URL

```

下载：

```
GET

↓

Permission

↓

Driver
```

支持：

```
Token

JWT

临时URL

```

以后：

OSS、

S3、

都能支持。

---

# Phase4 Image Runtime

图片以后需求很多。

例如：

头像：

生成缩略图。

Markdown：

压缩。

AI图片：

WebP。

新增：

```
Thumbnail

Resize

Crop

Rotate

Compress

Convert

Watermark

Blur
```

以后：

```
GET

↓

image runtime

↓

Driver
```

而不是：

业务自己压图。

---

# Phase5 Storage Driver Runtime

真正开始抽象Driver。

Driver：

```
Local

Database

S3

OSS

COS

MinIO

NAS

FTP
```

统一：

```
StorageDriver
```

例如：

```
StorageDriver

upload()

delete()

copy()

move()

presignedUrl()

```

业务：

完全不知道：

```
存哪里。
```

---

为什么Database放这里？

因为：

你现在希望：

多个服务。

没有共享磁盘。

数据库天然：

共享。

所以：

Driver：

```
DatabaseStorageDriver
```

即可。

以后：

```
A

↓

DB

↓

B

↓

下载
```

多个节点天然共享。

十分简单。

---

# Phase6 Replication Runtime

企业开始需要：

```
Local

↓

Database

↓

OSS

```

自动同步。

例如：

```
Upload

↓

Local

↓

Async Replication

↓

S3
```

或者：

```
DB

↓

OSS
```

以后：

Driver：

Primary

Secondary

Mirror

Backup

支持：

```
双写

同步

异步

Failover
```

---

# Phase7 Version Runtime

很多资源需要版本。

例如：

```
模板

插件

配置

AI模型

```

增加：

```
ResourceVersion
```

例如：

```
v1

v2

v3
```

支持：

```
Rollback

Compare

History
```

插件升级：

直接依赖这里。

---

# Phase8 Lifecycle Runtime

企业开始需要：

生命周期。

例如：

```
Temp

7天删除

```

```
Export

30天

```

```
Backup

一年

```

增加：

Lifecycle：

```
HOT

WARM

COLD

ARCHIVE

DELETE
```

自动：

```
扫描

↓

迁移

↓

删除

↓

归档
```

以后：

不用人工清理。

---

# Phase9 Enterprise Resource Platform

最后：

Storage已经不是：

文件系统。

而是：

企业资源平台。

支持：

### 多租户

```
Tenant
```

---

### 多Region

```
JP

CN

US
```

---

### 多Storage

```
S3

OSS

NAS

```

---

### CDN

```
Resource

↓

CDN

↓

User
```

---

### 内容审核

```
Upload

↓

AI Scan

↓

Virus

↓

Sensitive

↓

Storage
```

---

### 数据加密

```
AES

Envelope

KMS
```

---

### 操作审计

```
谁

什么时候

下载

删除

修改
```

全部记录。

---

# 为什么采用这样的 RoadMap？

整个路线遵循的是**能力逐层叠加，而不是过早引入复杂基础设施**。

| Phase                           | 核心目标                               | 为什么现在做                       |
| ------------------------------- | ---------------------------------- | ---------------------------- |
| P0 File Runtime                 | 建立统一上传/下载 API 与 `StorageDriver` 抽象 | 所有业务立即可用，先统一接口，再统一实现         |
| P1 Metadata Runtime             | 文件元数据中心                            | 后续检索、统计、权限、生命周期都依赖元数据        |
| P2 Resource Runtime             | 从"文件"升级到"资源"                       | 用统一资源模型支撑图片、插件、模板、模型等不同类型    |
| P3 Access Runtime               | 统一访问控制                             | 避免各业务重复实现下载鉴权                |
| P4 Image Runtime                | 图片处理能力                             | 图片是最常见资源，独立运行时可复用且便于扩展       |
| P5 Storage Driver Runtime       | 可插拔存储驱动                            | 在接口稳定后接入数据库、S3、OSS、MinIO 等后端 |
| P6 Replication Runtime          | 多节点共享与容灾                           | 满足高可用、跨节点部署和数据冗余             |
| P7 Version Runtime              | 资源版本管理                             | 支撑插件、模板、模型等可回滚资源             |
| P8 Lifecycle Runtime            | 生命周期治理                             | 自动归档、清理、冷热分层，降低运维成本          |
| P9 Enterprise Resource Platform | 企业级能力整合                            | 多租户、多区域、CDN、审计、加密、内容安全等统一纳管  |

## 与你整个 Core Platform 的契合

放在目前规划中，`core-storage` 会成为所有 Core 模块共享的基础设施：

```
core-auth
        │
        ├──────────────┐
core-user             │
        │              │
        ▼              │
   core-storage ◄──────┤
        ▲              │
        │              │
core-config            │
core-plugin            │
core-template          │
core-notify            │
core-ai                │
core-audit             │
core-backup            │
```

其中有一个设计点尤其值得坚持：**业务系统永远只面向 `StorageService` 和资源 ID 编程，而不是面向磁盘路径、S3 URL 或数据库 BLOB 编程。**

这样未来从本地文件切换到数据库存储，再切换到 MinIO、S3、OSS，甚至同时使用多种存储介质，都无需修改业务代码，只需要替换或新增 `StorageDriver` 实现。这也是从 MVP 平滑演进到企业级平台时，成本最低、可维护性最高的一条路线。
