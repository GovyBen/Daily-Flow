# Daily Flow：Codex 详细实施计划

版本：v1.0  
编制日期：2026-06-13  
用途：供 Codex 按任务逐项实施、测试和提交  
状态：执行中

## 1. 文档定位

本文件是开发执行手册，不替代产品范围文档。

- 产品范围和已批准需求以 `docs/DEVELOPMENT_PLAN.md` 为准。
- 简要审批内容以 `docs/APPROVAL_SUMMARY.md` 为准。
- 雷电模拟器配置和设备测试准备以 `docs/LDPLAYER_TEST_ENV_PLAN.md` 为准。
- 本文件在数据结构和代码复用方式上给出更具体的实施决策；若与早期文档中的建议模型冲突，以本文件为准。

项目目标是基于 My Brain 构建完全开源的 Android 日程与生活记录应用，并尽可能移植、适配现有 GPL 开源代码，避免重新实现成熟能力。

## 2. 已冻结的产品决策

除非用户明确修改，Codex 不应在开发过程中重新讨论以下事项：

1. 应用暂定名为 `Daily Flow`。
2. My Brain 是唯一主干，保留其 Kotlin、Compose、Room、Koin、Ktor 和 Koog 技术栈。
3. Track & Graph 只做选择性代码移植，不合并完整工程，不引入 Hilt。
4. 最低系统版本为 Android 8 / API 26。
5. 首版包含任务、系统日历事件、结构化记录、统计、多重提醒、DeepSeek、普通备份和应用锁。
6. 首版不包含 CalDAV、自建同步、内置语音识别、数据库加密和加密备份。
7. 语音转文字由 Android 输入法完成，应用只接收文本。
8. DeepSeek 使用 OpenAI-compatible API。
9. AI 查询可以直接执行；AI 写操作必须先生成本地提案，用户确认后才能调用写入用例。
10. 时间、目标对象、模板或提醒有歧义时必须追问。
11. API Key 不允许以明文写入 DataStore、日志、备份或崩溃报告。
12. 应用继续使用 GPLv3，并满足上游版权和源码提供义务。
13. GitHub Release APK 是首要发布渠道，F-Droid 是正式发布目标。

## 3. 上游代码基线

### 3.1 My Brain

- 仓库：`https://github.com/mhss1/MyBrain`
- 标签：`v3.1.0`
- 固定提交：`a49727cbec0e686edb82f447b2984ecb674d3edf`
- 许可证：GPLv3
- 角色：项目主干

优先原样保留：

- `app`
- `tasks`
- `calendar`
- `diary`
- `notes`
- `bookmarks`
- `settings`
- `ai:data`
- `ai:domain`
- `ai:presentation`
- `core:alarm`
- `core:database`
- `core:notification`
- `core:preferences`
- `core:ui`
- `core:util`
- `core:widget`
- `core:di`
- `widget`

已确认可复用能力：

- 任务创建、编辑、完成、重复规则和任务提醒。
- Android Calendar Provider 访问。
- AlarmManager、开机恢复和通知基础设施。
- Room 数据库、迁移和依赖注入。
- 日记、笔记、书签及现有导航。
- Koog Agent、OpenAI-compatible Provider、工具调用和消息卡片。
- JSON 导出、导入和自动备份。
- 生物识别应用锁。
- GitHub Actions Android 构建流程。

### 3.2 Track & Graph

- 仓库：`https://github.com/SamAmco/track-and-graph`
- 固定提交：`4bb925a731e0537f6330971853770e9aafb51983`
- 许可证：GPLv3-or-later
- 角色：选择性移植来源和算法参考

优先评估和移植的上游文件：

```text
app/data/.../database/entity/Feature.kt
app/data/.../database/entity/Tracker.kt
app/data/.../database/entity/DataPoint.kt
app/data/.../dto/DataPoint.kt
app/data/.../dto/IDataPoint.kt
app/data/.../sampling/DataSample.kt
app/data/.../sampling/RawDataSample.kt
app/app/.../adddatapoint/SuggestedValueHelper.kt
app/app/.../adddatapoint/AddDataPointPage.kt
app/app/.../adddatapoint/AddDataPointsView.kt
app/app/.../adddatapoint/AddDataPointsViewModel.kt
app/app/.../reminders/
ui/compose/ui/DurationInput.kt
ui/compose/ui/DateTimeSelectorButtons.kt
app/data/.../csvreadwriter/CSVReadWriterImpl.kt
app/app/.../graphstatview/functions/aggregation/FixedBinAggregator.kt
app/app/.../graphstatview/functions/aggregation/MovingAggregator.kt
app/app/.../graphstatview/helpers/TimeHelper.kt
```

对应单元测试应与生产代码一起评估和移植。不得只复制实现而丢弃可复用测试。

### 3.3 上游锁定原则

1. 开发开始时将两个提交记录到 `docs/UPSTREAM_PROVENANCE.md`。
2. 新建 `upstream-mybrain` 和 `upstream-trackgraph` Git remote。
3. 不直接追踪上游浮动分支作为构建输入。
4. 更新上游前先创建独立升级任务，检查迁移、行为变化和许可证。
5. Track & Graph 不作为 Gradle composite build、Git submodule 或运行时依赖。
6. 从 Track & Graph 移植的文件应保留原版权头；有明显改写时增加 Daily Flow 修改说明。

## 4. 减少新代码的强制规则

Codex 在每个实现任务中按以下顺序寻找方案：

1. 搜索 Daily Flow / My Brain 是否已有相同能力。
2. 搜索 Track & Graph 固定提交是否已有可移植实现和测试。
3. 搜索 AndroidX、Kotlin 标准库或项目已有依赖是否提供该能力。
4. 评估一个许可证兼容、F-Droid 可构建的开源依赖。
5. 只有前四项都不能合理满足时，才编写新的产品代码。

每个任务结束时必须记录：

- 原样复用的文件。
- 修改的上游文件。
- 移植的 Track & Graph 文件及原路径。
- 新增的胶水代码。
- 未复用现成实现的原因。

禁止事项：

- 不为 Daily Flow 自行编写图表引擎。
- 不自行编写 CSV 解析器。
- 不自行设计加密算法或密钥派生算法。
- 不另建一套网络栈、依赖注入框架或数据库。
- 不同时保留 Koin 和 Hilt。
- 不整体复制 Track & Graph 的数据库、导航、Lua 或函数系统。
- 不进行全项目包名替换，除非独立任务证明收益高于风险。
- 不因命名偏好重构与需求无关的 My Brain 文件。
- 不先写新组件再寻找上游实现。
- 不让 Compose 页面直接访问 DAO、Calendar Provider 或网络客户端。

## 5. 代码来源分级

所有生产文件在评审时归入以下一种来源：

| 类别 | 含义 | 要求 |
|---|---|---|
| A | My Brain 原样保留 | 尽量不改 |
| B | My Brain 小范围修改 | 保持原结构，测试修改行为 |
| C | Track & Graph 移植或改写 | 记录原路径、提交和版权 |
| D | Daily Flow 新胶水或新需求代码 | 必须说明复用缺口 |
| E | 新增开源依赖 | 许可证、版本和 F-Droid 审核 |

阶段目标：

- 在 `v0.7` 前，My Brain 既有生产文件至少 80% 保持无行为修改。
- 图表、聚合、CSV、日期时间输入和提醒降级不得从零实现。
- 新代码主要集中于模板、记录会话、通用数据点适配和 AI 确认流程。
- 每次提交只处理一个任务编号，避免来源和行为难以审计。

## 6. 目标工程结构

