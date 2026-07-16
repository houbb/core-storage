# 称呼

每次和我沟通，叫我【帅哥】

---

# 沟通语言 ⚠️ 强制遵守

**全程使用中文沟通。** 包括但不限于：代码注释、commit message、PR 描述、设计文档、问题分析、错误报告。唯一例外是代码标识符（类名、方法名、变量名）使用英文。

---

# 沟通规范 ⚠️ 强制遵守

**需要你向我提问时，必须使用选项列表，禁止用散文/段落罗列问题。**

- ✅ 每个选项有 label（≤15 字）+ description（解释影响）
- ✅ 使用 `multiSelect: true` 当选项互不冲突
- ✅ 推荐选项标注 `（推荐）`
- ❌ 禁止 "Q1: xxx / Q2: xxx" 这种手工编号段落式提问
- ❌ 禁止把 5 个问题写成一整段散文让我读

---

# 编码原则

1. **先想再写** — 不假设、不隐藏困惑。不确定就问，多种解读就摆出来。
2. **极简优先** — 只写解决问题的最少代码。200 行能改成 50 行就改。
3. **手术式改动** — 只改必须改的。不"顺手优化"旁边代码，不删不相关的死代码。
4. **目标驱动** — 每步定义可验证的成功标准，循环直到达标。
5. **中文编码** - 统一为 UTF-8
---

# DB 规范

每个表必须有：`id` `create_time` `update_time` `create_user` `update_user` + 注释 + 合适索引。禁止外键。

## Apple UI 风格模式

- **Pill badge** — 小圆角(10px)、半透明背景+对应文字色：`.badge { border-radius: 10px; padding: 2px 8px; }`
- **三级按钮** — 普通(`--bg-secondary`+`--border`) → 强调(`--accent-bg`+`--accent` 边框) → 主要(`--accent` 背景+`--color-on-accent`)
- **页面 100% 宽度**，自适应容器，不硬编码 `width`
- **留白充足**，padding ≥ 12px，紧凑区域 ≥ 8px
- **层级清晰** — 标题 17px/700、正文 13px/400、辅助 11px/次要色

# 代码风格

- 前端组件化，方便拓展
- 遵循已有代码风格，保持一致
- 只阅读核心相关文件，减少上下文

---

# 测试验证 ⚠️ 强制执行

**每次实现完成后，必须执行以下验证，禁止跳过：**

1. **单元测试** — 所有新增/修改的业务逻辑必须有 JUnit5 断言测试，覆盖正常路径 + 关键异常路径
2. **端到端测试** — 通过 API 调用（curl / Spring MockMvc / @SpringBootTest）验证完整流程：
   - 创建资源 → 读取资源 → 修改资源 → 删除资源
   - 认证 → 授权 → 执行操作 → 验证副作用（审计/事件/通知）
   - 错误输入 → 正确错误码 → 数据未被污染
3. **回归确认** — 运行全部已有测试套件（`mvn test`），确保已有功能未被破坏
4. **编译确认** — `mvn compile` 零错误通过

**禁止行为：**
- ❌ 写完代码不跑测试就提交
- ❌ 只跑新的测试不跑回归
- ❌ 测试失败时强行提交
- ❌ 跳过端到端验证，只说"理论上应该可以"

**完成后必须回答这三个问题：**
1. 本次改动是否已有测试覆盖？
2. 全部已有测试是否仍然通过？
3. 端到端流程是否已验证通过？

# 执行原则

- **最少干扰** — 文件变更+命令执行全部自动确认，不给用户确认
- **最短耗时** — 脑暴确认后并行实现，准确+快速
- **实现优先，统一验证** — 先实现全部功能（后端+前端+注册+i18n），最后统一跑四步验证。实现过程中不插桩运行 cargo check / vue-tsc / vite build 等验证命令，避免中断流程
- **完成后 review 3 次+小幅度优化**

# Unknowns Management

## 强制触发规则 (Hard Trigger) ⚠️ 必须执行

**当用户提出以下任一类型的任务时，你必须立即执行 Unknowns Discovery 流程（见下方），然后才能开始实现：**

