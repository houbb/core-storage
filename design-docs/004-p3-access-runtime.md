我建议 **P3 不要叫 Access Runtime，而是叫 Unified Access Runtime（统一访问运行时）**。

原因是从这一阶段开始，`core-storage` 不再负责**存储资源**，而是开始负责**控制资源如何被访问**。

这是 Storage 从"仓库"走向"平台"的第一步。

---

# Phase 3：Unified Access Runtime ⭐⭐⭐⭐⭐

> **目标：建立统一资源访问层（Unified Resource Access），所有资源访问必须经过 Access Runtime，而不是直接访问磁盘或对象存储。**

一句话：

> **Storage 负责保存资源，Access 负责控制资源如何被访问。**

以后整个系统：

```text
Browser

↓

Access Runtime

↓

Permission

↓

Storage Driver

↓

Disk / Database / S3
```

而不是：

```text
Browser

↓

/storage/avatar.png
```

---

# 一、为什么需要 Access Runtime？

P2 已经有：

```
Resource
```

但是：

资源：

```text
Avatar

Plugin

Template

Backup

AI Model
```

访问规则：

完全不同。

例如：

头像：

```
公开访问
```

插件：

```
登录即可下载
```

模板：

```
管理员
```

AI模型：

```
内部系统
```

备份：

```
禁止普通用户下载
```

如果：

全部：

```java
if(role.equals("ADMIN"))
```

以后：

整个项目：

到处都是权限判断。

所以：

统一：

```
Access Runtime
```

---

# 二、整体架构

```text
                     Browser

                        │

              Resource URL

                        │

                Access Runtime

        ┌──────────────┴──────────────┐

 Permission Engine            URL Resolver

        │                              │

 Resource Repository          Metadata Runtime

        │                              │

        └──────────────┬──────────────┘

                       │

                Storage Driver
```

注意：

以后：

浏览器：

不知道：

Storage Driver。

只能：

经过：

Access。

---

# 三、统一访问模型

新增：

```java
ResourceAccess
```

建议：

```text
ResourceAccess

↓

Resource

↓

Policy

↓

Permission

↓

Driver
```

以后：

下载：

不是：

```text
Driver.download()
```

而是：

```text
AccessService.download()
```

---

# 四、访问模式（Access Mode）

统一：

```java
enum AccessMode
```

建议：

```text
PUBLIC

LOGIN

PRIVATE

ROLE

OWNER

TOKEN

SIGNED_URL

SYSTEM
```

解释：

| 模式         | 说明       |
| ---------- | -------- |
| PUBLIC     | 所有人可访问   |
| LOGIN      | 登录即可     |
| OWNER      | 资源拥有者    |
| ROLE       | 指定角色     |
| TOKEN      | 临时 Token |
| SIGNED_URL | 签名 URL   |
| SYSTEM     | 系统内部     |

以后：

新增：

ACL：

不用：

改数据库。

---

# 五、Resource Policy

新增：

```java
ResourcePolicy
```

例如：

```text
Resource

↓

Policy
```

Policy：

例如：

```text
Avatar

↓

PUBLIC
```

插件：

```text
Plugin

↓

LOGIN
```

备份：

```text
Backup

↓

ADMIN
```

AI模型：

```text
Model

↓

SYSTEM
```

以后：

Policy：

可配置。

不是：

写死。

---

# 六、数据库设计

## storage_access_policy

建议：

```sql
CREATE TABLE storage_access_policy
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,

    resource_uuid       VARCHAR(64),

    access_mode         VARCHAR(32),

    role_name           VARCHAR(64),

    allow_download      BOOLEAN,

    allow_preview       BOOLEAN,

    allow_delete        BOOLEAN,

    allow_share         BOOLEAN,

    expire_time         DATETIME,

    create_time         DATETIME
);
```

以后：

一个资源：

多个策略。

例如：

```
管理员

↓

下载
```

普通用户：

```
只能：

预览
```

---

# 七、签名URL（Signed URL）

企业以后：

一定需要。

例如：

```
https://xxx

?

expires=xxx

signature=xxx
```

Access Runtime：

负责：

生成。

Storage：

不用：

知道。

以后：

S3：

OSS：

MinIO：

全部：

支持。

---

# 八、下载流程

统一：

```text
GET /resource/{uuid}

↓

Access Runtime

↓

查询 Resource

↓

查询 Policy

↓

Permission

↓

Storage Driver

↓

InputStream

↓

Response
```

注意：

Driver：

不知道：

权限。