为减少 Gradle 模块样板代码，首版只新增一个 `tracking` Android library 模块，不提前拆分 `analytics`、`reminders` 或 `security` 模块。

```text
tracking/
  src/main/.../domain/
  src/main/.../data/
  src/main/.../presentation/
  src/main/.../analytics/
  src/test/
  src/androidTest/
```

其余能力在原模块内扩展：

| 能力 | 实施位置 |
|---|---|
| 结构化记录与统计 | 新增 `tracking` 模块 |
| 任务和日历多提醒 | 扩展 `core:alarm`、`tasks`、`calendar` |
| DeepSeek 和确认提案 | 扩展现有 `ai:*` |
| API Key 安全存储 | 扩展 `core:preferences` 或 `settings:data` |
| 应用锁 | 扩展现有 settings/app-lock 代码 |
| 普通备份 | 扩展现有 settings backup 代码 |
| 今日聚合入口 | 扩展 `app` |
| 桌面快捷记录 | 扩展 `widget` |

只有出现以下任一情况才允许继续拆模块：

- 单模块循环依赖无法通过接口反转解决。
- tracking 编译时间明显影响全工程。
- 某能力需要被三个以上模块稳定复用。
- 拆分能移除实际依赖，而不只是整理目录。

## 7. 结构化记录的最终数据方案

### 7.1 设计目标

保留 Track & Graph 的 `Tracker + DataPoint(value/label/note/time)` 核心表达，只新增“模板”和“一次记录会话”两层。八种输入控件共享同一套数据点存储和统计适配器。

不采用早期计划中的八种独立值表，也不为每种控件建立独立统计管线。

### 7.2 实体

`RecordTemplateEntity`

```text
id: String
name: String
description: String
icon: String
color: Long
isActive: Boolean
displayOrder: Int
createdAtEpochMilli: Long
updatedAtEpochMilli: Long
```

`TrackerEntity`

```text
id: String
name: String
type: MULTI_SELECT | SINGLE_SELECT | COUNTER | SCALE |
      BOOLEAN | DURATION | NUMBER | TEXT
unit: String?
configJson: String
isActive: Boolean
createdAtEpochMilli: Long
updatedAtEpochMilli: Long
```

`TemplateFieldEntity`

```text
id: String
templateId: String
trackerId: String
displayOrder: Int
required: Boolean
displayNameOverride: String?
defaultValueJson: String?
```

`TrackerOptionEntity`

```text
id: String
trackerId: String
label: String
numericValue: Double?
color: Long?
displayOrder: Int
isActive: Boolean
```

`RecordSessionEntity`

```text
id: String
templateId: String
occurredAtEpochMilli: Long
zoneId: String
note: String?
source: MANUAL | AI | IMPORT
createdAtEpochMilli: Long
updatedAtEpochMilli: Long
```

`DataPointEntity`

```text
id: String
sessionId: String?
trackerId: String
epochMilli: Long
utcOffsetSeconds: Int
value: Double?
label: String?
note: String?
optionId: String?
createdAtEpochMilli: Long
updatedAtEpochMilli: Long
```

与 Track & Graph 原始模型相比，`DataPointEntity` 使用独立生成的 `id` 作为主键，并增加 `sessionId`。这是必要的最小改动，因为多选字段会在同一时间、同一 tracker 下产生多个数据点，不能使用 `(epochMilli, featureId)` 作为唯一主键。

### 7.3 控件到数据点的映射

| 控件 | 数据点表达 |
|---|---|
| 多选 | 每个已选选项一个数据点；同一 `sessionId`；`value` 为选项数值或 1；`label` 保存选项快照 |
| 单选 | 一个数据点；`value` 为选项数值或 1；`label` 保存选项快照 |
| 计数 | 一个数据点；`value` 为非负整数 |
| 滑条 | 一个数据点；`value` 为范围内数值 |
| 布尔 | 一个数据点；`value` 为 1 或 0 |
| 时长 | 一个数据点；`value` 统一存秒数 |
| 普通数值 | 一个数据点；`value` 为 Double |
| 文本 | 一个数据点；`note` 保存正文，`value` 为空 |

### 7.4 必须保持的约束

1. 保存一个记录会话及其全部数据点必须使用 Room 事务。
2. 编辑会话必须在同一事务中校验并替换该会话的数据点。
3. 已使用的模板、字段和选项只允许停用，不物理删除。
4. 数据点必须保存选项和文字含义快照，避免重命名破坏历史。
5. 所有数值进入数据库前执行本地类型和范围校验。
6. 所有时间以 epoch milliseconds 保存，并保留录入时区。
7. 统计层通过适配器将 `DataPointEntity` 转换为移植的 `DataSample`。
8. 文本记录默认不参与数值聚合。
9. AI 和导入数据必须经过与手工输入相同的验证器。
10. 数据库迁移必须具备从上一发布版本升级的测试。

## 8. Codex 单任务执行规程

每次开发会话应遵循以下步骤：

1. 读取本文件中当前任务、前置任务和验收条件。
2. 执行 `git status --short`，不得覆盖用户已有修改。
3. 用 `rg` 搜索现有实现和调用点。
4. 打开上游固定提交中的候选文件和对应测试。
5. 写出本任务的最小文件清单。
6. 先移植或适配测试，再实现行为。
7. 使用 `apply_patch` 做手工修改。
8. 运行任务级测试。
9. 运行受影响模块的 lint 或编译。
10. 检查 `git diff --check` 和 `git diff --stat`。
11. 更新来源台账和任务状态。
12. 一个提交只包含一个任务编号。

建议提交标题：

```text
DF-<编号> <imperative summary>
```

提交正文至少包含：

```text
Reuse:
- My Brain: <paths>
- Track & Graph: <paths or none>

New code:
- <paths and reason>

Verification:
- <commands>
```

若任务需要修改超过 20 个生产文件，或新增超过 800 行非生成生产代码，Codex 应暂停实施并重新检查是否复制了过多上游工程、遗漏了复用点或把多个任务混在一起。

## 9. 通用验证命令

