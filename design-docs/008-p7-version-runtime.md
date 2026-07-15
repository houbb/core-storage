我认为 **P7 Version Runtime** 是 `core-storage` 从"存储平台"迈向"内容平台（Content Platform）"的关键一步。

很多存储系统只能保存**一个文件**。

真正的平台保存的是：

> **资源（Resource）的演化历史（Evolution）。**

例如：

* 插件有 v1、v2、v3
* 模板不断修改
* Logo 经常替换
* AI Model 不断升级
* Prompt 持续优化
* 合同不断修订
* 配置不断发布

如果没有 Version Runtime，最终只能：

```text
plugin_v1.zip

plugin_v2.zip

plugin_v3_final.zip

plugin_v3_final_new.zip

plugin_v3_final_last.zip
```

所有企业最后都会变成这样。

所以 **Version Runtime** 不是"文件版本"，而是：

> **Resource Evolution Runtime（资源演化运行时）**

---

# Phase 7：Version Runtime ⭐⭐⭐⭐⭐

> **目标：建立统一资源版本管理平台，使任何 Resource 都可以拥有多个版本，并支持历史、比较、回滚、发布。**

一句话：

> **Resource 永远稳定，Version 不断演进。**

整个系统：

以后认识：

```text
Resource

↓

Version

↓

Metadata

↓

Driver
```

而不是：

```text
Resource

↓

Metadata
```

---

# 一、为什么需要 Version？

举个例子。

插件：

```text
Plugin

↓

1.0

↓

1.1

↓

1.2

↓

2.0
```

用户：

永远：

安装：

某一个 Version。

不是：

Plugin。

---

AI模型：

```text
Llama3

↓

Q4

↓

Q5

↓

Q8
```

其实：

也是：

Version。

---

Logo：

```text
Logo

↓

2024

↓

2025

↓

2026
```

也是：

Version。

---

所以：

真正需要：

Version Runtime。

---

# 二、整体架构

```text
                 Resource Runtime

                        │

                 Version Runtime

                        │

        ┌───────────────┴──────────────┐

 Version Manager

 Compare Engine

 Publish Engine

 Rollback Engine

                        │

                Metadata Runtime

                        │

                Storage Runtime
```

以后：

Resource：

永远：

稳定。

Version：

不断：

增加。

---

# 三、统一模型

新增：

```java
StorageVersion
```

关系：

```text
Resource

↓

Version

↓

Metadata

↓

Driver
```

例如：

```text
Plugin

↓

v1.0

↓

plugin.zip

↓

Driver
```

Resource：

一个。

Version：

很多。

---

# 四、Version 生命周期

建议：

统一：

```text
Draft

↓

Uploaded

↓

Validated

↓

Published

↓

Deprecated

↓

Archived

↓

Deleted
```

解释：

Draft：

还没发布。

Uploaded：

文件上传。

Validated：

校验完成。

Published：

正式。

Deprecated：

不推荐。

Archived：

归档。

---

# 五、版本号

统一：

```java
VersionNumber
```

支持：

```text
1

1.0

1.0.1

2.0.0

2026.07

latest
```

不要：

限定：

SemVer。

很多：

模板：

不用。

---

# 六、数据库设计

## storage_version

建议：

```sql
CREATE TABLE storage_version
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    version_uuid VARCHAR(64) UNIQUE,

    resource_uuid VARCHAR(64),

    metadata_uuid VARCHAR(64),

    version_name VARCHAR(64),

    version_code BIGINT,

    status VARCHAR(32),

    published BOOLEAN,

    latest BOOLEAN,

    checksum VARCHAR(64),

    create_time DATETIME,

    publish_time DATETIME
);
```

注意：

Metadata：

属于：

Version。

不是：

Resource。

---

## storage_version_history

```sql
CREATE TABLE storage_version_history
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    version_uuid VARCHAR(64),

    action VARCHAR(32),

    operator_id VARCHAR(64),

    remark TEXT,

    create_time DATETIME
);
```

例如：

```text
Publish

Rollback

Archive

Delete
```

全部：

记录。

---

# 七、Latest Pointer

这是：

整个：

Version：

最重要：

设计。

不要：

查询：

```text
最大版本
```

而是：

维护：

```text
Latest
```

例如：

```text
Resource

↓

Latest

↓

v3
```

以后：

下载：

```text
/latest
```

直接：

找到。

---

# 八、Compare Engine

新增：

Compare。

例如：

图片：

比较：

```text
Width

Height
```

模板：

比较：

```text
Hash
```

插件：

比较：

```text
Manifest
```

AI模型：

比较：

```text
Parameter
```

以后：

不同：

Type：

不同：

Compare。

---

# 九、Rollback

统一：

流程：