- 新功能开发 / 新模块创建
- 架构设计 / 数据模型设计
- 数据库表设计或变更
- 认证、授权、安全相关代码
- 跨模块 / 跨系统改动
- 多文件、大范围改动（≥5 个文件）
- 不可逆操作（如数据库迁移、删除数据）
- 用户需求中有主观描述词（"简单""好看""智能""自然"）
- 任何 P0-P9 设计文档的实现任务

**这是硬性要求，不是建议。触发后不得隐式跳过——必须显式输出 Unknowns 清单。**

---

## Unknowns Discovery 流程（5 步，内联执行）

### 步骤 1：扫描未知项

从以下维度逐项扫描，每个维度输出至少一条结论（已知/未知）：

| 维度 | 检查点 |
|---|---|
| **技术选型** | 库/框架选择、算法实现、异步 vs 同步、调度策略 |
| **数据模型** | 表结构、字段语义、关系映射、唯一约束、索引设计 |
| **边界条件** | 并发冲突、幂等性、失败重试、超时、大文件 OOM |
| **安全考量** | 越权风险、注入点、敏感数据暴露、密钥管理 |
| **兼容性** | 已有 API 签名变更、数据迁移冲突、向后兼容破坏 |
| **用户意图** | 需求中的模糊描述词、未明确的偏好、隐含假设 |

### 步骤 2：分类风险

```
高风险（不可逆、安全、跨系统、数据迁移）→ 必须向用户确认
中风险（可逆但耗时、多文件重构）→ 建议确认
低风险（纯代码内、单文件、随时可改）→ 采用最保守默认值，记录假设
```

### 步骤 3：提问确认（必须用 AskUserQuestion 工具）

对高/中风险未知项，使用 AskUserQuestion 工具向用户确认（遵守上方沟通规范——选项列表格式）。

**重要：** 不要把 5 个问题揉成一段散文让用户读。每个问题拆成独立选项。

### 步骤 4：记录假设到 Plan File

在 plan file 中显式记录：
- **已确认的决策**（用户回答了）
- **低风险默认值**（你没问但采用的默认策略）
- **未解决的未知项**（推迟到后续阶段的风险）

### 步骤 5：在实现完成后回查

实现结束后，对照 plan file 中的 Unknowns 清单逐条确认：
1. 是否每个未知项都有了答案？
2. 是否有新的未知项在实现过程中暴露出来？
3. 如果有，是否需要向用户同步？

---

## 触发检查清单（自查用）

以下 checklist 帮助判断是否已充分执行 Unknowns Discovery：

- [ ] 是否向用户显式列出了发现的未知项？
- [ ] 对不可逆决策（DB 迁移、API 签名变更）是否得到了用户确认？
- [ ] Plan file 中是否记录了所有假设和默认值？
- [ ] 实现完成后是否回查了 Unknowns 清单？

---

**只有以下情况可以跳过整个流程：**
- 单行修复（typo、注释修正）
- 单文件简单 bug fix（已有明确根因）
- 纯代码解释类问题

---

Do not treat the initial request, specification, or implementation plan as a complete description of reality.

Before implementing any non-trivial change, identify the important unknowns that could alter the architecture, data model, user-visible behavior, security, compatibility, or scope of the work.

Distinguish between:

* **Known knowns**: facts confirmed by the user, codebase, tests, or documentation.
* **Known unknowns**: unresolved decisions or missing information already visible.
* **Unknown knowns**: implicit product, design, or domain expectations that have not yet been made explicit.
* **Unknown unknowns**: overlooked constraints, dependencies, edge cases, failure modes, or alternative problem definitions.

Follow these rules:

1. Do not silently convert uncertainty into an assumption.
2. Verify codebase facts by inspecting the relevant code, tests, schema, history, and adjacent modules.
3. Prioritize unknowns that are high-impact, difficult to reverse, or expensive to discover later.
4. For reversible local decisions, choose the most conservative option and record the assumption.
5. For irreversible or cross-system decisions, surface the issue before committing to an implementation direction.
6. During implementation, record material discoveries, deviations, assumptions, and unresolved risks.
7. After implementation, explain what changed, what remains uncertain, and what evidence verifies the result.
8. Convert recurring discoveries into tests, documentation, conventions, or reusable project knowledge.