Windows PowerShell 默认命令：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug --stacktrace
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
git diff --check
git status --short
```

模块级命令按实际 Gradle task 名称调整：

```powershell
.\gradlew.bat :tracking:testDebugUnitTest
.\gradlew.bat :tracking:connectedDebugAndroidTest
.\gradlew.bat :core:alarm:testDebugUnitTest
.\gradlew.bat :ai:data:testDebugUnitTest
.\gradlew.bat :app:assembleRelease
```

设备命令在模拟器准备完成后执行：

```powershell
adb devices -l
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop <applicationId>
adb shell monkey -p <applicationId> 1
adb logcat -d
```

不得把“本地没有设备”当作跳过 JVM 单元测试、编译和 lint 的理由。

## 10. 分阶段实施任务

任务状态标记：

- `[ ]` 未开始
- `[~]` 进行中
- `[x]` 完成
- `[!]` 阻塞

### P0：主干导入、可构建基线和许可证

阶段出口：固定 My Brain 基线能够在本机和 CI 构建；来源、许可证和构建工具明确。

#### DF-001 导入 My Brain 固定基线

- [x] 状态
- 前置：无
- 目标：在当前工作区根目录建立以 My Brain `v3.1.0` 为历史基线的 Git 仓库，同时保留现有 `docs/`。
- 复用：My Brain 全工程。
- 操作：
  1. 备份当前 `docs/` 文件清单和校验值。
  2. 初始化 Git 或在临时目录克隆后安全合入。
  3. 添加 `upstream-mybrain` remote。
  4. fetch 固定标签和提交。
  5. 检出独立 `main` 分支。
  6. 恢复并加入当前 Daily Flow 文档。
- 禁止：不使用会删除未跟踪文档的命令；不执行 `git reset --hard`。
- 验收：
  - `git rev-parse HEAD` 可追溯到固定上游。
  - 当前 `docs/` 文档均存在。
  - `git status` 中没有意外删除。

#### DF-002 建立 Track & Graph 参考 remote

- [x] 状态
- 前置：DF-001
- 目标：允许按固定提交读取 Track & Graph，而不合并其历史。
- 操作：
  1. 添加 `upstream-trackgraph` remote。
  2. fetch 固定提交。
  3. 建立只读参考 tag，例如 `upstream-trackgraph-20260613`。
  4. 不把 Track & Graph checkout 到生产源码目录。
- 验收：`git show 4bb925a... --stat` 可用，主分支文件无变化。

#### DF-003 记录许可证和来源

- [x] 状态
- 前置：DF-001、DF-002
- 新增：
  - `THIRD_PARTY_NOTICES.md`
  - `docs/UPSTREAM_PROVENANCE.md`
- 内容：
  - 两个仓库 URL、许可证、固定提交。
  - Track & Graph 移植文件表。
  - 修改日期和 Daily Flow 修改摘要。
  - 构建依赖许可证生成方式。
- 验收：所有上游来源可由路径和提交唯一定位。

#### DF-004 验证本地基线构建

- [x] 状态
- 前置：DF-001
- 目标：在改业务代码前确认 JDK、Gradle Wrapper 和 Android SDK。
- 命令：
  - `.\gradlew.bat --version`
  - `.\gradlew.bat :app:assembleDebug --stacktrace`
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat lintDebug`
- 产物：`docs/BUILD_BASELINE.md`
- 记录：
  - JDK 版本。
  - Gradle、AGP、Kotlin 版本。
  - compileSdk、targetSdk、minSdk。
  - 构建时长、警告和已知失败。
- 验收：Debug APK 成功生成，或所有阻塞均有可复现错误和修复任务。

#### DF-005 固定品牌变更的最小范围

- [~] 状态：代码和静态检查完成，等待设备并存安装验证
- 前置：DF-004
- 目标：只修改用户可见名称、应用 ID 和必要资源，不全局替换 Kotlin package。
- 决策：
  - 先确认最终 `applicationId`。
  - Kotlin namespace 是否保留由 manifest、provider authority 和迁移成本决定。
  - authority、FileProvider、backup 文件名必须随 `applicationId` 检查。
- 测试：安装、启动、备份 provider、日历权限和通知 channel。
- 验收：旧 My Brain 与 Daily Flow 可并存安装；不存在 authority 冲突。

#### DF-006 调整并加固 CI

- [~] 状态：工作流已配置，等待首次远程 Runner 验证
- 前置：DF-004
- 复用：My Brain `.github/workflows/build-android.yml`。
- 修改：
  - 固定 JDK 17。
  - 缓存 Gradle。
  - 执行 unit test、lint、assembleDebug。
  - release 任务只在 tag 或显式 workflow 运行。
  - 禁止把 DeepSeek Key 写入 CI。
- 验收：干净 runner 可重复构建；失败日志不包含 secret。

#### DF-007 建立测试夹具和来源统计脚本

- [x] 状态
- 前置：DF-004
- 目标：统一测试时间、ID、时区和数据构造，避免后续测试重复样板。
- 优先复用：My Brain 测试工具和 Track & Graph 测试 fixtures。
- 允许新增：
  - `TestClock`
  - `TestIdGenerator`
  - `TestZone`
  - entity factory
- 来源统计：先使用文档和 Git diff，不为此引入复杂代码生成器。
- 验收：测试不依赖当前日期、系统时区或随机 UUID。

### P0.5：首轮体验后的产品结构纠偏

阶段出口：完整日历成为主导航栏位；DeepSeek 和中国大陆 Provider 作为内置
预设可见；笔记、日记和书签收敛到一个内容库入口。

详细产品决策、默认 Provider 和验收范围见
`docs/PRODUCT_RESTRUCTURE_PLAN.md`。

#### DF-008 日历升级为主导航栏位

- [x] 状态
- 前置：DF-005
- 目标：
  - 底栏固定为概览、日历、空间、设置。
  - 日历默认显示完整月视图及所选日期事件。
  - 从空间移除重复日历卡片。
- 导航：
  - 区分 tab 导航和应用级详情导航。
  - 概览日历卡片切换到日历 tab。
  - 事件详情、Deep Link 和返回栈保持兼容。
- 验收：四个主栏位可恢复状态，日期新增事件使用所选日期，设备测试通过。
- 完成记录（2026-06-13）：
  - 月视图成为新的默认值，旧版列表视图偏好使用独立键隔离。
  - Android 9 / API 28 雷电实例完成启动、四栏切换和 Calendar Deep Link 冒烟验证。
  - `testDebugUnitTest`、`lintDebug` 和 `:app:assembleDebug` 通过。

#### DF-009 提前执行 SecretStore 审计和迁移

- [x] 状态
- 前置：DF-004
- 目标：在新增云 Provider Key 前执行 `DF-601` 至 `DF-603`。
- 验收：现有及新增 Provider Key 均不以明文出现在 DataStore、日志或备份。
- 完成记录（2026-06-13）：Keystore 仪器测试验证密文往返、DataStore 无明文及旧 Key 迁移。

#### DF-010 建立 AI Provider 注册表

- [x] 状态
- 前置：DF-009
- 目标：
  - 用稳定 ID、分类、默认 endpoint、默认模型和能力声明替代 UI 内硬编码列表。
  - 保留现有 Provider 偏好的迁移映射。
  - OpenAI-compatible Provider 复用当前 Koog/OpenAI 客户端。
- 验收：增加 Provider 不改变既有 ID，设置页不直接维护协议分支。
- 完成记录（2026-06-13）：既有 ID 由单元测试锁定，协议与 OpenAI-compatible
  客户端选择由注册表驱动。

#### DF-011 增加推荐和中国大陆 Provider 预设

- [x] 状态
- 前置：DF-010
- 预设：
  - 推荐：DeepSeek、OpenAI。
  - 中国大陆：通义千问、Kimi、智谱 GLM。
  - 保留 Gemini、Anthropic、OpenRouter、Ollama 和 LM Studio。
- UI：
  - DeepSeek、OpenAI 置顶并显示“推荐”。
  - 首次启用 AI 默认选择 DeepSeek。
  - 模型和 Base URL 可编辑并可恢复默认。
- 验收：无 API Key 时不发请求；用户自定义模型不被应用升级覆盖。
- 完成记录（2026-06-13）：雷电实例验证 DeepSeek 默认启用、推荐标签、Provider
  分组、默认模型和 Base URL；云 Provider 无 Key 时不创建执行器。

#### DF-012 增加 Provider 连接和能力测试

- [ ] 状态
- 前置：DF-011
- 测试：最小文本、流式响应、Tool Calling、多工具调用、错误分类和不支持参数降级。
- 安全：连接测试只使用无副作用 fake tool，不得写入任务、日历或记录。
- 验收：每个内置 Provider 有 fake server 契约测试和明确能力状态。

#### DF-013 建立统一内容库入口

- [x] 状态
- 前置：DF-004
- 目标：空间只暴露一个“内容库”，内部筛选全部、笔记、日记和链接。
- 限制：先不删除原模块和路由。
- 验收：三个内容类型可从一个页面浏览、搜索和新增。
- 完成记录（2026-06-13）：空间页仅保留一个内容库卡片；雷电实例验证统一
  搜索、四类筛选、空状态和新增菜单。