```text
Current

↓

Select Version

↓

Switch Latest

↓

Done
```

注意：

不要：

复制。

只是：

切换：

Latest。

效率：

最高。

---

# 十、发布（Publish）

统一：

```text
Draft

↓

Publish

↓

Latest

↓

Old Version

↓

History
```

整个：

发布：

不用：

删除。

---

# 十一、API

创建版本：

```http
POST /resources/{uuid}/versions
```

获取：

版本：

```http
GET /resources/{uuid}/versions
```

详情：

```http
GET /versions/{uuid}
```

发布：

```http
POST /versions/{uuid}/publish
```

回滚：

```http
POST /versions/{uuid}/rollback
```

比较：

```http
GET /versions/{v1}/{v2}/compare
```

Latest：

```http
GET /resources/{uuid}/latest
```

---

# 十二、前端 UX

资源详情：

新增：

Version。

```text
┌─────────────────────────────────────────────┐

Resource

━━━━━━━━━━━━━━━━━━━━

Versions

━━━━━━━━━━━━━━━━━━━━

v2.0

Published

━━━━━━━━━━━━━━━━━━━━

v1.2

Deprecated

━━━━━━━━━━━━━━━━━━━━

v1.1

Archived

━━━━━━━━━━━━━━━━━━━━

```

点击：

Version：

详情。

---

Version：

详情：

```text
Version

状态

发布日期

Hash

Metadata

Download

Compare

Rollback

Publish
```

---

# 十三、交互设计（重点）

建议采用 **Git 风格** 的版本时间线，而不是普通表格。

例如：

```text
Version Timeline

● v2.0 (Latest)
│
├── Published
│
● v1.2
│
├── Deprecated
│
● v1.1
│
├── Archived
│
● v1.0
```

用户：

一眼：

知道：

资源：

如何：

演进。

---

点击：

两个版本：

支持：

Compare。

例如：

```text
v1.1

↓

v2.0

━━━━━━━━━━━━━━

Hash

不同

Size

+2MB

Metadata

新增：

AI 标签

```

以后：

不同：

ResourceType：

插件：

模板：

图片：

都：

可以：

扩展。

---

发布：

建议：

采用：

```text
Draft

↓

Review（以后）

↓

Publish

↓

Latest
```

MVP：

Review：

可以：

跳过。

企业版：

增加：

审批。

---

# 十四、为什么 Version 不放 Metadata？

很多系统：

会：

```text
Metadata

↓

Version
```

我建议：

反过来。

应该：

```text
Resource

↓

Version

↓

Metadata

↓

Driver
```

为什么？

因为：

真正：

变化：

的是：

文件。

Metadata：

也：

变化。

Version：

应该：

拥有：

Metadata。

Resource：

保持：

稳定。

---

# 十五、P7 核心设计原则（必须坚持）

Version Runtime 建立的是**资源演化模型**，因此必须坚持：

1. **Resource 是稳定身份，Version 是演进历史。Resource UUID 永远不变，Version UUID 每次发布生成。**
2. **Metadata 属于 Version，而不是 Resource。每个版本拥有独立的 Metadata、Hash、存储位置。**
3. **发布（Publish）不是复制资源，而是切换 Latest Pointer。这样回滚只是修改指针，成本极低。**
4. **Version 生命周期独立于 Resource 生命周期。一个 Resource 可以长期存在，同时不断产生新 Version。**
5. **Compare、Rollback、History、Publish 全部围绕 Version 建立，不允许业务自行实现版本管理。**

---

# 十六、建议增加 Version Alias（版本别名）⭐⭐⭐⭐☆

这是很多成熟平台（Docker、Git、Maven、Helm）都会有的能力。

除了：

```text
v1.0

v1.1

v2.0
```

建议：

支持：

```text
latest

stable

beta

preview

lts
```

数据库：

```sql
storage_version_alias
```

例如：

```text
latest

↓

v2.0

stable

↓

v1.8

beta

↓

v2.1-beta
```

这样：

API：

直接：

```http
GET

/resources/plugin/latest
```

或者：

```http
GET

/resources/plugin/stable
```

无需：

解析：

版本号。

---

# 整个 Version Runtime 完成后的架构

```text
StorageResource
        │
        ▼
StorageVersion
        │
        ▼
StorageMetadata
        │
        ▼
StorageReplica
        │
        ▼
StorageProfile
        │
        ▼
StorageDriver
        │
        ▼
Local / Database / MinIO / OSS / S3
```

这也是我认为 **`core-storage` 最稳定的一条主链路**。

从这里开始，后面的 **P8 Lifecycle Runtime**、**P9 Enterprise Resource Platform** 都不再改变这条主链，而是在其上增加治理、生命周期、企业能力、多租户、安全、审计等高级特性。
