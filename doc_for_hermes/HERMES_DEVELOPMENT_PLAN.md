# Daily Flow — Hermes 开发计划

> 编制者：Hermes Agent (deepseek-v4-pro)  
> 编制日期：2026-06-15  
> 状态：草稿，待用户审批  
> 基于文档：`docs/CODEX_IMPLEMENTATION_PLAN.md` v1.0、`docs/DEVELOPMENT_PROGRESS.md` (2026-06-15)

---

## 1. 出发点

项目当前处于 **P4 阶段中段**。最新提交 `a413a36` 完成了 DF-404（提醒重算与同步），下一步是 **DF-405：任务多提醒迁移**。

本计划严格遵循已有的 `CODEX_IMPLEMENTATION_PLAN.md` 第 10 节任务定义，并按原有优先级推进。所有产品决策、代码来源规则和测试约定保持不变。

---

## 2. 执行策略

### 2.1 代码修改原则

1. **复用优先**：先搜索 My Brain / Track & Graph 已有实现，再写新代码
2. **先测试后实现**：移植或编写测试 → 验证失败 → 实现 → 验证通过
3. **小步提交**：每个 DF-NNN 任务一个独立 commit，附带来源台账
4. **不破坏构建**：每个任务完成后运行模块级测试和 lint
5. **记录来源**：每次提交必须记录 Reuse/New code/Verification

### 2.2 构建与验证

由于开发环境为 Windows + 百度同步盘，构建使用以下约定：
- 使用外部 build root 避免文件锁：`-PdailyFlow.buildRoot="%LOCALAPPDATA%\DailyFlow\build"`
- JVM 单元测试在每次代码修改后运行
- 雷电 Android 9 模拟器用于集成验证（按需）
- 无法在本地运行 Gradle 时，至少保证代码审查和静态分析通过

### 2.3 文档输出

Hermes 创建的文档存放于 `doc_for_hermes/` 目录，不覆盖 `docs/` 下的原有档案：
- 每完成一个阶段，更新 `doc_for_hermes/DEVELOPMENT_LOG.md`（开发日志）
- 发现问题、风险或决策时，记录到 `doc_for_hermes/ISSUES_AND_DECISIONS.md`

---

## 3. 开发阶段与任务

### 阶段 P4：统一多重提醒（剩余）

**目标**：任务、日历事件可配置多个提醒，记录可按模板设置提示；重启后恢复；权限不足时降级。

#### DF-405 改造任务提醒 `[ ]`

- **前置**：DF-404
- **状态**：下一步
- **目标**：
  1. 任务截止时间 (`dueDate`) 与提醒时间分离
  2. 支持为一个任务创建多个绝对或相对提醒
  3. 将旧的单 alarm 行迁移到统一 `reminders` 表
  4. 复用现有任务编辑 UI、通知内容和"完成"动作
- **复用**：
  - DF-404 的 `ScheduleReminder` / `CancelReminder` / `RescheduleTargetReminders`
  - DF-403 的 `Reminder` 领域模型和 repository
  - 现有 `UpsertTaskUseCase` 的 alarm 创建/更新逻辑
- **关键决策**：
  - 迁移时旧 alarm 转换为单条绝对时间 Reminder
  - 任务 UI 新增"添加提醒"按钮，默认显示"准时"快捷选项
  - 旧 `alarms` 表保留但不再新增行
  - 迁移后清空 `Task.alarmId`
- **验收**：
  - 旧任务升级后提醒不丢失且不重复
  - 新任务可配置 0 个或多个提醒
  - 修改截止时间后相对提醒自动重算
  - 完成/删除任务后所有提醒全部取消
  - 雷电 Android 9 验证实际提醒触发和迁移

#### DF-406 改造日历事件提醒 `[ ]`

- **前置**：DF-404
- **目标**：
  1. Calendar Provider 事件 ID 与本地 `Reminder` target 关联
  2. 事件开始时间修改后重新计算相对提醒
  3. 外部删除日历事件时，安全处理悬空 target（转为 `PENDING` → `MISSED`）
- **验收**：无日历权限时不崩溃；权限恢复后可重新调度

#### DF-407 增加记录提示 `[ ]`