#### DF-014 建立内容库聚合模型和查询

- [x] 状态
- 前置：DF-013
- 新增：`ContentItem`、`ContentType`、`ContentLibraryRepository` 和类型适配器。
- 复用：现有 Note、Diary、Bookmark repository 和 Flow。
- 禁止：Compose 直接访问三个 DAO；不进行破坏性三表合一。
- 验收：统一排序、搜索和空状态，原外部 Markdown 能力不回归。
- 完成记录（2026-06-13）：应用层聚合三个现有 Repository Flow，保留笔记
  Repository 的 Room/外部 Markdown 动态实现；单元测试覆盖合并、排序、类型筛选
  以及标题、正文和 URL 搜索。

#### DF-015 统一内容编辑入口和兼容路径

- [x] 状态
- 前置：DF-014
- 目标：
  - 普通正文、带心情内容和带 URL 内容通过统一入口创建。
  - 旧 Deep Link 重定向到内容库详情。
  - JSON 备份继续兼容旧结构。
- 验收：已有笔记、日记、书签均不丢失，旧版本备份可导入。
- 完成记录（2026-06-13）：统一新增菜单按笔记、日记、链接进入原详情编辑器；
  旧列表路由重定向为内容库对应筛选，原详情路由和 Note Deep Link 保持兼容。
  三张数据表、外部 Markdown 与 JSON 导入导出结构均未改动。

### P1：结构化记录数据层

阶段出口：模板可以持久化，一次会话可以事务性保存八类字段对应的数据点，仓库测试完整。

#### DF-101 新增最小 `tracking` 模块

- [x] 状态
- 前置：DF-004
- 目标：创建单一 Android library module，并遵循现有 My Brain Gradle 约定。
- 依赖：
  - Compose、Room、Koin、Coroutines 使用版本目录或现有 convention。
  - 不添加 Hilt。
  - 不添加第二个 JSON 库。
- 验收：
  - `:tracking:assembleDebug` 成功。
  - app 暂不需要展示页面。
- 完成记录（2026-06-13）：已新增单一 Android library module，统一复用版本目录中的
  Compose、Room、Koin、Coroutines、Kotlin Serialization 和 datetime 依赖；
  `:tracking:assembleDebug` 通过，尚未接入应用页面。

#### DF-102 移植 DataSample 核心接口和测试

- [x] 状态
- 前置：DF-101、DF-003
- 来源：
  - Track & Graph `IDataPoint.kt`
  - `DataSample.kt`
  - `RawDataSample.kt`
  - 对应测试
- 目标：保留聚合算法需要的最小不可变数据表示。
- 修改：
  - 包名适配 Daily Flow。
  - 时间类型与现有 My Brain/Kotlin datetime 保持一致。
  - 去除未使用的 Hilt、Rx 或 Feature 依赖。
- 验收：移植测试通过；生产文件保留上游版权说明。
- 完成记录（2026-06-13）：移植 `IDataPoint`、`DataSampleProperties`、
  `DataSample` 和 `RawDataSample`，并将原 DTO 适配为 `RawDataPoint`；时间改用
  `kotlinx.datetime`，移除 ThreeTen、数据库 entity 和 Feature 依赖。JVM 测试覆盖
  相等性、惰性迭代、原始点追踪、空样本与资源释放。

#### DF-103 实现模板和 tracker Room 实体

- [x] 状态
- 前置：DF-101
- 新增：第 7 节定义的 template、tracker、template field、option 实体。
- 复用：
  - My Brain BaseDatabase、converter、migration 约定。
  - Track & Graph Tracker 的字段命名和约束思想。
- 要求：
  - ID 由注入的生成器创建。
  - `configJson` 使用 Kotlin Serialization 的 sealed config。
  - 外键和索引通过 Room schema 测试确认。
- 测试：
  - 每种 config 序列化往返。
  - 排序稳定。
  - 停用不会删除历史引用。
- 完成记录（2026-06-13）：新增模板、tracker、模板字段和选项实体；8 种
  `TrackerConfig` 使用 Kotlin Serialization sealed model。实体由注入式 ID
  生成器工厂创建，JVM 测试覆盖配置往返与稳定排序，内存 Room 仪器测试覆盖
  外键、索引及停用后引用保留。

#### DF-104 实现 RecordSession 和 DataPoint 实体

- [x] 状态
- 前置：DF-103
- 来源：Track & Graph `DataPoint.kt` 和 DTO。
- 必要偏差：
  - 使用 `id` 主键。
  - 增加 `sessionId`、`optionId` 和更新时间。
  - 保留 epoch、offset、value、label、note 表达。
- 索引：
  - `sessionId`
  - `trackerId, epochMilli`
  - `templateId, occurredAtEpochMilli`
- 测试：同一 session、tracker、timestamp 能保存多个多选数据点。
- 完成记录（2026-06-13）：新增会话来源枚举、记录会话与通用数据点实体；
  数据点使用独立 ID，并保留时区偏移、选项和文本快照。Room 仪器测试验证
  同一 session、tracker、timestamp 可持久化多个多选数据点。

#### DF-105 实现 DAO 和事务边界

- [ ] 状态
- 前置：DF-104
- DAO：
  - Template DAO
  - Tracker DAO
  - Session DAO
  - DataPoint DAO
- 事务：
  - 创建模板及字段。
  - 保存 session 及全部 points。
  - 编辑 session 并替换 points。
  - 停用 template/tracker/option。
- 测试：
  - 任一点失败时整次保存回滚。
  - 编辑不留下孤立 point。
  - 时间范围查询边界正确。

#### DF-106 实现八类字段验证器

- [ ] 状态
- 前置：DF-103
- 新代码理由：这是 Daily Flow 特有模板语义，两个上游均无完全匹配实现。
- 结构：
  - 一个公共 `TrackerValueValidator`。
  - sealed 输入值。
  - 每种类型的小型纯函数验证器。
- 规则：
  - counter 为非负整数。
  - scale 落在 min/max 且遵守 step 容差。
  - duration 非负且有合理上限。
  - number 拒绝 NaN 和 Infinity。
  - select 只能引用活动选项。
  - required 字段不得为空。
- 测试：表驱动测试覆盖正常值、边界、错误值。

#### DF-107 实现输入值到 DataPoint 的映射器

- [ ] 状态
- 前置：DF-104、DF-106
- 目标：八种控件共享一个转换入口。
- 规则：严格按第 7.3 节映射。
- 测试：
  - 多选生成多个 point。
  - option label 被快照。
  - boolean 生成 0/1。
  - duration 统一为秒。
  - text 不伪造 numeric value。

#### DF-108 实现 tracking repository 和 use cases

- [ ] 状态
- 前置：DF-105、DF-107
- 用例：
  - CreateTemplate
  - UpdateTemplate
  - DuplicateTemplate
  - ReorderTemplates
  - SaveRecordSession
  - UpdateRecordSession
  - DeleteRecordSession
  - ObserveTemplates
  - ObserveRecordHistory
  - GetSuggestedValues
- 复用：My Brain repository/use-case 风格；不创建第二套 Result 包装。
- 验收：presentation 层不接触 DAO。

#### DF-109 建立数据库迁移和 schema 导出

- [ ] 状态
- 前置：DF-105
- 目标：把 tracking 表加入主数据库。
- 要求：
  - 开启 Room schema 导出。
  - 使用 MigrationTestHelper。
  - 新安装和从 My Brain 基线升级都测试。
  - 不使用 destructive migration。
