# Daily Flow — 下一步开发计划

> 编制日期：2026-06-17  
> 来源：用户反馈 + 功能需求  
> 状态：已被 2026-06-27 P9 体验重构计划取代

---

## 2026-06-27 状态更新

本文件保留为 DF-901 至 DF-906 的历史需求记录。当前下一阶段开发计划已经迁移到：

```text
doc_for_hermes/P9_UNIFIED_DAILY_ITEMS_PLAN.md
```

用户已确认新的优先方向：

- Calendar 与 Tasks 合并为统一“事项”模块。
- Daily Flow 内部事项是主数据源，系统 Calendar Provider 仅作为可选同步目标。
- 首页 Dashboard 需要可编辑，第一版做显示/隐藏、上移/下移、范围和条数配置，拖拽布局后置。
- 语音识别暂不进入开发计划，仅保留技术方案评估。
- Tasks 的范围筛选不在旧 Tasks 模块单独实现，改为统一事项模块完成后提供“前后 7 天”等筛选。

DF-901 至 DF-906 已在提交 `ba7102ad` 合入一批实现；其中 Scale 修复、AI 悬浮入口和番茄钟入口
已有代码，番茄钟的前台服务、通知和持久化会话仍属于后续可选完善，不作为 P9 的前置条件。

## 1. Bug 修复（高优先级）

### DF-901 修复 Scale 字段浮点数显示与保存问题

- **现象**：自定义记录中「习惯次数」模板的心情（Scale 类型）字段，当值为 4 或 7 时，显示为浮点数形式，且处于浮点数状态时无法保存。
- **根因推测**：Scale 滑块控件使用 `Float` 或 `FloatRange` 作为底层值类型，在某些步长/精度计算下产生 `4.0000000001` 之类浮点误差，导致显示异常；保存时可能做了整数校验（`value % 1 != 0`）而拒绝。
- **修复方向**：
  1. 审查 Scale 字段的 ViewModel / Composable 中的值转换逻辑
  2. 确保滑块值始终 `roundToInt()` 后再用于显示
  3. 保存时对 Scale 值做 `roundToInt()` 或 `toInt()` 归一化
  4. 检查其他数值字段（Number、Duration）是否有类似精度问题
- **验收**：所有 Scale 步长值（1-10）显示为整数，不出现浮点数，均可正常保存。

### DF-902 修复 Scale 滑块无法拖到最小值

- **现象**：心情（Scale 1-10）滑块从起始位置无法直接拖到 1，必须先拖到 2 再拖回 1。
- **根因推测**：`Slider` 的 `valueRange` 或手势处理有边界问题。可能是 Compose `Slider` 的 `onValueChangeFinished` 与初始值 0/1 的交互问题，或者自定义滑条组件的触摸区域计算有偏移。
- **修复方向**：
  1. 定位心情模板使用的 Scale 控件代码
  2. 检查 `Slider` 的 `valueRange` 是否正确设为 `1f..10f`（而非 `0f..10f`）
  3. 检查是否有 `coerceIn` 或 `coerceAtLeast` 阻止了边界值
  4. 在所有 8 种字段类型上做滑块边界回归测试
- **验收**：滑块可从任意位置直接拖到 1，也可直接拖到 10，无死区。

---

## 2. AI 助手增强

### DF-903 全局 AI 悬浮按钮

- **目标**：用户开启 AI Assistant 后，在应用任意页面显示一个悬浮按钮（FAB / Floating Bubble），点击直接进入 AI 对话界面。
- **技术要求**：
  1. 悬浮按钮通过 Compose `FloatingActionButton` 或自定义 `Popup`/`Box` 叠加在 `Scaffold` 之上
  2. 仅在 AI Provider 已配置且 API Key 已设置时显示
  3. 可拖拽到屏幕边缘（类似 Android 无障碍菜单）
  4. 点击后导航到 `AssistantScreen`，保留当前页面栈（以便返回时回到原页面）
  5. 在 AI 对话页面上不重复显示（避免遮挡输入框）
- **验收**：AI 配置完成后，概览/日历/空间/设置各页面均可看到悬浮按钮；点击进入 AI 对话；返回后回到原页面；未配置 AI 时不显示。

### DF-904 AI 全面内容创建支持

- **背景**：当前 AI 已具备 TaskToolSet、CalendarToolSet、DiaryToolSet、NoteToolSet、BookmarkToolSet、TrackingToolSet 六大工具集，理论上已支持所有内容类型的创建。但实际能力如下：