- **前置**：DF-404、DF-204
- **能力**：按模板设置固定时间提示（每日/指定星期），点击通知直达快速记录页
- **首版限制**：只支持每日和指定星期；复杂 RRULE 后置
- **验收**：模板停用会取消未来提示

#### DF-408 实现多提醒编辑组件 `[ ]`

- **前置**：DF-405、DF-406
- **能力**：
  - 添加/删除多个提醒
  - 快捷选项：准时、提前 5/15/30 分钟、1 小时、1 天
  - 自定义绝对时间
  - 权限说明和降级提示
- **验收**：UI 阻止重复提醒和过去时间；提醒列表通过 Flow 实时更新

---

### 阶段 P5：DeepSeek 意图识别和确认卡

**目标**：自然语言可生成任务、事件、日记和结构化记录提案，所有写操作在确认后执行。

#### DF-501~509 概述

| 任务 | 内容 | 前置 |
|------|------|------|
| DF-501 | 验证 DeepSeek 提案兼容性 | DF-011, DF-012 |
| DF-502 | 定义 ActionProposal sealed model | DF-501 |
| DF-503 | 将写工具改为"只提案" | DF-502 |
| DF-504 | 新增结构化记录 AI 工具 | DF-108, DF-502 |
| DF-505 | 实现本地日期歧义解析层 | DF-502 |
| DF-506 | 实现确认卡和编辑 | DF-502, DF-505 |
| DF-507 | 实现 ProposalExecutor | DF-503, DF-504, DF-506 |
| DF-508 | AI 数据发送预览和日志脱敏 | DF-501 |
| DF-509 | AI 统计总结 | DF-303, DF-508 |
| DF-012 | Provider 连接和能力测试 | DF-011 |

> **注**：DF-012（Provider 契约测试）在 P0.5 中未完成，需在 P5 前补齐。

---

### 阶段 P6：安全、备份和数据迁移（剩余）

**目标**：API Key 由 Keystore 保护；应用锁可用；tracking 数据进入备份；安全边界有测试。

| 任务 | 内容 | 前置 |
|------|------|------|
| DF-604 | 扩展现有应用锁（生物识别/超时锁定/缩略图遮挡） | DF-004 |
| DF-605 | 扩展 JSON 备份 DTO（含 tracking + reminders） | DF-109 |
| DF-606 | 实现恢复预检和原子恢复 | DF-605 |
| DF-607 | 发布版网络和隐私加固 | DF-501, DF-602 |

> DF-601~603（SecretStore 审计、实现、迁移）已在 P0.5 提前完成。

---

### 阶段 P7：集成、体验和稳定性

**目标**：主要用户流程连贯，无回归，性能和无障碍达标。

| 任务 | 内容 |
|------|------|
| DF-701 | 构建统一"今天"视图 |
| DF-702 | 建立全局搜索 |
| DF-703 | 字符串、主题和无障碍完成 |
| DF-704 | 性能和数据库查询审计 |
| DF-705 | 进程死亡和离线恢复 |
| DF-706 | 建立回归测试套件 |

---

### 阶段 P8：发布、F-Droid 和 v1.1 准备

| 任务 | 内容 |
|------|------|
| DF-801 | 依赖和许可证审计 |
| DF-802 | Release 签名流程 |
| DF-803 | GitHub Release 完善 |
| DF-804 | F-Droid metadata 准备 |
| DF-805 | Beta 验收 |
| DF-806 | v1.1 数据库加密技术验证 |

---

## 4. 建议执行顺序

```text
当前 ──→ DF-405 ──→ DF-406 ──→ DF-407 ──→ DF-408   (完成 P4)
                                              │
                    DF-012 (补齐Provider测试)  │
                                              │
                    ┌─────────────────────────┘
                    ▼
              DF-501~509 (P5 DeepSeek确认卡)
                    │
                    ▼
              DF-604~607 (P6 安全备份)
                    │
                    ▼
              DF-701~706 (P7 集成回归)
                    │
                    ▼
              DF-801~806 (P8 发布)
```

---

## 5. Hermes 执行节奏

### 每轮开发会话

