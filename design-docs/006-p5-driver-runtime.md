这是整个 `core-storage` 最关键的一层。

前面：

* P0~P4 都是**业务能力**
* **P5 开始真正实现"存储与业务彻底解耦"**

我甚至认为，它的重要性仅次于 **Resource Runtime**。

很多系统一开始就直接支持：

* Local
* MinIO
* OSS
* S3

最后代码越来越乱。

**正确的顺序应该是：**

> **先稳定 Resource API，再稳定 Driver SPI，最后再不断增加 Driver。**

所以 P5 的目标不是支持更多存储，而是建立一个**统一的 Storage Driver SPI（Storage Service Provider Interface）**。

---

# Phase 5：Storage Driver Runtime ⭐⭐⭐⭐⭐

> **目标：建立统一存储驱动层（Storage Driver Runtime），让任何存储介质都可以无缝接入，业务永远无需感知底层实现。**

一句话：

> **业务只认识 Resource，Storage Runtime 只认识 Driver。**

以后：

```text
User

↓

Resource Runtime

↓

Storage Runtime

↓

Storage Driver

↓

Disk
Database
MinIO
OSS
S3
Azure
NAS
FTP
...
```

Resource 永远不知道：

到底存在哪里。

---

# 一、为什么需要 Driver Runtime？

如果没有 Driver。

代码会变成：

```java
if(local){

}

if(minio){

}

if(oss){

}

if(s3){

}
```

整个项目：

越来越大。

Driver Runtime：

就是解决：

这个问题。

以后：

新增：

```text
Huawei OBS

Ceph

NAS

FTP
```

全部：

新增：

Driver。

不用：

改业务。

---

# 二、整体架构

```text
               Storage Runtime

                       │

              StorageDriver SPI

        ┌──────────────┼──────────────┐

 Local Driver   Database Driver   Object Driver

        │               │                │

     Disk           SQLite/MySQL     S3/OSS/MinIO
```

以后：

Storage Runtime：

只认识：

```java
StorageDriver
```

---

# 三、统一 Driver SPI

建议：

```java
public interface StorageDriver {

    DriverType type();

    UploadResult upload(...);

    DownloadResult download(...);

    DeleteResult delete(...);

    MoveResult move(...);

    CopyResult copy(...);

    ExistsResult exists(...);

    MetadataResult metadata(...);

    UrlResult url(...);

    HealthResult health();
}
```

注意：

不要：

只有：

```java
upload()

download()
```

企业：

一定：

需要：

Move

Copy

Health

Metadata

---

# 四、Driver 生命周期

统一：

```text
Load

↓

Initialize

↓

Health Check

↓

Ready

↓

Serving

↓

Stopping

↓

Stopped
```

以后：

Driver：

可以：

热切换。

---

# 五、DriverType

建议：

统一：

```java
enum DriverType
```

```text
LOCAL

DATABASE

MINIO

S3

OSS

COS

AZURE

NAS

FTP

CUSTOM
```

以后：

插件：

还能：

注册：

Driver。

---

# 六、Storage Profile

新增：

Storage Profile。

例如：

```text
default

↓

LOCAL
```

图片：

```text
image

↓

MINIO
```

备份：

```text
backup

↓

NAS
```

AI模型：

```text
model

↓

S3
```

不是：

所有资源：

都：

一个Driver。

---

## storage_profile

```sql
CREATE TABLE storage_profile
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    profile_name VARCHAR(64),

    driver_type VARCHAR(32),

    root_path VARCHAR(512),

    bucket_name VARCHAR(128),

    endpoint VARCHAR(255),

    enabled BOOLEAN,

    create_time DATETIME
);
```

以后：

Resource：

绑定：

Profile。

不是：

Driver。

---

# 七、Resource 与 Driver

新增：

```text
Resource

↓

Storage Profile

↓

Driver
```

而不是：

```text
Resource

↓

Driver
```

为什么？

以后：

切换：

Profile：

即可。

不用：

Resource：

全部：

改。

---

# 八、Driver Factory

建议：

新增：

```java
StorageDriverFactory
```

统一：

```java
StorageDriver driver =

factory.get(profile);
```

不要：

```java
new LocalDriver()
```

以后：

Driver：

插件化。

---

# 九、Driver Registry

新增：

```java
DriverRegistry
```

启动：

```text
Local

↓

注册
```

MinIO：

```text
↓

注册
```

OSS：

```text
↓

注册
```

以后：

Factory：

查：

Registry。

---

# 十、Health Runtime

Driver：

增加：

```java
health()
```

例如：

Local：

```text
Disk OK
```

S3：

```text
Bucket OK
```

Database：

```text
Connection OK
```

以后：

平台：

直接：

监控：

Storage。

---

# 十一、数据库设计

## storage_driver

```sql
CREATE TABLE storage_driver
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    driver_name VARCHAR(64),

    driver_type VARCHAR(32),

    version VARCHAR(32),

    enabled BOOLEAN,

    status VARCHAR(32),

    health_status VARCHAR(32),

    create_time DATETIME
);
```

---

## storage_profile

```sql
CREATE TABLE storage_profile
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    profile_name VARCHAR(64),

    driver_name VARCHAR(64),

    is_default BOOLEAN,

    create_time DATETIME
);
```

---

## storage_resource

新增：

```text
profile_name
```

以后：

下载：