| 工具集 | 搜索 | 创建 | 状态 |
|--------|------|------|------|
| TaskToolSet | ✅ searchTasks | ✅ createTask / createMultipleTasks / updateTaskCompleted | 已完成 |
| CalendarToolSet | ✅ searchEvents / getEventsWithinRange | ✅ createEvent / createEvents | 已完成 |
| DiaryToolSet | ✅ searchDiaryEntries | ✅ createDiaryEntry | 已完成 |
| NoteToolSet | ✅ searchNotes | ✅ createNote / createMultipleNotes | 已完成 |
| BookmarkToolSet | ✅ searchBookmarks | ✅ createBookmark | 已完成 |
| TrackingToolSet | ✅ getRecordTemplates / searchRecordSessions | ⚠️ proposeCreateRecordSession（仅返回 proposal JSON） | 需完善 |
| ContentLibrary | ❌ 无 | ❌ 无统一入口 | 需新增 |

- **缺口**：
  1. **TrackingToolSet.proposeCreateRecordSession** 目前只返回 proposal JSON，实际创建流程（解析字段值、验证、保存）不完整
  2. **内容库**（笔记/日记/书签）缺少统一 AI 工具入口，模型需要分别调用不同 ToolSet
  3. AI 对话中没有引导用户选择创建类型的提示
- **实施**：
  1. 完善 `TrackingToolSet.proposeCreateRecordSession`：接收字段名-值的映射，生成完整的 `CreateRecordSessionProposal`
  2. 新增 `ContentLibraryToolSet`（或增强 system prompt 引导模型自行选择正确工具集）
  3. AI system prompt 中增加内容创建指引，告知模型何时使用哪个工具集
  4. 验证端到端流程：用户说"帮我记录今天健身 60 分钟" → AI 调用 getRecordTemplates → 找到"健身"模板 → proposeCreateRecordSession → 确认卡 → 写入
- **验收**：用自然语言可创建所有 6 种内容类型，确认卡正确显示待创建内容，确认后成功写入数据库。

---

## 3. 番茄钟功能

### DF-905 番茄钟核心功能

- **目标**：在「空间」页新增番茄钟入口，提供标准番茄工作法计时。
- **功能规格**：
  1. **计时模式**：
     - 默认：25 分钟专注 + 5 分钟短休息
     - 每 4 个番茄钟后：15 分钟长休息
     - 用户可自定义专注时长（5-60 分钟）、短休息（1-15 分钟）、长休息（5-30 分钟）
  2. **UI**：
     - 圆形倒计时显示（类似时钟），剩余时间大字居中
     - 开始/暂停/重置按钮
     - 当前阶段标签（专注中 / 短休息 / 长休息）
     - 跳过按钮（直接进入下一阶段）
  3. **通知**：
     - 计时结束时播放提示音 + 通知
     - 通知带"开始休息"/"开始专注"快捷操作
  4. **后台运行**：
     - 使用 Foreground Service 保持计时（通知栏显示倒计时）
     - 应用切换到后台不中断
- **技术方案**：
  - 计时核心：Kotlin `Flow` + `ticker` 或 `CountDownTimer`
  - 持久化：Room 表记录番茄钟会话（开始时间、结束时间、类型、是否完成）
  - 前台服务：`PomodoroService`（Foreground Service + Notification）
- **验收**：完整 25+5 分钟番茄钟周期正常；通知触发；后台不中断；自定义时长生效。

### DF-906 番茄钟每日统计

- **目标**：在番茄钟页面 / 概览页显示当日和历史的番茄钟统计。
- **功能规格**：
  1. 今日完成番茄钟数（大数字醒目显示）
  2. 今日总专注时长
  3. 本周每日番茄钟数柱状图
  4. 历史记录列表（日期、完成数、总时长）
- **验收**：完成番茄钟后统计数据实时更新；历史数据可回溯。

---

## 4. 探索性议题

### EXP-001 内置语音识别的可行性

**议题**：在当前版本中增加内置语音转文字功能，使用户可通过语音输入任务、日记和记录内容。

**现状分析**：
- 当前开发计划（`DEVELOPMENT_PLAN.md` §4.1）已列出"使用手机本地输入法完成语音转文字"，即依赖系统输入法的语音按钮，不内置模型
- Android 提供 `SpeechRecognizer` API（需 Google 服务）和 `android.speech` 包

