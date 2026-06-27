# Daily Flow 下一轮代码审查前提文档

更新日期：2026-06-27
用途：供下一次对话开始时，作为 Codex/Hermes 进行代码审查的必要上下文。
审查目标：在继续 P9 开发或审查 P9 实现前，先准确理解当前仓库状态、计划边界、已知风险和应优先核验的代码路径。

---

## 1. 当前仓库状态

- 当前分支：`main`
- 当前 HEAD：发布 v0.11.0 后应为 P9 统一事项提交；若不一致，以 `git log -1 --oneline` 为准。
- 当前远端状态：发布后应与 `origin/main` 对齐。
- 当前数据库版本：Room v9。
- 当前应用版本：`versionName = "0.11.0"`，`versionCode = 11`，release applicationId 为 `com.dailyflow.app`，debug 后缀为 `.debug`。
- P8 之后已有继续迭代提交：
  - `DF-901..906`：Scale 修复、AI 悬浮入口、增强 AI prompt、番茄钟入口。
  - `DF-910`：AI proposal confirmation dialog。
  - `DF-1001..1004`：多 tracker 图表、YEAR 范围、日历 tracking 值、Dashboard sparkline。
- 2026-06-27 新增/更新了一批 P9 代码、计划文档、测试报告和发布文档；发布 v0.11.0 后这些内容应已提交。

可能存在的未提交文档变更：

```text
docs/DEVELOPMENT_PROGRESS.md
docs/NEXT_CODE_REVIEW_CONTEXT.md
doc_for_hermes/CHANGELOG.md
doc_for_hermes/DEVELOPMENT_LOG.md
doc_for_hermes/NEXT_DEVELOPMENT_PLAN.md
doc_for_hermes/P9_UNIFIED_DAILY_ITEMS_PLAN.md
doc_for_hermes/LDPLAYER_TEST_REPORT_2026-06-27.md
```

如果下一轮对话开始时这些文件仍未提交，请先运行：

```powershell
git status --short
git diff --check
```

不要误把这些文档变更当作用户无关改动回滚。

---

## 2. 必读文档顺序

下一轮代码审查前，按以下顺序阅读：

1. `docs/DEVELOPMENT_PROGRESS.md`
   - 获取当前里程碑、P9 状态和已知风险。
2. `doc_for_hermes/P9_UNIFIED_DAILY_ITEMS_PLAN.md`
   - 获取 P9 的详细产品决策、数据结构、迁移、提醒、日历同步、Dashboard 自定义和任务拆分。
3. `doc_for_hermes/DEVELOPMENT_LOG.md`
   - 获取 Hermes/另一个 Agent 的阶段性提交和遗留项。
4. `doc_for_hermes/CHANGELOG.md`
   - 获取 beta、P8 和 post-beta UX 实验范围。
5. 本文件：`docs/NEXT_CODE_REVIEW_CONTEXT.md`
   - 获取审查优先级和注意事项。

---

## 3. 下一阶段真实方向

P9 的核心方向已经由用户确认：

- Calendar 与 Tasks 合并为统一“事项”模块。
- Daily Flow 内部事项是主数据源。
- 系统 Calendar Provider 只是 App -> 系统日历的可选同步目标。
- 不做系统日历 -> app 的默认反向同步。
- 首页 Dashboard 需要可编辑。
- 第一版 Dashboard 自定义只做显示/隐藏、上移/下移、范围和条数配置；拖拽布局后置。
- 旧 Tasks 的范围筛选不单独开发，必须等统一事项模块完成后，在“事项”模块中实现。
- 语音识别暂不纳入 P9 开发计划，只保留方案评估。

审查时如果发现实现偏离这些决策，应作为高优先级问题提出。

---

## 4. 代码审查优先级

下一轮如果用户要求“审查 P9 实现”或“继续开发后审查”，默认采用代码审查姿态：

1. 数据丢失风险
2. Room 迁移错误
3. 旧 Tasks/Reminders 迁移重复或丢失
4. Calendar Provider 同步方向错误
5. 提醒重复、漏发、完成后未取消
6. Dashboard 配置导致首页崩溃
7. 备份/恢复破坏新表或同步状态
8. 权限撤销、系统日历删除、外部修改等失败路径
9. UI 状态与领域状态不一致
10. 测试缺口

审查输出应先列 findings，按严重性排序，并附文件和行号。

---

## 5. P9 审查重点清单

### 5.1 DailyItem 领域模型

检查点：

- `DailyItem` 是否能表达 task-like、event-like、no-date plan。
- `endAt` 是否禁止早于 `startAt`。
- `COMPLETED` 是否必须有 `completedAt`。
- no-date item 是否合法。
- 时区字段是否明确，不应把本地墙上时间直接丢失。
- recurrence 是否复用现有能力，未擅自引入复杂 RRULE 引擎。

高风险信号：