- 验收：升级保留已有任务、日记、设置和日历映射。

#### DF-110 提供内置示例模板

- [ ] 状态
- 前置：DF-108
- 模板：
  - 健身：胸、背、臀、腿等多选 + 时长 + 备注。
  - 心情：1 至 10 滑条 + 备注。
  - 习惯次数：社交、吸烟等计数示例，但不预设健康结论。
- 实现：
  - 仅首次启动插入。
  - 使用固定稳定 ID。
  - 用户删除或停用后不被下次启动恢复。
- 验收：已有用户升级不重复插入。

### P2：模板编辑、快速记录和历史

阶段出口：用户可建立模板、使用八种控件录入、编辑历史，并从今日页快速进入。

#### DF-201 移植日期时间和时长输入组件

- [ ] 状态
- 前置：DF-101
- 来源：
  - Track & Graph `DurationInput.kt`
  - `DateTimeSelectorButtons.kt`
- 适配：
  - 使用 My Brain Material 主题、字符串资源和尺寸。
  - 无障碍 label。
  - 24/12 小时制跟随系统。
- 测试：Compose UI 测试覆盖输入、清空、确认和配置变化。

#### DF-202 建立通用动态字段渲染器

- [ ] 状态
- 前置：DF-106、DF-201
- 目标：单一入口按 TrackerType 渲染控件。
- 优先复用：
  - My Brain Compose form 组件。
  - Track & Graph 快速输入布局。
- 禁止：每种类型建立一套页面和 ViewModel。
- 验收：八种控件均输出 sealed input value，错误由验证器统一显示。

#### DF-203 移植 SuggestedValueHelper

- [ ] 状态
- 前置：DF-108
- 来源：Track & Graph `SuggestedValueHelper.kt` 及测试。
- 目标：
  - 提供最近值、常用值和默认值。
  - counter 可一键递增。
  - 不对文本隐私内容默认展示长历史。
- 验收：建议值排序稳定，不阻塞主线程。

#### DF-204 实现模板列表

- [ ] 状态
- 前置：DF-108
- 能力：
  - 创建、复制、排序、停用。
  - 固定常用模板。
  - 显示最后记录时间。
- 复用：My Brain 列表、空状态、FAB、确认对话框。
- 验收：列表变化来自 Flow，旋转不丢状态。

#### DF-205 实现模板编辑器

- [ ] 状态
- 前置：DF-202、DF-204
- 能力：
  - 名称、图标、颜色。
  - 增删和排序字段。
  - 类型特定配置。
  - 选项管理。
  - 预览。
- 约束：
  - 已有历史的 tracker 不允许改变为不兼容类型。
  - 删除改为停用并清晰提示。
- 测试：兼容性规则、必填校验、排序和恢复草稿。

#### DF-206 实现快速记录页面

- [ ] 状态
- 前置：DF-202、DF-203
- 参考：Track & Graph AddDataPoint 页面和 ViewModel。
- 能力：
  - 一页显示模板全部字段。
  - 修改发生时间。
  - 添加会话备注。
  - 保存后显示真实结果。
  - 防止重复点击提交。
- 验收：典型三字段模板在少于五次操作内完成记录。

#### DF-207 实现记录历史和编辑

- [ ] 状态
- 前置：DF-108、DF-206
- 能力：
  - 按模板、日期筛选。
  - 查看 session 内全部字段。
  - 编辑发生时间和值。
  - 删除需确认并支持短时撤销。
- 验收：编辑和删除通过 repository 事务，不直接逐点操作。

#### DF-208 接入主导航和今日页

- [ ] 状态
- 前置：DF-204、DF-206
- 目标：
  - 在空间增加“自定义记录”主入口，不在当前四栏底部导航中增加第五项。
  - 今日页展示常用模板快捷按钮和今日摘要。
  - 保留任务入口；日历使用 DF-008 主栏；日记通过内容库进入。
- 限制：先沿用现有导航框架，不迁移到新导航库。
- 验收：深链、返回栈和进程恢复不破坏已有页面。

#### DF-209 增加桌面快捷记录

- [ ] 状态
- 前置：DF-206、DF-208
- 复用：My Brain `widget` 和 `core:widget`。
- 范围：
  - 最多展示若干固定模板。
  - 点击打开对应快速记录页。
  - 首版不在 widget 内直接编辑所有字段。
- 验收：模板停用后 widget 不崩溃并提示重新配置。

#### DF-210 按日期整合日历事件和自定义记录

- [ ] 状态
- 前置：DF-008、DF-206、DF-207
- 目标：
  - 日历日期格分别显示事件和记录指示。
  - 日期详情按“日历事件”和“当日记录”分组。
  - 新增入口明确选择写入 Calendar Provider 或本地 tracking。
- 边界：全日事件、跨日事件和记录发生时间按设备时区正确归日。
- 验收：两类数据独立失败、独立授权，不出现写错存储目标。

### P3：统计、图表和 CSV

阶段出口：记录可按日、周、月统计，支持趋势、分布、连续天数和 CSV 导入导出。

#### DF-301 移植时间分箱和聚合函数

- [ ] 状态
- 前置：DF-102、DF-108
- 来源：
  - `FixedBinAggregator.kt`
  - `MovingAggregator.kt`
  - `TimeHelper.kt`
  - 对应工厂、filter 和测试
- 移植范围：
  - sum、count、average、min、max。
  - 固定日/周/月分箱。
  - 需要时保留 moving average。
- 排除：Lua、自定义函数图、首版未使用的复杂组合器。
- 验收：上游测试和 Daily Flow 时区边界测试通过。

#### DF-302 实现 DataPoint 到 DataSample 适配器

- [ ] 状态
- 前置：DF-102、DF-104
- 目标：统计算法不直接依赖 Room entity。
- 规则：
  - numeric value 转为 sample。
  - multi-select 按 label 分组。
  - text 默认过滤。
  - boolean 允许 count 或比例。
- 验收：相同输入在适配前后含义一致。

#### DF-303 实现统计查询用例

- [ ] 状态
- 前置：DF-301、DF-302
- 用例：
  - GetDailySummary
  - GetWeeklySeries
  - GetMonthlySeries
  - GetOptionDistribution
  - GetCurrentStreak
  - GetLongestStreak
- 要求：DAO 只负责范围查询，聚合规则保持纯 Kotlin 可测。
- 测试：空数据、单点、跨午夜、夏令时、跨月和停用字段。

#### DF-304 接入开源图表实现

- [ ] 状态
- 前置：DF-303
- 首选：
  1. 评估移植 Track & Graph 当前 AndroidPlot 图表层。
  2. 若与 Compose 集成成本可控，通过 `AndroidView` 包装。
  3. 只有评估失败才选择另一 F-Droid 兼容开源图表库。
- 禁止：用 Canvas 从零重写折线、坐标轴、缩放和标签布局。
- 产物：在 `docs/UPSTREAM_PROVENANCE.md` 记录选型。
- 验收：折线、柱状、饼图在空数据和大字体下可用。

#### DF-305 实现统计页面

- [ ] 状态
- 前置：DF-303、DF-304
- 能力：
  - 日、周、月范围。
  - tracker 和聚合方式选择。
  - 折线、柱状、分布图。
  - 当前/最长连续天数。
  - 无数据说明。
- 性能：统计计算使用后台 dispatcher；切换范围可取消旧任务。
- 验收：一年日数据的交互无明显主线程卡顿。

#### DF-306 移植 CSV 读写