**方案对比**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. 系统输入法语音 | 零集成成本，用户已熟悉 | 依赖 GBoard/系统输入法；离线不可用 |
| B. Android SpeechRecognizer | 系统级 API，可自定义 UI | 需要 Google App；仅在线；中国大陆不可靠 |
| C. Whisper 本地模型 | 完全离线，隐私好，中文支持好 | 模型包大（~150MB tiny, ~500MB small）；推理延迟 |
| D. Vosk 离线引擎 | 轻量（~50MB），实时流式 | 中文准确率低于 Whisper |

**建议**：
- **v1.1 不纳入内置模型**（工程量和包体积代价大）
- 短期：在 AI 助手的输入框旁增加**显式麦克风按钮**，拉起系统输入法的语音输入
- v1.2+：调研 `whisper.cpp` Android 集成（已有开源示例），评估 tiny 模型在 2024+ 手机上的推理速度

**决策点**：
- [ ] 同意 v1.1 仅增加麦克风按钮调用系统语音
- [ ] 同意 v1.2+ 再评估 Whisper 本地集成

---

### EXP-002 迁移到鸿蒙和 iOS 的可行性

**议题**：评估将 Daily Flow 迁移到 HarmonyOS 和 iOS 平台的可行性和代价。

#### 鸿蒙 (HarmonyOS)

**现状**：鸿蒙 Next (5.0+) 已不再兼容 Android APK，需要原生 ArkTS 开发。

| 维度 | 评估 |
|------|------|
| 语言 | Kotlin → ArkTS (TypeScript 类似) |
| UI | Jetpack Compose → ArkUI（声明式，概念相似） |
| 数据库 | Room → 鸿蒙关系型数据库 (RDB) |
| DI | Koin → 无直接等价物，需手工 DI 或用 InversifyJS |
| 网络 | Ktor → @ohos.net.http |
| 提醒 | AlarmManager → @ohos.reminderAgentManager |
| 日历 | Calendar Provider → calendarManager |
| 安全 | Android Keystore → 鸿蒙通用密钥库 (HUKS) |
| **工程估算** | 完全重写，约 **8-12 人月**（含学习曲线） |

**关键障碍**：
- 所有 Kotlin 代码不可复用，需完全重写
- Koog Agent 框架无鸿蒙版，AI 工具调用需自建
- 生态不成熟，第三方库稀缺

**建议**：鸿蒙迁移**暂不启动**。等待鸿蒙生态成熟（1-2 年），观察用户需求变化后重新评估。

#### iOS

| 维度 | 评估 |
|------|------|
| 语言 | Kotlin → Swift |
| UI | Jetpack Compose → SwiftUI（概念高度相似） |
| 数据库 | Room → SwiftData / GRDB / Core Data |
| DI | Koin → Swift 手动 DI 或 Factory |
| 网络 | Ktor → Alamofire / URLSession |
| 跨平台 | 可考虑 Kotlin Multiplatform (KMP) 共享 domain/data 层 |

**策略选项**：

| 方案 | 说明 | 工程估算 |
|------|------|----------|
| A. 纯原生 Swift | 重写全部代码 | 10-14 人月 |
| B. KMP 共享层 | domain/data 用 KMP，UI 原生 SwiftUI | 8-10 人月 |
| C. Compose Multiplatform | 共享全部 Kotlin 代码（含 UI） | 6-8 人月（实验性） |

**建议**：
- 短期不启动原生 iOS 迁移
- v1.1 后调研 **Compose Multiplatform for iOS**（JetBrains 已发布 beta），评估 Daily Flow 实际兼容性
- 如 CMP 实验可行，可在 3-6 个月内实现 iOS Alpha

**决策点**：
- [ ] 同意鸿蒙暂不启动
- [ ] 同意 iOS 暂不启动，v1.1 后做 CMP 技术验证（1 周 Spike）

---

## 5. 建议执行顺序

```text
当前 ──→ DF-901/902 (Bug 修复, 1-2h)
           │
           ▼
       DF-903 (AI 悬浮按钮, 半天)
           │
           ▼
       DF-904 (AI 全面创建, 1-2天)
           │
           ▼
       DF-905/906 (番茄钟, 2-3天)
           │
           ▼
       EXP-001/002 技术验证 (按审批结果)
```

---

## 6. 待审批

1. [ ] 同意 DF-901~906 进入开发排期（Bug 修复 + AI 增强 + 番茄钟）
2. [ ] 同意 EXP-001 方案：v1.1 仅加麦克风按钮，v1.2 再评估 Whisper
3. [ ] 同意 EXP-002 方案：鸿蒙/iOS 暂不启动，v1.1 后做 CMP Spike