- 用 nullable 字段堆逻辑但没有 validator。
- status 与 completedAt 可互相矛盾。
- due-only item 被强制写入系统日历。

### 5.2 Room v8 -> v9 迁移

检查点：

- `MyBrainDatabase.version` 是否从 8 提升到 9。
- `MIGRATION_8_9` 是否注册到 `DatabaseModule`。
- v9 schema 是否导出。
- `daily_items`、`daily_item_calendar_sync`、`dashboard_panels` 是否存在。
- 旧 `tasks` 表是否保留。
- 旧 Task 是否使用同一 ID 迁移为 DailyItem。
- Task reminders 是否安全 retarget 到 `DAILY_ITEM`。
- 迁移是否 idempotent 或配合 startup reconciliation 保证不重复。

必须有的测试：

- 空 v8 -> v9。
- 有未完成 task。
- 有已完成 task。
- 有 dueDate + reminder 的 task。
- 无 dueDate task。
- 重复启动/重复 reconcile 不产生重复 DailyItem。

高风险信号：

- 删除 `tasks` 表。
- 默认清空 `reminders`。
- 把系统日历事件批量导入 DailyItem。
- 没有 migration test 但改了 Room version。

### 5.3 统一提醒

检查点：

- `ReminderTargetType.DAILY_ITEM` 是否加入。
- resolver 是否按 `startAt -> dueAt -> null` 解析目标时间。
- completed/cancelled/archived item 是否不会继续保留未来提醒。
- notification tap 是否进入 DailyItem details。
- notification complete action 是否调用 DailyItem completion use case。
- 相对提醒是否禁止绑定 no-date item，或保持 PENDING 并给 UI 提示。

高风险信号：

- 仍默认创建 `TASK` target reminder。
- 完成事项只改 UI，不取消系统 alarm/fallback work。
- provider event reminder 与 app reminder 混用。

### 5.4 Calendar Provider 单向同步

P9 的同步方向只能是：

```text
DailyItem -> Android Calendar Provider
```

检查点：

- 创建 DailyItem 时默认不写系统日历，除非用户启用 sync。
- 修改 DailyItem 后可更新 provider event。
- 外部修改 provider event 不应自动覆盖 DailyItem。
- 外部删除 provider event 不应删除 DailyItem。
- 权限失败时 DailyItem 不丢失，只标记 sync failed/unlinked。
- sync fingerprint 或等价机制是否能检测 external drift。

高风险信号：

- 观察系统日历并自动创建/覆盖 DailyItem。
- 把 Calendar Provider 事件当作主数据。
- 同步失败后回滚/删除 DailyItem。
- 完成事项时删除 provider event，除非用户明确选择。

### 5.5 事项显示与筛选

检查点：

- 范围筛选是否实现在 Daily Items 模块，而不是旧 Tasks 页面。
- 必须支持：
  - 今日
  - 前后 7 天
  - 未来 7 天
  - 本周
  - 本月
  - 逾期
  - 无日期
  - 已完成
  - 全部
  - 自定义范围
- “前后 7 天”应包含当前日期前 7 天至后 7 天，并合理携带逾期未完成项。
- 排序应稳定，不能因 Flow 重组导致列表乱跳。

高风险信号：

- 只给旧 Tasks 加筛选，没有统一事项。
- all-day/timezone 边界按 UTC 日期处理。
- 逾期项在前后 7 天视图中消失。

### 5.6 Dashboard 自定义

检查点：

- 是否有持久化 panel 配置。
- 默认 panel 是否只初始化一次。
- 用户修改顺序/显示状态后重启仍保留。
- malformed config 是否 fallback，不应崩溃首页。
- Custom Tracking 快速记录能被固定到首页。
- Daily Items panel 能配置 Today 或 +/- 7 days。

高风险信号：

- Dashboard 配置存到 transient ViewModel。
- config JSON 解析失败直接崩溃。
- reset defaults 覆盖用户配置且无确认。

### 5.7 Backup / Restore

检查点：

- Daily Items、calendar sync mapping、Dashboard panels 是否进入 JSON backup。
- restore 后 provider IDs 是否被视为 stale/unlinked，而不是直接写系统日历。
- restore preview 是否显示 Daily Items 和 stale sync links 数量。
- restore 失败是否不破坏现有数据库。

高风险信号：

- restore 时自动写 Calendar Provider。
- provider event ID 跨设备直接复用。
- 备份遗漏 reminders retarget 后的新 target。

---

## 6. 当前已知需复核但非 P9 前置的问题

### 6.1 AI Proposal

事实：

- 领域层 `ProposalExecutor` 已存在。
- Assistant confirmation dialog 已存在。

需复核：

- Confirm 按钮是否真正调用 `ProposalExecutor.execute(...)`。
- 是否只是把确认消息写回聊天。

