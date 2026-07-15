我认为 **P8 Lifecycle Runtime** 是整个 `core-storage` 的**企业级分水岭**。

前面的 P0~P7 都是在回答：

> **资源怎么存？怎么访问？怎么演化？**

而 P8 开始回答另一个问题：

> **资源什么时候该留下？什么时候该删除？什么时候该归档？**

这是所有企业最终都会遇到的问题。

随着时间推移：

* 文件越来越多
* 备份越来越大
* AI 模型越来越重
* 导出文件越来越多
* 临时文件没人删
* 成本越来越高

如果没有生命周期，最后 Storage 一定会失控。

所以：

**Lifecycle Runtime 本质不是"自动删除文件"，而是 Resource Governance（资源治理）。**

---

# Phase 8：Lifecycle Runtime ⭐⭐⭐⭐⭐

> **目标：建立统一资源生命周期治理平台，根据策略自动完成资源保留、归档、迁移、冻结、删除等操作。**

一句话：

> **资源有生命周期，不是永久存在。**

以后：

```text
Create

↓

Active

↓

Warm

↓

Cold

↓

Archive

↓

Delete
```

而不是：

```text
Upload

↓

永久存在
```

---

# 一、为什么需要 Lifecycle？

例如：

用户头像：

```text
永久保存
```

导出 Excel：

```text
7 天
```

日志附件：

```text
30 天
```

临时图片：

```text
24 小时
```

插件：

```text
永久
```

AI 模型：

```text
长期保存
```

数据库备份：

```text
保留 180 天
```

如果：

全部：

人工：

管理。

最终：

没人：

敢删。

---

# 二、整体架构

```text
                Resource Runtime

                       │

              Lifecycle Runtime

                       │

      ┌────────────────┼────────────────┐

 Policy Engine

 Scheduler

 Lifecycle Executor

 Retention Engine

                       │

                Storage Runtime
```

Lifecycle：

不是：

定时任务。

它：

是：

规则引擎。

---

# 三、统一生命周期模型

新增：

```java
ResourceLifecycle
```

关系：

```text
Resource

↓

Lifecycle Policy

↓

Current Stage

↓

Executor
```

整个：

生命周期：

不依赖：

Driver。

---

# 四、生命周期阶段

建议：

统一：

```java
LifecycleStage
```

```text
ACTIVE

WARM

COLD

ARCHIVED

DELETED
```

解释：

ACTIVE

正常使用。

---

WARM

较少访问。

---

COLD

几乎不用。

---

ARCHIVED

归档。

不能：

直接：

修改。

---

DELETED

等待：

物理删除。

---

不要：

直接：

删。

---

# 五、生命周期策略

新增：

```java
LifecyclePolicy
```

例如：

```text
Avatar

↓

永久
```

Export：

```text
7 天
```

Temp：

```text
24 小时
```

Backup：

```text
180 天
```

Plugin：

```text
永久
```

AI：

```text
5 年
```

策略：

绑定：

Category。

不是：

Driver。

---

# 六、数据库设计

## storage_lifecycle_policy

```sql
CREATE TABLE storage_lifecycle_policy
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    policy_name VARCHAR(64),

    resource_type VARCHAR(64),

    category VARCHAR(64),

    active_days INT,

    warm_days INT,

    cold_days INT,

    archive_days INT,

    delete_days INT,

    enabled BOOLEAN,

    create_time DATETIME
);
```

例如：

```text
Temp

↓

1

↓

Delete
```

---

## storage_lifecycle_task

```sql
CREATE TABLE storage_lifecycle_task
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    resource_uuid VARCHAR(64),

    task_type VARCHAR(32),

    status VARCHAR(32),

    execute_time DATETIME,

    finish_time DATETIME,

    error_message TEXT
);
```

Lifecycle：

全部：

Task。

---

# 七、Lifecycle Action

建议：

统一：

```java
LifecycleAction
```

```text
NOTHING

MOVE

ARCHIVE

FREEZE

DELETE

VERIFY
```

以后：

新增：

不用：

改：

架构。

---

# 八、执行流程

例如：

```text
Scheduler

↓

Find Resource

↓

Policy Match

↓

Lifecycle Action

↓

Task

↓

Executor

↓

Update Stage
```

以后：

支持：

暂停：

恢复。

---

# 九、自动归档

例如：

```text
180 天

↓

Archive

↓

Storage Profile

↓

Archive
```

Profile：

可以：

切换：

```text
Hot

↓

Cold
```

不用：

业务：

知道。

---

# 十、自动删除

删除：

建议：

两阶段。

第一步：

```text
Soft Delete
```

第二步：

```text
Grace Period
```

例如：

```text
7 天
```

之后：

```text
Physical Delete
```

这样：

管理员：

还能：

恢复。

---

# 十一、自动迁移

例如：

```text
Hot

↓

Warm

↓

Cold
```

对应：

Storage：

```text
SSD

↓

NAS

↓

OSS Archive
```

以后：

Driver：