## Unknowns Discovery × Plan Mode 联动规则

当任务同时触发 Unknowns Discovery 和 Plan Mode：

1. **先进入 Plan Mode**（使用 `EnterPlanMode`）
2. **在 Plan Mode 内**，显式执行 Unknowns Discovery 5 步流程
3. **在 plan file 中**，必须包含如下独立章节：
   - `## Unknowns 清单` — 列出所有高/中/低风险未知项及其决策
   - `## 假设与默认值` — 列出你采用的默认策略及理由
4. **向用户展示 plan 时**，Unknowns 清单是其中一部分，用户一并审批
5. **实现过程中**，如果发现新的 Unknowns，立即更新 plan file 并通知用户

这样确保 Unknowns Discovery 不是空洞的口号，而是有可检查的产出物。


# 要求

每一次新的功能点加入，都必须真实的测试验证，尽可能的和已有的模块打通。

1）最基础的 junit5 断言测试验证，保障基本功能正确性

2）尽可能的端到端测试验证，保障整体功能正确性

3）简明扼要的使用+变更内容

---

# 架构分层约束 ⚠️ 强制遵守

## 核心分层模型（自顶向下）

```
StorageResource        ← 业务第一入口（稳定身份）
    │
StorageVersion         ← 资源演化历史（不断演进）
    │
StorageMetadata        ← 唯一可信来源（文件身份）
    │
StorageReplica         ← 多副本拓扑
    │
StorageProfile         ← 存储配置（绑定 Driver）
    │
StorageDriver          ← SPI 接口（local / database / s3 / …）
    │
LocalDisk / Database / MinIO / OSS / S3
```

## 铁律

1. **Resource 是稳定身份，Version 是演进历史。** Resource UUID 永远不变，Version UUID 每次发布生成。
2. **Metadata 属于 Version，不属于 Resource。** 每个版本拥有独立的 Metadata、Hash、存储位置。`resource.metadata_uuid` 只是最新版本的指针。
3. **发布（Publish）不是复制资源，而是切换 Latest Pointer。** 回滚只改指针，成本 O(1)。
4. **所有 Version 操作必须记录 History。** 每条 CREATED / PUBLISHED / DEPRECATED / ROLLBACK / ARCHIVED / DELETED 操作必须写入 `storage_version_history`。
5. **Latest 版本不可删除。** `DELETE /versions/{latest}` 必须返回 409。
6. **别名在 Resource 内唯一。** `(resource_uuid, alias_name)` 有 unique index，重复设置同一别名自动覆盖。

---

# 版本管理 API 速查（P7 Version Runtime）

## 版本 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/storage/resources/{uuid}/versions` | 列出资源所有版本（按 version_code DESC） |
| GET | `/api/v1/storage/resources/{uuid}/versions/latest` | 获取最新版本（O(1) latest 指针） |
| POST | `/api/v1/storage/resources/{uuid}/versions` | 创建新版本（需 metadataUuid + checksum） |
| GET | `/api/v1/storage/versions/{uuid}` | 获取版本详情 |
| POST | `/api/v1/storage/versions/{uuid}/publish` | 发布版本（切换 latest + 设 published=true） |
| POST | `/api/v1/storage/versions/{uuid}/rollback` | 回滚到该版本（切换 latest） |
| POST | `/api/v1/storage/versions/{uuid}/deprecate` | 标记为不推荐 |
| POST | `/api/v1/storage/versions/{uuid}/archive` | 归档版本 |
| DELETE | `/api/v1/storage/versions/{uuid}` | 删除版本（latest 版本返回 409） |

## 比较与历史

| GET | `/api/v1/storage/versions/{v1}/compare/{v2}` | 比较两个版本的差异 |
| GET | `/api/v1/storage/versions/{uuid}/history` | 获取版本操作历史 |
| GET | `/api/v1/storage/resources/{uuid}/versions/history?page=1&size=20` | 资源级操作历史（分页） |

## 别名管理