- [ ] 状态
- 前置：DF-108
- 来源：Track & Graph `CSVReadWriterImpl.kt` 及测试。
- 依赖：优先保留 Apache Commons CSV；核对版本和许可证。
- Daily Flow 格式：
  - schema version
  - session ID
  - template/tracker 名称和 ID
  - occurredAt
  - zoneId
  - value/label/note
- 要求：
  - 导入先预览和校验。
  - 错误包含行号。
  - 导入使用事务。
- 验收：导出后重新导入等价；逗号、换行、中文和引号测试通过。

### P4：统一多重提醒

阶段出口：任务、日历事件和记录提示可配置多个提醒，重启后恢复，精确权限不可用时可降级。

#### DF-401 审计 My Brain 现有提醒行为

- [ ] 状态
- 前置：DF-004
- 阅读：
  - `UpsertTaskUseCase`
  - `core:alarm`
  - `AlarmSchedulerImpl`
  - `BootBroadcastReceiver`
- 输出：`docs/REMINDER_CURRENT_STATE.md`
- 记录：
  - alarm ID/requestCode 生成。
  - 修改和删除如何取消。
  - 开机恢复。
  - Android 12+ exact alarm 权限。
  - 通知 channel。
- 验收：现有单提醒行为有自动测试或可重复手工步骤。

#### DF-402 移植 Track & Graph 调度降级模式

- [ ] 状态
- 前置：DF-401
- 来源：
  - Track & Graph `reminders/`
  - `AndroidPlatformScheduler.kt`
  - WorkManager fallback、boot recreation 和测试
- 目标：提取 AlarmManager + WorkManager 降级所需的最小闭包。
- 适配：使用 Koin、My Brain notification 和现有 receiver。
- 禁止：保留两套并行 boot receiver 或 notification 基础设施。
- 验收：调度策略为纯接口，单测可使用 fake scheduler。

#### DF-403 新增统一 ReminderEntity

- [ ] 状态
- 前置：DF-401
- 字段：
  - id
  - targetType
  - targetId
  - absoluteTriggerAt
  - relativeOffsetMinutes
  - enabled
  - status
  - createdAt/updatedAt
- 目标类型：TASK、CALENDAR_EVENT、RECORD_PROMPT。
- 约束：绝对时间和相对 offset 必须有且仅有一种来源语义。
- 测试：多提醒唯一 request code，不相互覆盖。

#### DF-404 实现提醒重算和同步用例

- [ ] 状态
- 前置：DF-402、DF-403
- 用例：
  - ScheduleReminder
  - CancelReminder
  - RescheduleTargetReminders
  - RestoreAllReminders
  - ReconcileScheduledReminders
- 触发：
  - 目标时间修改。
  - 开机。
  - 时区变化。
  - 应用升级。
  - 权限变化。
- 验收：重复调用幂等，过期提醒不重复弹出。

#### DF-405 改造任务提醒

- [ ] 状态
- 前置：DF-404
- 目标：
  - 任务截止时间与提醒时间分离。
  - 支持多个绝对或相对提醒。
  - 迁移旧单提醒。
- 复用：现有任务编辑 UI、通知内容和完成动作。
- 验收：旧任务升级后提醒不丢失且不重复。

#### DF-406 改造日历事件提醒

- [ ] 状态
- 前置：DF-404
- 目标：
  - Calendar Provider 事件 ID 与本地 reminder target 关联。
  - 事件修改后重新计算相对提醒。
  - 外部日历删除事件时安全处理悬空目标。
- 验收：无 Calendar 权限时不崩溃，用户可修复权限。

#### DF-407 增加记录提示

- [ ] 状态
- 前置：DF-404、DF-204
- 能力：按模板设置固定时间提示，点击通知直达快速记录页。
- 首版：支持每日和指定星期；复杂 RRULE 后置。
- 验收：模板停用会取消未来提示。

#### DF-408 实现多提醒编辑组件

- [ ] 状态
- 前置：DF-405、DF-406
- 复用：My Brain 日期时间控件和 DF-201。
- 能力：
  - 添加多个提醒。
  - 快捷选项：准时、提前 5/15/30 分钟、1 小时、1 天。
  - 自定义绝对时间。
  - 权限和降级说明。
- 验收：UI 阻止重复提醒和过去时间。

### P5：DeepSeek、意图提案和确认执行

阶段出口：自然语言可生成任务、事件、日记和结构化记录提案，所有写操作在确认后执行。

#### DF-501 验证 DeepSeek 提案阶段兼容性

- [ ] 状态
- 前置：DF-011、DF-012
- 复用：
  - DF-010 Provider 注册表。
  - DF-012 Tool Calling 能力测试。
- 目标：确认当前 DeepSeek 默认模型能够稳定生成本阶段 ActionProposal 所需的
  结构化工具参数。
- 要求：不硬编码 API Key；错误区分认证、限流、网络、参数和 tool calling
  不兼容。
- 验收：使用 fake server 和显式测试配置完成 request/response 契约测试，
  不把真实 Key 写入仓库或 CI。

#### DF-502 定义 ActionProposal sealed model

- [ ] 状态
- 前置：DF-501
- 类型：
  - CreateTaskProposal
  - CreateCalendarEventProposal
  - CreateDiaryEntryProposal
  - CreateRecordSessionProposal
  - CompleteTaskProposal
  - ClarificationRequest
- 字段：
  - proposal ID
  - 来源文本
  - 结构化参数
  - 缺失/歧义字段
  - 用户可编辑字段
  - 本地验证结果
- 新代码理由：My Brain 当前工具直接写入，不具备确认隔离。
- 验收：proposal 可序列化，但不持久化 secret。

#### DF-503 将写工具改为“只提案”

- [ ] 状态
- 前置：DF-502
- 来源：My Brain 现有 Task/Calendar/Diary 工具。
- 修改：
  - 写工具不再持有写 repository/use case。
  - 工具只构造 proposal。
  - 查询工具保留只读执行能力，并限制返回条数。
- 安全测试：
  - 模型调用写工具后数据库无变化。
  - 伪造 tool result 不能绕过本地执行器。

#### DF-504 新增结构化记录 AI 工具

- [ ] 状态
- 前置：DF-108、DF-502
- 工具：
  - getRecordTemplates
  - proposeCreateRecordSession
  - searchRecordSessions
- 语义：
  - tracker/option 只接受本地已知 ID。
  - 模型输出经过 DF-106 验证器。
  - 模糊模板或选项转为 clarification。
- 验收：不存在“健身”模板时不得静默创建任意 tracker。

#### DF-505 实现本地日期和歧义解析层

- [ ] 状态
- 前置：DF-502
- 目标：模型提供语义字段，本地库负责最终时间计算。
- 规则：
  - 使用设备时区。
  - “明天下午”缺少具体时间时追问。
  - “下周三”在确认卡显示完整年月日。
  - 结束时间缺失时应用明确默认时长并展示。
  - 提醒提前量缺失时追问。
- 测试：固定 Clock 覆盖跨月、跨年、夏令时和中文相对日期。

#### DF-506 实现确认卡和编辑

- [ ] 状态
- 前置：DF-502、DF-505
- 复用：My Brain `MessageCard` 和现有 Assistant UI。
- 卡片必须显示：
  - 操作类型。
  - 标题或模板。
  - 完整日期时间及时区。
  - 日历目标。
  - 所有提醒。
  - 结构化字段值。
  - 将写入本地还是系统日历。
- 操作：编辑、确认、取消。
- 验收：取消后无写入；编辑后再次本地校验。

