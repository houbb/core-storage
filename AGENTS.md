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