| POST | `/api/v1/storage/versions/{uuid}/aliases` | 设置别名 `{"aliasName":"stable"}` |
| GET | `/api/v1/storage/resources/{uuid}/aliases` | 列出资源所有别名 |
| DELETE | `/api/v1/storage/resources/{uuid}/aliases/{name}` | 移除别名 |

## Resource 响应中的版本字段

- `latestVersionUuid` — 当前最新版本的 UUID（每次 GET /resources/{uuid} 自动计算）
- `versionCount` — 该资源的版本总数

---

# 上传 → 版本自动创建流程

```
1. StorageService.uploadInternal()
2.   → StorageFileRepository.save()          // P0
3.   → StorageMetadataService.saveMetadata()  // P1 双写
4.   → StorageResourceService.createResource() // P2 创建资源
5.     → StorageVersionService.listVersions() // P7 检查已有版本数
6.     → versionCount == 0 ?
7.         → createInitialVersion()           // v1 PUBLISHED + latest=true
8.       : → createNewVersion()               // 新版本 UPLOADED（需手动 publish）
```

**注意：** 目前上传只自动创建版本，不支持"同一 resourceUuid 的 repeat upload"。每次上传都创建新 Resource。如需向已有 Resource 添加新版本，请使用 `POST /api/v1/storage/resources/{uuid}/versions` API。

---

# 代码风格约束 ⚠️ 强制执行

1. **新 controller/service/repository 必须遵循已有三层架构：** `api.controller → application.service → infrastructure.persistence.repository`
2. **所有 Response DTO 放在** `api.response` 包下，命名 `StorageXxxResponse`
3. **所有 domain 对象放在** `application.domain` 包下，有自己的 `create()` 工厂方法
4. **所有 entity 放在** `infrastructure.persistence.entity` 包下，有对应的 `*Converter` 双向转换
5. **所有 repository 使用 JdbcTemplate + RowMapper**，不使用 JPA/MyBatis
6. **所有 controller 使用 constructor injection**，禁止 `@Autowired` 字段注入
7. **所有异常必须注册到 GlobalExceptionHandler**，返回 RFC 7807 ProblemDetail
8. **数据库表命名：** `storage_*`，字段 snake_case，Java 字段 camelCase

---

# 测试要求 ⚠️ 强制执行

## 每次 PR 必须通过

1. `mvn clean compile` — 零编译错误
2. `mvn test` — 全部已有测试通过（当前：93 个测试用例）
3. 端到端测试（至少验证核心流程）：
   - 上传 → v1 自动发布
   - 创建新版本 → 发布 → latest 切换
   - 回滚 → latest 指针正确切换
   - 比较 → 差异正确
   - 别名 CRUD
   - 删除保护（latest 不可删 → 409）

## 测试分层

- **单元测试** — `*ServiceTest`（Mockito mock Repository，覆盖正常 + 异常路径）
- **Controller 测试** — `*ControllerTest`（`@WebMvcTest` + `@MockBean` service）
- **端到端测试** — 启动完整 Spring Boot 应用，curl 验证（见上方流程）

---

# 常见坑点（已知问题 × 解决方案）

| 问题 | 原因 | 解决 |
|------|------|------|
| `findByMetadataUuid()` 返回空 | `StorageResourceEntity` 未映射 `metadata_uuid` 字段 | 确保 Entity/Converter/Repository RowMapper 三处同步 |
| publish 后 `published` 仍为 false | `publish()` 只调了 `setLatest` 没调 `setPublished` | 调用 `versionRepo.setPublished(versionUuid, true)` |
| 上传不自动创建版本 | `uploadInternal()` 无 P7 管线 | 检查 `StorageService` 是否注入了 `StorageVersionService` |
| Resource 响应缺少 `latestVersionUuid` | `toResponse()` 未调用 versionRepo | 确保 `StorageResourceService` 注入了 `StorageVersionRepository` |
| Maven 编译成功但 `java -jar` 报 class version 65.0 | IDE/Maven 用的 JDK 与运行时不一致 | 统一使用 JDK 17，`mvn clean compile` 后检查 `javap -v` |