#### DF-507 实现 ProposalExecutor

- [ ] 状态
- 前置：DF-503、DF-504、DF-506
- 目标：唯一允许把已确认 proposal 交给现有写 use case 的组件。
- 规则：
  - proposal ID 单次消费。
  - 确认后重新校验。
  - 执行结果来自真实 repository。
  - 部分失败不得报告整体成功。
  - Calendar Provider 和本地 reminder 失败需要补偿或明确可恢复状态。
- 测试：重复确认、进程重建、权限撤销和 repository 失败。

#### DF-508 实现 AI 数据发送预览和日志脱敏

- [ ] 状态
- 前置：DF-501
- 能力：
  - 用户可查看即将发送的范围。
  - AI 总结只发送用户选择的模板、字段和时间范围。
  - API Key、内部路径和未选择的记录不进入 payload。
- 日志：
  - 默认不记录完整 prompt 和个人正文。
  - 网络日志在 release 关闭 body。
- 验收：自动测试扫描日志构造器和 backup DTO 不含 secret。

#### DF-509 实现 AI 统计总结

- [ ] 状态
- 前置：DF-303、DF-508
- 目标：把本地聚合后的紧凑摘要发送给 DeepSeek，不默认上传全部原始记录。
- 能力：
  - 时间范围和字段选择。
  - payload 预览。
  - 结果只作为文本建议，不作医学诊断。
- 验收：离线时统计仍可用，AI 失败不影响数据。

### P6：API Key、安全、备份和恢复

阶段出口：API Key 由 Keystore 保护；应用锁可用；tracking 数据进入现有备份；安全边界有测试。

#### DF-601 审计当前 secret 和日志路径

- [x] 状态
- 前置：DF-004
- 已知问题：My Brain 当前 OpenAI Key 位于普通 DataStore preference。
- 搜索：
  - `OPENAI_KEY`
  - request logging
  - exception reporting
  - backup serialization
  - Compose preview/test fixture
- 输出：`docs/SECURITY_CURRENT_STATE.md`
- 验收：所有 secret 读写点有清单。

#### DF-602 实现 KeystoreSecretStore

- [x] 状态
- 前置：DF-601
- 方案：
  - Android Keystore 内生成不可导出的 AES/GCM key。
  - DataStore 只保存版本、IV 和 ciphertext。
  - 使用 Android JCA 实现，不编写自定义密码算法。
  - 提供接口和 fake 实现供 JVM 测试。
- 行为：
  - 设置、读取、替换、删除。
  - key invalidation 时清除密文并提示重新输入。
  - 不要求 StrongBox。
- 验收：API Key 不以明文出现在 preferences 文件、日志和备份。

#### DF-603 迁移现有明文 API Key

- [x] 状态
- 前置：DF-602
- 流程：
  1. 检测旧值。
  2. 写入 SecretStore。
  3. 验证可解密。
  4. 删除旧 preference。
  5. 标记迁移完成。
- 失败：保留可恢复状态，不在日志打印 key。
- 测试：无旧 key、正常迁移、加密失败和重复启动。
- 完成记录（2026-06-13）：启动迁移在验证解密后删除旧 preference，失败时保留
  旧值并在下次启动重试；迁移标记保证重复启动幂等。

#### DF-604 扩展现有应用锁

- [ ] 状态
- 前置：DF-004
- 复用：My Brain `AppLockManager.kt` 和 `AuthScreen.kt`。
- 范围：
  - 设备凭据/生物识别。
  - 后台超时锁定。
  - 最近任务缩略图遮挡。
  - 不因锁屏中断已调度提醒。
- 不做：自定义密码数据库，除非现有能力无法覆盖用户需求。
- 验收：无生物识别设备仍可使用设备凭据或关闭应用锁。

#### DF-605 扩展 JSON 备份 DTO

- [ ] 状态
- 前置：DF-109
- 复用：
  - `JsonBackupData.kt`
  - ExportJson/ImportJson use cases
  - auto backup scheduler
- 加入：
  - templates
  - trackers
  - options
  - sessions
  - data points
  - reminders
- 排除：
  - API Key
  - Keystore ciphertext
  - 临时 proposal
- 格式：增加 schema version，保持旧备份可导入。
- 验收：完整往返测试和旧版本 fixture 测试通过。

#### DF-606 实现恢复预检和原子恢复

- [ ] 状态
- 前置：DF-605
- 能力：
  - 解析和版本校验。
  - 展示数量预览。
  - 在临时/事务边界验证。
  - 成功后替换或按现有策略合并。
  - 失败不破坏当前数据库。
- 验收：截断 JSON、未知版本、重复 ID 和无效 tracker type 均安全失败。

#### DF-607 发布版网络和隐私加固

- [ ] 状态
- 前置：DF-501、DF-602
- 项目设置：
  - release 禁止 cleartext HTTP。
  - 检查 exported components。
  - 禁止 Android 自动云备份敏感数据或明确配置 backup rules。
  - 不引入广告、跟踪和闭源分析 SDK。
- 验收：
  - manifest merger 报告人工检查。
  - release APK 依赖清单检查。

### P7：集成、体验和稳定性

阶段出口：主要用户流程连贯，已有 My Brain 功能无明显回归，性能和无障碍达到发布标准。

#### DF-701 构建统一“今天”视图

- [ ] 状态
- 前置：DF-208、DF-305、DF-408
- 内容：
  - 今日任务。
  - 今日系统日历事件。
  - 固定记录模板。
  - 今日记录摘要。
  - 待处理提醒。
- 导航：概览保持摘要定位，完整日期浏览统一进入 DF-008 日历主栏。
- 复用：现有 dashboard/card 组件。
- 性能：各数据源独立 Flow，避免一次阻塞加载。

#### DF-702 建立全局搜索

- [ ] 状态
- 前置：DF-207
- 范围：任务、日历事件、日记和记录 session。
- 复用：My Brain 搜索模式。
- 限制：首版只本地搜索，不把搜索词发送给 AI。
- 验收：中文、空格、日期范围和无结果状态。

#### DF-703 完成字符串、主题和无障碍

- [ ] 状态
- 前置：主要 UI 完成
- 要求：
  - 新字符串全部进入 resources。
  - 中文为首要验证语言，保留可翻译性。
  - 触控目标、TalkBack 描述和动态字体。
  - 颜色不能是唯一状态信号。
- 验收：大字体、深色模式和 TalkBack 核心流程手工测试。

#### DF-704 性能和数据库查询审计

- [ ] 状态
- 前置：DF-701
- 检查：
  - 主线程磁盘/网络。
  - N+1 查询。
  - 无界 Flow collect。
  - 一年和五年数据统计。
  - 大量 option 和 session。
- 工具：Android Studio profiler 或 Macrobenchmark；不引入运行时分析 SDK。
- 验收：核心页面无明显 jank，关键查询有索引。

#### DF-705 进程死亡和离线恢复

- [ ] 状态
- 前置：DF-507、DF-701
- 场景：
  - 快速记录未提交。
  - AI proposal 待确认。
  - 日历权限被撤销。
  - 网络中断。
  - 系统回收后返回。
- 原则：未确认 proposal 不自动执行；草稿按隐私设置决定是否保存。

#### DF-706 建立回归测试套件

- [ ] 状态
- 前置：P1-P6
- 必测：
  - 原 My Brain 任务创建/完成。
  - 原日历读写。
  - 原日记和笔记。
  - 应用锁。
  - 备份恢复。
  - tracking 八种控件。
  - 多提醒。
  - AI 确认隔离。