Resource：

↓

Profile：

↓

Driver。

---

# 十二、API

Driver：

```http
GET

/storage/drivers
```

详情：

```http
GET

/storage/drivers/{name}
```

Health：

```http
GET

/storage/drivers/{name}/health
```

Profile：

```http
GET

/storage/profiles
```

新增：

```http
POST

/storage/profiles
```

切换：

```http
PUT

/storage/profiles/{id}
```

---

# 十三、前端 UX

新增：

Storage Drivers。

```text
┌────────────────────────────────────────────┐

Drivers

──────────────────────────────────────────────

🟢 Local

Healthy

──────────────────────────────────────────────

⚪ Database

Disabled

──────────────────────────────────────────────

⚪ MinIO

Disabled

──────────────────────────────────────────────

⚪ S3

Disabled

```

点击：

详情。

---

Profile：

```text
┌────────────────────────────────────────────┐

Default

↓

Local

━━━━━━━━━━━━━━━━━━━━━━

Image

↓

MinIO

━━━━━━━━━━━━━━━━━━━━━━

Backup

↓

NAS

```

管理员：

直接：

切换。

---

# 十四、交互设计（重点）

### Storage Drivers 页面

采用"运行时监控"而不是"配置中心"的设计。

```text
┌─────────────────────────────────────────────────────────────┐

Storage Drivers

─────────────────────────────────────────────────────────────

Driver      Status      Profile Count      Health

🟢 Local     Running     2                  Healthy

🟢 Database  Running     1                  Healthy

⚪ MinIO     Disabled    0                  -

─────────────────────────────────────────────

点击进入 Driver
```

进入 Driver 后：

```text
Basic

Endpoint

Root Path

Health

Capacity

Read

Write

Latency

Recent Errors

```

不是：

只有：

配置。

真正：

Runtime。

---

### Storage Profile 页面

Profile：

建议：

拖拽绑定。

```text
Image

↓

Local

↓

MinIO（切换）

↓

保存
```

管理员：

不需要：

修改代码。

---

### 上传流程

上传：

```text
Upload

↓

选择：

Profile

↓

Storage Runtime

↓

Factory

↓

Driver

↓

完成
```

用户：

完全：

不知道：

Driver。

---

# 十五、为什么 Database Driver 要放到 P5？

这是与你最初规划最契合的一点。

你的目标是：

> **前期保持部署极简，不依赖 Redis、MQ、MinIO 等额外组件；多节点时也希望能够共享文件。**

因此数据库驱动（SQLite / MySQL）不是权宜之计，而是 **Storage Driver 的正式实现之一**。

建议支持两种数据库模式：

### SQLite（默认）

适用于：

* 单机部署
* 零依赖
* Demo
* 小团队

资源内容：

```text
Metadata → SQLite

Binary → Local Disk
```

或者：

```text
Metadata → SQLite

Binary → SQLite BLOB（可配置）
```

---

### MySQL（共享）

适用于：

* 多节点
* Docker
* K8s

资源内容：

```text
Metadata → MySQL

Binary → MySQL BLOB
```

或者：

```text
Metadata → MySQL

Binary → Shared Disk
```

这样：

A 服务：

上传。

B 服务：

立即：

可以：

下载。

无需：

NAS。

无需：

MinIO。

这是一个非常符合 **MVP → 企业级** 演进路线的设计。

---

# 十六、P5 核心设计原则（必须坚持）

这一阶段真正要固定的是**驱动边界**，未来十年都尽量不要改变。

因此建议坚持以下原则：

1. **Storage Runtime 永远只依赖 `StorageDriver` SPI，禁止依赖任何具体 Driver。**
2. **Resource 永远绑定 `StorageProfile`，而不是绑定具体 Driver，实现资源与存储介质彻底解耦。**
3. **每个 Driver 都必须实现统一能力（上传、下载、删除、复制、移动、元数据、健康检查），保证行为一致。**
4. **新增存储能力只能通过新增 Driver，不允许修改 Runtime 或业务代码。**
5. **Database Driver 与 Local Driver 同等地位，它不是临时方案，而是 MVP、多节点部署和企业演进中的正式存储后端。**

---

## 我建议在 P5 再补充两个企业级扩展点（先预留接口，不急于实现）

### ① Capability（驱动能力声明）

不同 Driver 支持的能力不同，例如：

```text
Local：
✔ Move
✔ Copy
✔ Stream

S3：
✔ Signed URL
✔ Multipart Upload

Database：
✔ Transaction
✔ Consistency
```

建议：

```java
StorageCapability
```

统一声明：

```text
MULTIPART_UPLOAD
VERSIONING
SIGNED_URL
STREAMING
TRANSACTION
LIFECYCLE
```

Runtime 根据 Capability 自动启用或隐藏相关功能，而不是通过 `if(driver instanceof S3Driver)` 判断。

---

### ② Driver Migration（驱动迁移）

后续企业版一定会遇到：

```text
Local
   ↓
MinIO
   ↓
S3
```

如果现在就让 Resource 绑定 `StorageProfile`，那么未来只需要新增：

```text
Migration Task
```

即可后台迁移资源，迁移完成后切换 Profile，而**Resource ID、业务引用、访问 API 全部保持不变**。

这会成为 `core-storage` 从 MVP 演进到企业级对象存储平台时最有价值的能力之一。