Driver：

只负责：

读。

---

# 九、预览（Preview）

新增：

```
Preview
```

以后：

不是：

所有：

下载。

例如：

图片：

```
Preview
```

PDF：

```
Preview
```

Markdown：

```
Preview
```

视频：

```
Preview
```

下载：

另外：

判断。

---

# 十、分享（Share）

新增：

```
Resource Share
```

以后：

用户：

点击：

```
分享
```

生成：

```
Share Link
```

例如：

```
https://...

token=xxxx
```

支持：

```
24小时

7天

永久
```

以后：

插件市场：

直接：

复用。

---

# 十一、API

统一入口：

```
GET /resource/{uuid}
```

下载：

```
GET /resource/{uuid}/download
```

预览：

```
GET /resource/{uuid}/preview
```

分享：

```
POST /resource/{uuid}/share
```

签名：

```
POST /resource/{uuid}/signed-url
```

Policy：

```
GET /resource/{uuid}/policy
```

修改：

```
PUT /resource/{uuid}/policy
```

---

# 十二、前端 UX

资源详情：

新增：

```
┌──────────────────────────────────────┐

资源详情

━━━━━━━━━━━━━━━━━━━━━━━━━━

名称

类型

Owner

Visibility

──────────────────────────

访问控制

○ Public

○ Login

○ Owner

○ Role

○ Signed URL

──────────────────────────

权限：

☑ 下载

☑ 预览

☐ 删除

☑ 分享

──────────────────────────

分享：

复制链接

生成签名URL

设置过期时间

└──────────────────────────┘
```

---

资源列表：

增加：

```
🌍 Public

🔒 Private

👤 Owner

🕒 Signed

```

用户：

一眼：

知道。

---

# 十三、交互设计

资源详情页新增一个独立的 **Access** 标签页，而不是把权限塞到基础信息里。

```text
┌─────────────────────────────────────────────┐
Basic | Metadata | References | Access | History

─────────────────────────────────────────────

访问方式：

(●) Public

( ) Login

( ) Owner

( ) Role

( ) Signed URL

─────────────────────────────────────────────

允许操作：

☑ Preview

☑ Download

☐ Delete

☑ Share

─────────────────────────────────────────────

分享链接：

https://...

[复制]

[重新生成]

过期：

24h ▼

─────────────────────────────────────────────
```

同时资源列表增加快捷操作：

```
下载

复制链接

分享

查看权限
```

无需进入详情页即可完成常见操作。

---

# 十四、为什么 P3 不做 ACL？

很多系统一开始就设计：

```
ACL

User

Group

Role

Policy

```

结果：

整个系统：

复杂十倍。

实际上：

MVP：

90%的资源：

只有：

```
Public

Login

Owner

Admin
```

真正：

ACL：

企业：

以后：

P9：

再做。

所以：

P3：

只做：

Policy。

不做：

ACL Engine。

---

# 十五、P3 核心设计原则（必须坚持）

这一阶段真正要建立的是**统一资源访问入口**，而不是权限系统本身。

因此必须坚持以下原则：

1. **所有资源访问必须经过 Access Runtime，业务禁止绕过 Access 直接访问 Storage Driver。**
2. **Storage Driver 只负责读写字节，不负责权限、分享、签名、下载控制等业务逻辑。**
3. **访问策略（Policy）与资源（Resource）解耦，任何资源都可以独立配置访问方式。**
4. **统一使用 Access API（Download、Preview、Share、Signed URL）作为对外接口，未来切换到 OSS、S3、CDN 时业务接口保持不变。**
5. **Policy 优先于具体实现，先建立统一策略模型，再逐步演进到企业级 ACL、ABAC 或 RBAC 引擎。**

---

# 我建议对 P3 再增加一个 Runtime：Access Log Runtime ⭐⭐⭐⭐☆

这是很多对象存储平台很早就会加入的能力。

增加：

```sql
storage_access_log
```

例如：

```sql
id
resource_uuid
access_type      -- PREVIEW / DOWNLOAD / SHARE
operator_id
client_ip
user_agent
result           -- SUCCESS / DENIED
access_time
duration_ms
```

作用：

* 下载次数统计
* 热门资源排行
* 分享分析
* 安全审计
* 异常下载检测
* 后续 `core-audit` 无缝集成

这样，**P4 Image Runtime** 就可以专注于图片处理（缩略图、压缩、裁剪、格式转换等），而所有资源访问、安全控制和访问行为统计，都已经在 P3 建立好了统一基础。