1. **读取任务定义**：从 `CODEX_IMPLEMENTATION_PLAN.md` 获取当前任务详情
2. **检查环境**：`git status --short`，确认无未提交修改
3. **搜索现有实现**：
   - 搜索 Daily Flow / My Brain 中是否已有相同能力
   - 搜索 Track & Graph 固定提交中是否已有可移植实现
4. **列出文件清单**：本任务需修改/新增的最小文件列表
5. **先写测试**：移植或编写 JVM 测试
6. **实现代码**：使用文件编辑工具进行修改
7. **运行验证**：
   - 模块级 JVM 单元测试
   - lint 检查
   - debug 构建
8. **更新台账**：
   - 更新 `docs/UPSTREAM_PROVENANCE.md`（如有移植）
   - 更新 `docs/DEVELOPMENT_PROGRESS.md`（任务状态）
   - 更新 `doc_for_hermes/DEVELOPMENT_LOG.md`（Hermes 日志）
9. **提交**：单任务提交，标题 `DF-NNN <摘要>`

### 构建命令（用于验证）

由于 Hermes 运行在 Bash 环境下，使用以下命令：

```bash
# 设置外部 build root
export DAILYFLOW_BUILD_ROOT="$LOCALAPPDATA/DailyFlow/build"

# 模块级 JVM 测试
./gradlew.bat -PdailyFlow.buildRoot="$LOCALAPPDATA/DailyFlow/build" :core:alarm:testDebugUnitTest :tasks:domain:testDebugUnitTest

# lint
./gradlew.bat -PdailyFlow.buildRoot="$LOCALAPPDATA/DailyFlow/build" lintDebug

# debug 构建
./gradlew.bat -PdailyFlow.buildRoot="$LOCALAPPDATA/DailyFlow/build" :app:assembleDebug
```

> ⚠️ Gradle 命令在 Bash 中需要 `./gradlew.bat` 形式调用，Windows 环境变量需用 `$LOCALAPPDATA` 引用。

---

## 6. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 百度同步盘文件锁 | 构建失败 | 使用外部 build root |
| 雷电仅为 API 28 | 无法测 exact alarm / notification 权限 | 高版本验证留给真实设备 Beta 阶段 |
| Hermes 无法直接运行 Android 构建 | 无法自行验证构建 | 依赖用户运行构建命令并提供结果；代码层面通过逻辑审查和静态分析 |
| DF-405 迁移复杂度 | 旧 alarm 数据丢失或重复 | 基线测试先行，迁移使用事务，保留旧 alarm 表做回滚参考 |
| DeepSeek API 兼容性变化 | AI 功能不可用 | Provider 抽象层 + 契约测试 |

---

## 7. 里程碑

| 版本 | 内容 | 预期 |
|------|------|------|
| v0.5 | P4 多提醒完成 | 任务/事件多提醒、记录提示 |
| v0.6 | P5 DeepSeek 提案 | 写入前确认、确认卡 |
| v0.7 | P6 Keystore + 备份 | secret 安全、数据可恢复 |
| v0.8 | P7 集成 | 今日视图、全局搜索、无障碍 |
| v0.9 | P8 Beta | 真实设备验证 |
| v1.0 | 正式发布 | GitHub / F-Droid |

---

## 8. Hermes 专用文档

以下文件由 Hermes 维护，存放于 `doc_for_hermes/`：

| 文件 | 用途 |
|------|------|
| `PROJECT_UNDERSTANDING.md` | 项目完整理解（本文档的基线参考） |
| `HERMES_DEVELOPMENT_PLAN.md` | 本文件 — Hermes 开发计划 |
| `DEVELOPMENT_LOG.md` | 逐任务开发日志（每完成一个 DF-NNN 追加） |
| `ISSUES_AND_DECISIONS.md` | 发现的问题、风险、决策记录 |

---

## 9. 审批

请确认以下事项后开始执行：

1. [ ] 同意从 DF-405 开始，按 P4→P5→P6→P7→P8 顺序推进
2. [ ] 同意 Hermes 在 `doc_for_hermes/` 下维护开发档案
3. [ ] 同意每个 DF-NNN 任务一个 commit 的提交策略
4. [ ] 确认构建验证由 Hermes 尝试运行 Gradle，如遇环境问题会告知用户