不用：

改。

---

# 十二、引用保护（Reference Protection）

这是：

Lifecycle：

最重要：

设计。

删除：

先查：

```text
Reference Count
```

如果：

```text
>0
```

禁止：

删除。

例如：

Logo：

被：

100 个页面：

引用。

Lifecycle：

不会：

删。

---

# 十三、API

生命周期：

```http
GET /resources/{uuid}/lifecycle
```

策略：

```http
GET /lifecycle/policies
```

新增：

```http
POST /lifecycle/policies
```

执行：

```http
POST /resources/{uuid}/lifecycle/run
```

归档：

```http
POST /resources/{uuid}/archive
```

恢复：

```http
POST /resources/{uuid}/restore
```

删除：

```http
DELETE /resources/{uuid}
```

这里只：

进入：

Lifecycle。

不是：

立即：

删。

---

# 十四、前端 UX

新增：

Lifecycle。

资源：

详情：

```text
Lifecycle

━━━━━━━━━━━━━━

Stage

ACTIVE

━━━━━━━━━━━━━━

Policy

Export-30Days

━━━━━━━━━━━━━━

Retention

30 Days

━━━━━━━━━━━━━━

Next Action

Delete

2026-09-01
```

用户：

知道：

什么时候：

删除。

---

策略：

页面：

```text
Policy

Export

↓

30 Days

↓

Delete

━━━━━━━━━━━━━━

Temp

↓

1 Day

↓

Delete

━━━━━━━━━━━━━━

Backup

↓

365 Days

↓

Archive
```

管理员：

修改：

即可。

---

# 十五、交互设计（重点）

建议增加一个 **Lifecycle Dashboard**。

例如：

```text
┌─────────────────────────────────────────────┐

Lifecycle Dashboard

─────────────────────────────────────────────

ACTIVE

12,840

WARM

3,291

COLD

2,193

ARCHIVED

18,340

PENDING DELETE

142

─────────────────────────────────────────────
```

点击：

进入：

资源。

---

策略：

建议：

Timeline。

```text
ACTIVE

↓

30 Days

↓

WARM

↓

180 Days

↓

ARCHIVED

↓

365 Days

↓

DELETE
```

管理员：

拖动：

时间。

即可：

修改：

策略。

---

资源详情：

增加：

```text
History

↓

Lifecycle

↓

Version

↓

Replica

```

完整：

资源：

生命周期。

---

# 十六、为什么 Lifecycle 不直接删除？

很多：

Storage：

都是：

```text
Delete

↓

Done
```

这是：

最大的：

错误。

企业：

一定：

需要：

```text
Delete

↓

Recycle

↓

Grace Period

↓

Verify

↓

Physical Delete
```

否则：

误删：

无法：

恢复。

---

# 十七、P8 核心设计原则（必须坚持）

Lifecycle Runtime 建立的是**资源治理体系**，而不是定时清理任务，因此建议坚持：

1. **删除不是一个动作，而是一段生命周期。所有删除都先进入 Lifecycle，再决定是否物理删除。**
2. **生命周期由 Policy 驱动，而不是业务代码驱动。不同资源类型可以拥有不同保留策略。**
3. **Lifecycle 与 Storage 解耦，只决定资源状态，不直接依赖具体 Driver。真正的迁移、归档、删除由 Runtime 调用 Driver 完成。**
4. **任何生命周期操作都必须经过 Reference 检查，仍被引用的资源不得删除或归档。**
5. **所有生命周期操作采用 Task 模型执行，保证可暂停、可恢复、可重试，并与未来企业级调度能力兼容。**

---

# 我建议在 P8 再增加一个企业级能力：Legal Hold（法律保留）⭐⭐⭐⭐⭐

这是很多企业对象存储（S3 Object Lock、企业 ECM、金融档案系统）都会提供的能力。

新增：

```text
Resource

↓

Legal Hold

↓

Lifecycle Disabled
```

数据库：

```sql
storage_resource_hold
```

例如：

```text
resource_uuid

hold_type

LEGAL

AUDIT

INVESTIGATION

expire_time
```

作用：

* 审计期间禁止删除
* 法务保全
* 合规要求（金融、医疗等）
* 人工冻结资源

当资源处于 **Legal Hold** 状态时：

```text
Lifecycle

×

Delete

×

Archive

×

Move
```

直到解除 Hold。

---

## P8 完成后的整体架构

```text
StorageResource
        │
StorageVersion
        │
StorageLifecycle
        │
StorageMetadata
        │
StorageReplica
        │
StorageProfile
        │
StorageDriver
        │
Local / Database / MinIO / OSS / S3
```

可以看到，**Lifecycle 已经成为 Resource 的一个核心维度**，与 Version、Replica 同级，而不是一个独立的清理模块。这样 `core-storage` 就具备了完整的资源治理能力，为最后的 **P9 Enterprise Resource Platform**（多租户、审计、配额、加密、跨区域、内容安全等企业能力）奠定了稳定的架构基础。