- 验收：关键流程至少有 JVM、instrumentation 或明确手工测试中的一种。

### P8：发布、F-Droid 和 v1.1 准备

阶段出口：可复现构建 release APK，许可证和隐私文档完整，具备 GitHub Beta 发布条件。

#### DF-801 依赖和许可证审计

- [ ] 状态
- 前置：功能冻结
- 检查：
  - 每个依赖许可证。
  - 是否从 Maven Central/Google 等 F-Droid 可接受来源获取。
  - 是否包含闭源 AAR、动态下载二进制或遥测。
  - SBOM 或依赖报告。
- 验收：阻塞 F-Droid 的依赖已移除或替换。

#### DF-802 建立 release 签名流程

- [ ] 状态
- 前置：DF-006
- 要求：
  - keystore 不进入仓库。
  - CI secret 最小权限。
  - 本地无 secret 时仍可构建 unsigned release。
  - 记录密钥备份和轮换流程，但不记录密码。
- 验收：签名 APK 可安装并验证证书。

#### DF-803 完善 GitHub Release

- [ ] 状态
- 前置：DF-802
- 产物：
  - APK
  - SHA-256
  - source archive
  - changelog
  - license notices
- 验收：从干净 checkout 可按文档重建。

#### DF-804 准备 F-Droid metadata

- [ ] 状态
- 前置：DF-801
- 内容：
  - app summary/description
  - screenshots
  - license
  - source/changelog/issues URL
  - build recipe
  - anti-feature 检查
- 注意：DeepSeek 是用户自带 API Key 的可选网络能力，核心本地功能无账户可用。

#### DF-805 Beta 验收

- [ ] 状态
- 前置：DF-803
- 环境：
  - 雷电模拟器用于快速回归。
  - 至少一台 Android 真实设备。
  - 尽可能增加官方 AVD。
- 观察：至少 1 至 2 周日常使用。
- 发布阻塞：
  - 数据丢失。
  - 错误 AI 写入。
  - 提醒明显漏发或重复。
  - API Key 泄露。
  - 备份不可恢复。

#### DF-806 定义 v1.1 数据库加密和加密备份技术验证

- [ ] 状态
- 前置：v1.0 Beta 稳定
- 仅做技术选型，不在 v1.0 临时加入。
- 检查：
  - F-Droid 兼容的 Room/SQLite 加密方案。
  - 密钥派生、密码丢失和迁移策略。
  - 大数据库性能。
  - 加密备份版本化容器。
- 要求：使用成熟开源密码实现，不自行设计协议。

## 11. 阶段依赖和建议里程碑

```text
P0 基线
 ├─ P1 数据层
 │   ├─ P2 录入 UI
 │   │   ├─ P3 统计与 CSV
 │   │   └─ P7 今日视图
 │   └─ P5 结构化记录 AI 工具
 ├─ P4 多提醒
 ├─ P5 DeepSeek 与确认
 └─ P6 安全与备份

P1-P6 完成
 └─ P7 集成与回归
     └─ P8 发布
```

建议可安装里程碑：

| 版本 | 内容 | 主要出口 |
|---|---|---|
| v0.1 | My Brain 基线和品牌 | 可构建、可安装 |
| v0.2 | tracking 数据层 | repository 测试通过 |
| v0.3 | 模板和快速记录 | 手工记录闭环 |
| v0.4 | 统计与 CSV | 趋势和导出可用 |
| v0.5 | 多重提醒 | 重启恢复和降级 |
| v0.6 | DeepSeek 提案 | 写入前确认 |
| v0.7 | Keystore 和备份 | secret/数据恢复验证 |
| v0.8 | 今日页和 widget | 主流程集成 |
| v0.9 | Beta | 回归、性能、真实设备 |
| v1.0 | 正式发布 | GitHub Release/F-Droid 提交 |

## 12. 测试策略

### 12.1 JVM 单元测试

优先覆盖：

- 类型验证。
- input 到 DataPoint 映射。
- 聚合和连续天数。
- 时间歧义和相对日期。
- reminder trigger 计算。
- proposal 状态机。
- backup DTO 往返。
- secret store 接口行为。

### 12.2 Room instrumentation 测试

覆盖：

- DAO 查询。
- session 事务。
- migration。
- 外键和索引。
- backup restore 原子性。

### 12.3 Compose UI 测试

覆盖：

- 八种动态控件。
- 模板编辑。
- 多提醒编辑。
- AI 确认卡。
- 应用锁。

### 12.4 端到端手工测试

覆盖：

1. 从语音输入法说“提醒我明天下午检查会议纪要”。
2. AI 因时间不明确追问。
3. 用户给出具体时间。
4. 确认卡显示事件时间和多个提醒。
5. 用户编辑后确认。
6. 日历、任务或记录真实写入。
7. 通知按计划触发。
8. 重启后提醒仍存在。

### 12.5 测试数据规模

- 空数据库。
- 1 个模板、1 个 session。
- 20 个模板、每个 20 个字段。
- 5 年每日数据。
- 同一多选 tracker 一次选择 20 个 option。
- 1000 个未来 reminder。

## 13. 安全验收清单

- [ ] API Key 不以明文存在于 DataStore。
- [ ] API Key 不进入 JSON/CSV 备份。
- [ ] release 日志不包含请求正文和授权 header。
- [ ] release 禁止 cleartext HTTP。
- [ ] AI 写工具不能直接获得 repository。
- [ ] 未确认 proposal 不产生任何持久化写入。
- [ ] proposal 只能执行一次。
- [ ] 恢复失败不破坏当前数据库。
- [ ] exported component 均有明确理由和权限。
- [ ] 应用后台时可遮挡最近任务缩略图。
- [ ] 无广告、追踪或闭源分析 SDK。
- [ ] 第三方代码来源和许可证完整。

## 14. 每个阶段的完成报告模板

Codex 完成一个阶段时应生成：

```markdown
## <阶段> 完成报告

完成任务：
- DF-...

复用：
- My Brain: ...
- Track & Graph: ...
- 其他开源依赖: ...

新增代码及原因：
- ...

数据库/接口变化：
- ...

验证：
- 命令：...
- 结果：...
- 设备：...

未解决风险：
- ...

下一阶段入口条件：
- ...
```

## 15. 需要用户再次审批的变更

出现以下情况时，Codex 必须先说明影响并等待批准：

1. 更换 My Brain 主干或整体合并 Track & Graph。
2. 引入闭源 SDK、账号体系、云服务或遥测。
3. 改变 GPLv3 许可方向。
4. 新增需要持续付费的服务。
5. AI 写操作改为无确认自动执行。
6. v1.0 提前加入数据库加密、CalDAV 或内置语音模型。
7. 放弃 Android 8。
8. 需要清空或不可逆迁移用户数据。
9. 新增高权限、后台常驻或无障碍服务。
10. 发布签名、API Key 或用户真实数据需要离开本机。

## 16. 第一轮开发建议

首次 Codex 开发会话只执行 `DF-001` 至 `DF-007`，不要同时开始 tracking 功能。该轮最终应交付：

- 完整主干源码。
- 可复现的 Debug 构建。
- 基线测试结果。
- 上游来源台账。
- CI。
- 明确的 applicationId 和品牌变更范围。

基线稳定且模拟器环境可用后，下一轮先执行 `DF-008`，随后按
`docs/PRODUCT_RESTRUCTURE_PLAN.md` 完成 Provider 和内容库纠偏，再进入
`DF-101`。每轮最多处理一个批次中的 2 至 4 个相邻任务，并在可安装里程碑
处生成 APK 进行设备验证。
