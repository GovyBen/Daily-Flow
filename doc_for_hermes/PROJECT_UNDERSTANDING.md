# Daily Flow — 项目理解文档

> 编制者：Hermes Agent  
> 编制日期：2026-06-15  
> 用途：记录 Hermes 对项目的完整理解，作为后续开发的参考基线

---

## 1. 项目概述

**Daily Flow** 是一款完全开源的 Android 日程与生活事件记录应用，目标是将任务管理、日历事件、结构化习惯/事件记录、统计分析和 AI 辅助整合在单一应用中。

- **许可证**：GPLv3
- **开源主干**：Fork 自 [My Brain](https://github.com/mhss1/MyBrain) v3.1.0
- **算法参考**：选择性移植自 [Track & Graph](https://github.com/SamAmco/track-and-graph)
- **发布目标**：GitHub Release APK + F-Droid

---

## 2. 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room（当前 version=8） |
| DI | Koin |
| 并发 | Kotlin Coroutines + Flow |
| 网络 | Ktor（OpenAI-compatible 协议） |
| AI | Koog Agent + DeepSeek（及其他兼容 Provider） |
| 提醒 | AlarmManager + WorkManager |
| 日历 | Android Calendar Provider |
| 图表 | AndroidPlot 1.5.11（通过 Compose AndroidView） |
| CSV | Apache Commons CSV 1.14.1 |
| 安全 | Android Keystore (AES/GCM) |
| 最低版本 | Android 8 / API 26 |
| 编译 SDK | 36 |
| 构建 | Gradle 8.13, AGP 8.13.2, Kotlin 2.3.10, JDK 17 |

---

## 3. 项目架构

### 3.1 模块结构

```
Daily_Flow/
├── app/                    # 主应用模块（导航、主页、Application）
├── tasks/                  # 任务模块（domain/data/presentation）
├── calendar/               # 日历模块
├── diary/                  # 日记模块
├── notes/                  # 笔记模块
├── bookmarks/              # 书签模块
├── settings/               # 设置模块
├── tracking/               # ★ 新增：结构化记录模块
│   ├── domain/             # 模板、字段、记录、建议值
│   ├── data/               # Room 实体、DAO、Repository、CSV
│   ├── presentation/       # 模板编辑、快速记录、历史、统计、CSV
│   └── analytics/          # 聚合、分箱、样本适配、图表
├── ai/                     # AI 模块（data/domain/presentation）
├── core/
│   ├── alarm/              # 提醒核心（统一提醒模型 + 调度）
│   ├── database/           # 主数据库（含 tracking 表）
│   ├── notification/       # 通知基础设施
│   ├── preferences/        # DataStore 偏好（含 SecretStore）
│   ├── ui/                 # 通用 UI 组件
│   ├── util/               # 测试夹具和工具
│   ├── widget/             # 桌面小组件核心
│   └── di/                 # Koin 模块定义
└── widget/                 # 桌面小组件模块
```

### 3.2 分层约定

```
presentation/  →  Compose UI, ViewModel（不访问 DAO/网络）
domain/        →  Use case, Repository 接口, 领域模型
data/          →  Room 实现, DAO, 网络客户端
```

---

## 4. 当前开发进度

### 已完成阶段

| 阶段 | 任务 | 说明 |
|------|------|------|
| **P0** | DF-001~007 | 主干导入、构建基线、品牌、CI、测试夹具 |
| **P0.5** | DF-008~015 | 日历主导航、AI Provider 预设、内容库统一入口、SecretStore |
| **P1** | DF-101~110 | tracking 数据层：实体、DAO、验证器、映射器、仓库、迁移、种子模板 |
| **P2** | DF-201~210 | 模板编辑、快速记录、历史、导航接入、桌面小组件、日历整合 |
| **P3** | DF-301~306 | 统计、图表、CSV 导入导出 |
| **P4 (部分)** | DF-401~404 | 提醒审计、调度降级、统一提醒实体、重算与同步 |

### 当前在推进

- **DF-405**：任务多提醒迁移（将旧的单 `dueDate` 即提醒模式迁移到新的多提醒统一模型）

### 后续待完成（按阶段）

- **P4 剩余**：DF-406（日历事件多提醒）、DF-407（记录提示）、DF-408（多提醒编辑组件）
- **P5**：DF-501~509（DeepSeek 意图识别、确认卡、执行器）
- **P6**：DF-604~607（应用锁、备份扩展、隐私加固）
- **P7**：DF-701~706（今日视图、全局搜索、无障碍、性能、进程恢复、回归测试）
- **P8**：DF-801~806（许可证审计、签名、GitHub Release、F-Droid、Beta 验收、v1.1 规划）

### 已具备能力（摘要）

- 四栏主导航（概览、日历、空间、设置）
- 任务 CRUD、重复规则、日历事件读写
- 八种结构化记录字段（多选、单选、计数、滑条、布尔、时长、数值、文本）
- 模板管理（创建、复制、排序、停用、固定）
- 快速记录、历史查询、编辑、删除（含撤销）
- 统计（日/周/月聚合、连续天数、选项分布、折线/柱状/饼图）
- CSV 导入导出（含预览校验）
- 桌面小组件快捷记录
- 统一提醒数据模型与调度基础设施（DF-404）
- DeepSeek/OpenAI/通义千问/Kimi/智谱预设 Provider
- API Key Keystore 加密存储
- 雷电模拟器冒烟测试环境

---

## 5. 关键产品原则

1. **AI 写操作必须确认**：模型只能生成 `ActionProposal`，用户确认后才写入
2. **时间歧义必须追问**：缺少具体时间、提醒提前量等时追问
3. **软删除保护历史**：已使用的模板/字段/选项只停用不物理删除
4. **数据快照**：数据点保存选项 label 和数值快照，避免重命名破坏历史
5. **本地优先**：无网络时所有核心功能可用（除 AI）
6. **安全存储**：API Key 使用 Android Keystore (AES/GCM)，不进入日志/备份/DataStore 明文
7. **不引入闭源 SDK**：无广告、无追踪、无分析 SDK

---

## 6. 开发环境

### 宿主机
- OS：Windows 11 amd64
- JDK：Oracle 17.0.12 (`C:\Program Files\Java\jdk-17`)
- 工作区：`C:\Users\10844\Documents\BaiduSyncdisk\Codex Projects\Daily_Flow`
- ⚠️ 工作区位于百度同步盘，构建需使用外部 build root

### 模拟器
- LDPlayer 9.1.80.0
- 实例名：`Daily Flow Test`（index=1）
- Android：9 / API 28, x86_64
- ADB serial：`emulator-5556`
- 语言/时区：zh-CN / Asia/Shanghai
- ⚠️ 雷电为 API 28，无法验证 Android 12+ exact alarm 和 Android 13+ notification 权限

### 构建命令（PowerShell）

```powershell
# 使用外部 build root 避免同步盘文件锁冲突
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" :app:assembleDebug
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" testDebugUnitTest
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" lintDebug

# 模块级测试
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" :tracking:testDebugUnitTest
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" :core:alarm:testDebugUnitTest

# 冒烟测试
.\tools\android\smoke.ps1
```

### Git 仓库

- 当前分支：`main`
- 最新提交：`a413a36` — DF-404 提醒重算与同步
- 上游 remote：
  - `upstream-mybrain` → https://github.com/mhss1/MyBrain
  - `upstream-trackgraph` → https://github.com/SamAmco/track-and-graph

---

## 7. 代码来源分级

| 类别 | 含义 | 说明 |
|------|------|------|
| A | My Brain 原样保留 | 尽量不修改 |
| B | My Brain 小范围修改 | 保持原结构，测试修改行为 |
| C | Track & Graph 移植或改写 | 记录原路径、提交和版权 |
| D | Daily Flow 新胶水/新需求代码 | 必须说明复用缺口 |
| E | 新增开源依赖 | 许可证、版本和 F-Droid 审核 |

---

## 8. 测试约定

- 测试不得依赖当前日期、系统时区或随机 UUID
- 使用 `core:util` 中的 `TestClock`、`TestIdGenerator`、`TestZone`
- 先编写/移植测试，再实现行为
- 提交前运行模块级 JVM 测试、lint 和编译

---

## 9. 关键文档索引

| 文档 | 位置 | 内容 |
|------|------|------|
| 产品开发计划 | `docs/DEVELOPMENT_PLAN.md` | v1.0 范围、架构、数据模型 |
| Codex 实施计划 | `docs/CODEX_IMPLEMENTATION_PLAN.md` | 逐任务实施步骤 |
| 开发进度 | `docs/DEVELOPMENT_PROGRESS.md` | 已完成/进行中任务 |
| 产品结构修订 | `docs/PRODUCT_RESTRUCTURE_PLAN.md` | 导航、Provider、内容库决策 |
| 提醒现状 | `docs/REMINDER_CURRENT_STATE.md` | 提醒链路审计 |
| 安全状态 | `docs/SECURITY_CURRENT_STATE.md` | Secret/日志审计 |
| 审批摘要 | `docs/APPROVAL_SUMMARY.md` | 已批准产品决策 |
| P0 完成报告 | `docs/P0_COMPLETION_REPORT.md` | 基线建立结果 |
| 构建基线 | `docs/BUILD_BASELINE.md` | JDK/Gradle/AGP 版本 |
| 测试环境 | `docs/APP_TEST_ENVIRONMENT.md` | 雷电模拟器配置 |
| 品牌范围 | `docs/BRANDING_SCOPE.md` | applicationId 和命名决策 |
| 上游来源 | `docs/UPSTREAM_PROVENANCE.md` | Track & Graph 移植台账 |
| 第三方声明 | `docs/THIRD_PARTY_NOTICES.md` | 依赖许可证 |
| 测试约定 | `docs/TESTING_CONVENTIONS.md` | 确定性测试规则 |