除非用户要求，不要在 P9 审查中扩大为 AI 大重构。

### 6.2 Pomodoro

事实：

- 已有 Pomodoro 页面和本地 ViewModel 计时。

需复核：

- 没有看到完整 Foreground Service、通知和 Room session 持久化。

这不是 P9 前置。

### 6.3 记录提示

事实：

- `RECORD_PROMPT` 通知和 quick record deep-link 已有。

需复核：

- 模板级周期配置入口是否完整。

这不是 P9 前置。

---

## 7. 建议审查命令

代码审查前快速状态：

```powershell
git status --short
git log -8 --oneline --decorate
git diff --stat
git diff --check
```

快速定位 P9 相关实现：

```powershell
rg -n "DailyItem|daily_items|DashboardPanel|MIGRATION_8_9|DAILY_ITEM|CalendarSync|SyncDailyItem|SurroundingSevenDays|FutureSevenDays" -S .
```

Room 相关：

```powershell
rg -n "version =|MIGRATION_8_9|addMigrations|daily_items|dashboard_panels" core app daily -S
```

提醒相关：

```powershell
rg -n "ReminderTargetType|DAILY_ITEM|ReminderReceiver|NotificationUtil|TargetTimeResolver|CompleteDailyItem" core app daily -S
```

Calendar Provider 同步：

```powershell
rg -n "CalendarContract|systemEventId|systemCalendarId|EXTERNAL_DRIFT|CalendarSyncState|SyncDailyItem" calendar app daily -S
```

Dashboard 自定义：

```powershell
rg -n "DashboardPanel|dashboard_panels|DashboardEdit|QuickRecordPanel|DailyItemsPanel" app daily tracking -S
```

---

## 8. 建议验证命令

构建时优先使用外部 build root，避免同步盘文件锁：

```powershell
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" --no-problems-report :app:assembleDebug
```

模块测试按实际变更选择：

```powershell
.\gradlew.bat --no-problems-report :core:alarm:testDebugUnitTest
.\gradlew.bat --no-problems-report :core:database:connectedDebugAndroidTest
.\gradlew.bat --no-problems-report :tracking:testDebugUnitTest
```

如果新增 `daily` 模块：

```powershell
.\gradlew.bat --no-problems-report :daily:testDebugUnitTest
.\gradlew.bat --no-problems-report :daily:connectedDebugAndroidTest
```

Release smoke:

```powershell
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" --no-problems-report :app:assembleRelease
```

---

## 9. LDPlayer 审查前提

使用项目脚本启动雷电：

```powershell
powershell -File "tools/android/ldplayer.ps1" -Action start
```

环境参考：

- 文档：`doc_for_hermes/ENVIRONMENT_REFERENCE.md`
- 设备：LDPlayer 9 / Android 9 API 28
- serial：通常为 `emulator-5556`
- debug 包名：`com.dailyflow.app.debug`

P9 手动回归至少覆盖：

1. 升级后旧任务出现在 Daily Items。
2. 旧任务提醒不重复、不丢失。
3. 创建 app 内事项，不同步系统日历。
4. 创建同步事项，系统 Calendar Provider 可见。
5. 修改同步事项，系统日历更新。
6. 外部删除系统事件，Daily Item 不丢失。
7. 完成事项，未来提醒取消。
8. 前后 7 天筛选正确。
9. 首页 Dashboard 编辑、重启后配置保留。
10. logcat 无 crash/fatal/ANR。

---

## 10. 审查输出格式建议

如果发现问题，按如下格式输出：

```text
Findings

1. [P0/P1/P2] 标题
   文件:行号
   问题：
   影响：
   建议：

Open Questions

- ...

Verification

- 已运行：
- 未运行及原因：
```

优先报告 bug、数据风险、迁移风险和测试缺口。不要把风格偏好放在真正风险之前。

---

## 11. 不应误判的事项

- 语音识别暂不属于 P9 实施范围。
- 拖拽 Dashboard 布局暂不属于 P9 第一版范围。
- 系统日历反向同步不是目标。
- 旧 Tasks 页面无需先加范围筛选。
- Calendar Provider 事件不应被默认导入为 DailyItem。
- Pomodoro 前台服务不是 P9 前置。
- AI proposal 审计重要，但不要在没有用户要求时阻塞 P9 数据/事项审查。

---

## 12. 下一轮对话推荐开场

下一轮如果用户要求代码审查，可以先执行：

```powershell
git status --short
git diff --stat
rg -n "DailyItem|daily_items|DashboardPanel|MIGRATION_8_9|DAILY_ITEM|CalendarSyncState" -S .
```

然后阅读：

```text
doc_for_hermes/P9_UNIFIED_DAILY_ITEMS_PLAN.md
docs/NEXT_CODE_REVIEW_CONTEXT.md
docs/DEVELOPMENT_PROGRESS.md
```

再开始审查实现。
