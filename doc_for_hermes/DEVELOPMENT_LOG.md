# Hermes Development Log

## 2026-06-27 — LDPlayer P9 Manual Test

- 按 `BETA_TEST_PLAN.md` 在 LDPlayer `emulator-5556` 上测试当前 Debug APK。
- 结论：当前构建已进入 P9 风格 UI，底栏为 `概览 / Items / Records / 设置`，旧测试计划中的
  `日历 / 空间` 标签已不再直接适用。
- 通过：启动安装、概览刷新、Daily Items 新建/筛选/搜索/月视图/完成、Records 快速记录保存、
  设置页基础导航。
- 发现：系统日历同步在无运行时权限时保存为 `FAILED` 且未主动引导授权；底部导航选中态图标过大；
  新增 Items UI 存在中英混排；旧 beta 测试计划需要重写。
- 测试报告：`LDPLAYER_TEST_REPORT_2026-06-27.md`。

---

## 2026-06-27 — P9 Unified Daily Items Plan

- 用户确认下一阶段不再继续堆叠孤立功能，而是进行体验重构：Calendar 与 Tasks 合并为
  Daily Flow 内部统一“事项”模块。
- 决策：Daily Flow 内部事项是主数据源；系统 Calendar Provider 仅作为 App -> 系统日历的
  可选同步目标，不反向覆盖 app 数据。
- 决策：首页 Dashboard 需要可编辑；第一版做面板显示/隐藏、上移/下移、范围和条数配置，
  拖拽布局后置。
- 决策：旧 Tasks 的范围筛选不单独实现，等统一事项模块落地后直接提供事项筛选。
- 决策：语音识别暂不进入开发计划，仅保留技术方案评估。
- 新增详细计划：`P9_UNIFIED_DAILY_ITEMS_PLAN.md`。

---

## 2026-06-19 — Data Dashboard Enhancements

- **DF-1001..1004**: 多 tracker 叠加折线、YEAR 范围、日历记录值显示和 Dashboard sparkline。
- **Fixes**: `buildTrackingDashboardState` 测试参数修正、tracking lint 缺失翻译修正。
- **Commits**: `9ca188d`, `8d8a6f`, `a04f430`, `0e34171`

---

## 2026-06-18 — Post Beta UX Experiments

- **DF-901..906**: Scale 字段修复、AI 悬浮入口、增强 AI prompt、番茄钟入口。
- **DF-910**: AI proposal confirmation dialog。
- **DF-000**: v0.1.0 品牌资产、用户指南和关于页个性化。
- **Commits**: `af945c0`, `504f7c`, `ba7102a`

---

## 2026-06-16 — P6 Security, Backup & Privacy Complete

- **DF-604**: App lock (biometric+credential) — already exists from MyBrain, verified
- **DF-605**: JSON backup extended with reminders (schema v2), `ReminderEntity` @Serializable
- **DF-606**: Import pre-check validation, atomic transaction, safe failure modes
- **DF-607**: Release network security config (no cleartext), privacy hardening
- **Verification**: :app:assembleDebug ✅, JVM tests ✅
- **Commit**: `e9a5c20`

---

## 2026-06-16 — P5 AI Proposal Infrastructure Complete

- **DF-501/502**: ActionProposal sealed model (6 types + ClarificationRequest)
- **DF-503**: Proposal-based TaskToolSet, CalendarToolSet, DiaryToolSet
- **DF-504**: TrackingToolSet with proposeCreateRecordSession + TrackingDataProvider
- **DF-505**: DateAmbiguityResolver for natural-language date/time
- **DF-506**: ConfirmationCard Compose UI
- **DF-507**: ProposalExecutor with single-execution guarantee
- **DF-508**: AiLogSanitizer for API key/PII redaction
- **DF-509**: summarizeStatistics tool in UtilToolSet
- **Fixes**: KSP disabled on ai:data (JVM variant issue), manual Koin modules
- **Verification**: :app:assembleDebug ✅, JVM tests ✅, LDPlayer no crashes ✅
- **Commits**: `894a605`, `dd872dc`, `2d6a317`

---

## 2026-06-16 — DF-4xx 全部完成

- DF-405: 任务多提醒迁移 (`98623e7`)
- DF-406: 日历事件提醒 (`79053e6`)
- DF-407: 记录提示通知 (`79053e6`)
- DF-408: MultiReminderEditor集成 (`7814ace`)
- LDPlayer截图验证全部通过 ✅

---

## 2026-06-15 — 项目初始分析

- PROJECT_UNDERSTANDING.md, HERMES_DEVELOPMENT_PLAN.md 创建
- Git 基线: `a413a36` (DF-404)

---

## 遗留项

| 任务 | 状态 | 备注 |
|------|------|------|
| P9 | 待实施 | 统一事项、App -> 系统日历同步、首页自定义 |
| AI proposal | 待复核 | 领域 `ProposalExecutor` 存在，但 Assistant 确认弹窗需确认是否真实调用执行器 |
| Pomodoro | 待完善 | 已有本地计时入口，前台服务/通知/持久化会话未闭环 |
| CI | 待验证 | DF-006 lint 修复已合入，远程 Runner 首次验证仍需外部 CI 状态 |
