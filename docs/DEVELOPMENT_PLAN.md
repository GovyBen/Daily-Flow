# Daily Flow 安卓应用详细开发计划

版本：草案 v0.1  
编制日期：2026-06-12  
状态：已批准执行阶段 0

## 1. 项目目标

开发一款完全开源、以本地数据为核心的安卓日程与生活事件记录应用。应用在同一界面内提供：

- 任务、计划、日历事件、完成状态和重复规则。
- 可自定义的结构化日常记录模板。
- 多选、计数、滑条、布尔、时长、数值和文字等记录控件。
- 日、周、月趋势和标签分布等统计。
- 本地输入法语音转文字。
- 通过 DeepSeek API 识别自然语言意图。
- AI 写入前确认、歧义日期追问和可审计的操作结果。
- 统一的多重提醒体系。
- 本地优先、安全存储、无广告、无分析 SDK。

项目暂定名为 **Daily Flow**。正式名称、图标和应用 ID 在阶段 0 决定。

## 2. 已批准的产品原则

1. 以 `My Brain` 为主干 Fork，延续 GPLv3。
2. 所有核心功能整合在一个应用中。
3. 首版支持多选、计数、滑条、布尔、时长、数值和文字备注。
4. DeepSeek 可创建任务、日历事件、日记和结构化记录，但写入前必须确认。
5. 事件时间与提醒时间分离，支持多个提前提醒。
6. 首版使用 Android Calendar Provider，CalDAV 后置。
7. 首版实现 Android Keystore、应用锁；数据库加密和加密备份在第二阶段完成。
8. 首版提供日、周、月统计、标签分布、连续天数和 AI 总结。
9. 发布 GitHub APK，并以进入 F-Droid 为目标；最低 Android 8。

## 3. 开源复用策略

### 3.1 主干：My Brain

上游：

- 仓库：https://github.com/mhss1/MyBrain
- 基线版本：`v3.1.0`
- 核验提交：`a49727cbec0e686edb82f447b2984ecb674d3edf`
- 许可证：GPLv3

计划直接保留并演进：

- Kotlin、Jetpack Compose 和多模块工程结构。
- Room 数据库及迁移框架。
- Koin 依赖注入。
- 任务、子任务、优先级、截止时间和重复规则。
- AlarmManager 精确提醒和开机恢复。
- Android Calendar Provider 集成。
- 日记、五档心情和基础图表。
- Koog AI Agent、工具调用和 OpenAI 兼容接口。
- 笔记、书签、桌面组件、生物识别入口。
- 本地数据默认不上传、禁用 Android 自动备份的基础策略。

### 3.2 参考及选择性移植：Track & Graph

上游：

- 仓库：https://github.com/SamAmco/track-and-graph
- 核验提交：`4bb925a731e0537f6330971853770e9aafb51983`
- 许可证：GPLv3 或更高版本

优先复用或改写：

- 时间序列数据点的设计思想：时间、数值、标签和备注。
- 快速记录、默认值和历史输入建议。
- 聚合、分组、连续天数及图表计算逻辑。
- 折线图、柱状图和饼图的交互设计。
- CSV 导入导出和备份思路。
- 与记录相关的单元测试案例和边界条件。

不直接合并整个项目，原因如下：

- Track & Graph 使用 Hilt，My Brain 使用 Koin。
- 两者数据库、导航和领域模型不同。
- Track & Graph 的完整函数图和 Lua 系统超出首版需求。
- 整体合并会显著增加升级、迁移和测试成本。

执行方式：

- 保持 My Brain 架构为唯一主架构。
- 对移植文件保留原始版权头和 GPL 声明。
- 在 `THIRD_PARTY_NOTICES.md` 记录来源仓库、提交和修改说明。
- 对明显改写的算法，在提交信息中记录上游文件路径。
- 维持可重复构建，不加入闭源 SDK。

## 4. 产品范围

### 4.1 首个正式版包含

#### 日程与任务

- 创建、编辑、删除、搜索任务。
- 子任务、优先级、完成状态、截止时间。
- 每分钟、每天、每周、每月和每年重复。
- 创建和显示 Android 系统日历事件。
- 月视图、列表视图和今日总览。
- 任务和事件统一支持一个或多个提醒。

#### 结构化事件记录

- 用户创建、复制、排序、停用记录模板。
- 模板字段支持：
  - 多选项，例如胸、背、臀、腿。
  - 次数计数，例如吸烟、社交。
  - 数值滑条，例如心情 1 至 10。
  - 布尔打卡，例如是否服药。
  - 时长，例如运动 60 分钟。
  - 普通数值，例如体重、饮水量。
  - 单选项。
  - 文字备注。
- 一次提交可包含多个字段。
- 支持补录、修改时间、编辑和删除。
- 常用模板快捷入口及桌面组件入口。

#### 统计与总结

- 日、周、月、年时间范围。
- 总次数、总量、平均值、最大值和最小值。
- 标签或选项分布。
- 连续记录天数。
- 折线图、柱状图和饼图。
- 多模板今日汇总。
- 将用户选定的汇总数据发送给 DeepSeek 生成文字总结。

#### AI 与语音

- 使用手机本地输入法完成语音转文字。
- OpenAI 兼容 API 配置，首选 DeepSeek。
- 自然语言识别下列意图：
  - 创建任务。
  - 创建日历事件。
  - 创建日记。
  - 添加结构化事件记录。
  - 查询任务、事件和记录。
  - 标记任务完成。
- 所有写操作先生成确认卡。
- 时间、对象或模板不明确时先追问。
- 用户确认后才执行数据库或系统日历写入。
- 执行失败时显示明确原因，不允许模型假报成功。

#### 安全与隐私

- 数据默认仅保存在设备本地。
- API Key 使用 Android Keystore 保护。
- 支持生物识别或设备凭据应用锁。
- 发布版只允许 HTTPS API 地址。
- 不集成广告、追踪或崩溃分析 SDK。
- 提供即将发往 DeepSeek 的数据预览。
- AI 总结默认只发送用户主动选择的时间范围和字段。

### 4.2 首版不包含

- iOS、网页端和桌面端。
- 多人协作、共享日历和家庭账户。
- 自建云同步服务。
- 内置语音识别模型。
- CalDAV 原生客户端。
- Track & Graph 的 Lua 自定义函数系统。
- 医疗诊断、健康建议或自动风险判断。
- AI 未经确认自动执行写操作。

## 5. 信息架构

底部主导航建议为：

1. **今天**：任务、日历事件、快捷记录和今日摘要。
2. **日程**：任务列表、日历和计划。
3. **记录**：模板、快捷录入和历史记录。
4. **统计**：图表、连续天数和 AI 总结。
5. **更多**：日记、笔记、书签、设置和数据管理。

AI 助手使用全局悬浮入口或顶部入口，不单独占据底部导航。

## 6. 技术架构

### 6.1 技术基线

- 语言：Kotlin。
- UI：Jetpack Compose + Material 3。
- 最低版本：Android 8 / API 26。
- 编译 SDK：保持上游可构建版本，目标升级至 API 36。
- 数据库：Room。
- 依赖注入：Koin。
- 并发：Kotlin Coroutines、Flow。
- 网络：Ktor。
- AI：Koog + OpenAI 兼容端点。
- 提醒：AlarmManager、WorkManager 和启动广播。
- 日历：Android Calendar Provider。
- 图表：优先移植开源 Compose 绘图逻辑；必要时采用许可证兼容的开源库。

### 6.2 建议模块

保留 My Brain 现有模块，并新增：

```text
tracking/
  domain/          模板、字段、记录、聚合接口
  data/            Room 实体、DAO、仓库、迁移
  presentation/    模板编辑、快捷录入、历史列表

analytics/
  domain/          聚合、连续天数、分组计算
  presentation/    图表、筛选和总结界面

reminders/
  domain/          统一提醒模型和调度接口
  data/            AlarmManager 实现、重启恢复
  presentation/    多提醒编辑控件

security/
  domain/          SecretStore、锁定策略
  data/            Android Keystore、加密导出

ai/
  ...              在现有模块上增加提案和确认流程
```

所有新模块遵循现有 domain/data/presentation 分层，不在界面层直接访问 DAO 或调用 AI。

## 7. 核心数据模型

### 7.1 记录模板

`RecordTemplate`

- `id: String`
- `name: String`
- `description: String`
- `icon: String`
- `color: Long`
- `isActive: Boolean`
- `displayOrder: Int`
- `createdAt: Instant`
- `updatedAt: Instant`

`RecordField`

- `id: String`
- `templateId: String`
- `name: String`
- `type: FieldType`
- `unit: String?`
- `required: Boolean`
- `displayOrder: Int`
- `configJson: String`
- `createdAt: Instant`
- `updatedAt: Instant`

`FieldOption`

- `id: String`
- `fieldId: String`
- `label: String`
- `numericValue: Double?`
- `color: Long?`
- `displayOrder: Int`
- `isActive: Boolean`

`FieldType`

- `MULTI_SELECT`
- `SINGLE_SELECT`
- `COUNTER`
- `SCALE`
- `BOOLEAN`
- `DURATION`
- `NUMBER`
- `TEXT`

### 7.2 记录实例

`RecordEntry`

- `id: String`
- `templateId: String`
- `occurredAt: Instant`
- `createdAt: Instant`
- `updatedAt: Instant`
- `note: String`
- `source: MANUAL | AI | IMPORT`

`RecordValue`

- `id: String`
- `entryId: String`
- `fieldId: String`
- `numberValue: Double?`
- `textValue: String?`
- `booleanValue: Boolean?`
- `durationSeconds: Long?`

`RecordValueOptionCrossRef`

- `recordValueId: String`
- `optionId: String`

关键约束：

- 历史数据不能因模板修改而失去含义。
- 删除已使用字段时执行“停用”，不物理删除。
- 字段名称、单位和选项可在记录中保存快照，保证长期可读。
- 所有时间以 Instant 存储，并保留录入时区信息供导出。

### 7.3 统一提醒

`Reminder`

- `id: String`
- `targetType: TASK | CALENDAR_EVENT | RECORD_PROMPT`
- `targetId: String`
- `triggerAt: Instant`
- `offsetMinutes: Long?`
- `enabled: Boolean`
- `status: SCHEDULED | FIRED | CANCELLED`
- `createdAt: Instant`

提醒方案：

- 任务截止时间和提醒时间分开保存。
- 日历事件创建后保存系统事件 ID，再由本应用调度本地提醒。
- 支持绝对提醒时间和“提前 N 分钟”。
- 修改目标时间时重新计算相对提醒。
- 开机、时区变更和应用升级后重建调度。
- 精确提醒权限不可用时降级并明确告知用户。

## 8. AI 工作流

### 8.1 连接配置

- Provider 类型：OpenAI Compatible。
- Base URL：默认允许填写 DeepSeek 官方地址。
- Model：用户可配置。
- API Key：仅显示末尾四位，支持替换和删除。
- Tool Calling：启动时进行能力测试。
- 请求超时、重试和最大输出限制可配置为安全默认值。

### 8.2 写操作流程

```text
用户语音或文字
  -> DeepSeek 解析意图
  -> 返回结构化 ActionProposal
  -> 本地 schema 校验
  -> 时间、提醒、模板和字段语义校验
  -> 若有歧义，向用户提问
  -> 显示确认卡
  -> 用户确认
  -> 本地执行器调用 use case
  -> 返回真实执行结果
```

与 My Brain 当前直接执行工具的方式相比，需要增加一层“提案”：

- 模型只能产生 `ActionProposal`，不能直接获得写工具。
- 确认卡展示标题、时间、提醒、目标日历和结构化字段。
- 用户可在确认卡中修改内容。
- 只有确认后的本地对象才能交给写入 use case。
- 查询类工具可直接执行，但需限制返回的数据量。

### 8.3 首批工具

- `proposeCreateTask`
- `proposeCreateCalendarEvent`
- `proposeCreateDiaryEntry`
- `proposeCreateRecordEntry`
- `proposeCompleteTask`
- `searchTasks`
- `searchCalendarEvents`
- `searchRecordEntries`
- `getRecordTemplates`
- `summarizeSelectedData`

### 8.4 时间歧义规则

- “明天下午”缺少具体时间时必须询问。
- “下周三”根据当前时区计算并在确认卡显示完整日期。
- 结束时间缺失时，日历事件采用可配置默认时长，但必须展示。
- “提前提醒”缺少提前量时必须询问。
- 跨时区和夏令时变化由本地时间库计算，模型不负责毫秒时间戳。

## 9. 安全设计

### 9.1 首版

- 使用 Android Keystore 生成不可导出的 AES/GCM 密钥。
- API Key 加密后存入应用私有存储，不以明文写入普通 DataStore。
- 发布版关闭明文 HTTP。
- 日志禁止记录 API Key、完整提示词和个人记录正文。
- 应用切入后台时可遮挡最近任务缩略图。
- 应用锁支持生物识别和设备凭据。
- 导出前提示文件将离开应用保护范围。
- 依赖清单、许可证和软件物料清单随版本生成。

### 9.2 第二阶段

- 使用兼容 F-Droid 的开源数据库加密方案。
- 加密备份采用版本化容器、随机盐、强 KDF 和认证加密。
- 支持恢复前校验、错误密码处理和原子替换。
- 完成基础威胁模型和第三方依赖审计。

## 10. 开发阶段与验收标准

以下估算为**净工程量**，不含等待审批、商店审核和大规模需求变更。

### 阶段 0：项目建立与技术验证

工期：1 人周

工作：

- Fork My Brain，配置 `upstream` 和新应用 ID。
- 完成品牌占位替换和贡献说明。
- 建立 GitHub Actions 构建、单元测试和 APK 产物。
- 验证 DeepSeek Tool Calling 与 Koog 的兼容性。
- 验证 Android 8、12、14、16 构建和启动。
- 建立第三方代码来源台账。

验收：

- Debug 和 Release 构建成功。
- 无 DeepSeek Key 时应用可完整离线使用。
- 使用测试 Key 能返回结构化工具提案。
- 上游许可证和版权信息完整保留。

### 阶段 1：结构化记录内核

工期：2.5 人周

工作：

- 新增 tracking 模块和 Room 表。
- 实现八种字段类型及验证规则。
- 模板和记录 CRUD。
- 数据库迁移、事务和软删除。
- 建立健身、心情、吸烟和社交示例模板。

验收：

- 可创建含多种字段的模板。
- 一次记录可保存多个多选项和数值。
- 修改模板后旧记录仍可正确显示。
- 核心仓库和 use case 单元测试通过。

### 阶段 2：快捷录入与历史

工期：2 人周

工作：

- 模板列表、模板编辑器和动态表单。
- 计数器快捷加减、滑条、多选和时长输入。
- 补录时间、备注、编辑和删除。
- 最近值建议、默认值和常用模板置顶。
- 今日页快捷记录卡片。

验收：

- 常用记录可在三次点击内完成。
- 支持本地输入法语音填写文本。
- 旋转屏幕、切后台和进程恢复不会丢失未提交内容。

### 阶段 3：统计与总结

工期：2 人周

工作：

- 聚合查询和缓存。
- 日、周、月、年筛选。
- 折线图、柱状图、饼图。
- 连续天数和选项分布。
- CSV 导出。
- AI 总结的数据选择和脱敏预览。

验收：

- 1 万条记录下常用统计在目标设备上可接受。
- 时区切换不会导致数据错误归日。
- 图表数值与数据库聚合测试结果一致。
- 用户可在发送前看到全部待发送数据。

### 阶段 4：统一提醒

工期：2 人周

工作：

- 新提醒表和调度层。
- 任务支持多个绝对或相对提醒。
- 日历事件支持本地多提醒。
- 开机和时区变更恢复。
- 权限引导、降级策略和通知操作。

验收：

- 同一事项可配置多个提醒。
- 修改事项时间后相对提醒自动更新。
- 重启设备后未来提醒仍存在。
- 完成或删除事项后相关提醒全部取消。

### 阶段 5：DeepSeek 意图识别和确认卡

工期：2.5 人周

工作：

- OpenAI Compatible Provider 配置。
- Keystore SecretStore。
- ActionProposal schema。
- AI 写入确认卡和可编辑字段。
- 歧义追问。
- 任务、日历、日记、记录和完成任务工具。
- 错误、限流和网络中断处理。

验收：

- “明天下午三点检查会议纪要，提前半小时提醒”生成正确提案。
- “今天练了胸和背一小时”匹配健身模板并生成正确字段。
- 未点击确认时数据库没有任何写入。
- 模型输出非法字段或日期时不会执行。
- 工具结果来自真实 use case，而不是模型声明。

### 阶段 6：安全、备份和数据迁移

工期：1.5 人周

工作：

- 完成应用锁和后台隐私保护。
- API Key 迁移至 Keystore。
- JSON/ZIP 明文备份与恢复。
- 导出风险提示。
- My Brain 原有任务、日记和设置迁移测试。

验收：

- 卸载前导出的备份可在新安装中恢复。
- 错误或损坏备份不会覆盖现有数据。
- 日志和备份不包含明文 API Key。

### 阶段 7：质量加固与发布

工期：2 人周

工作：

- 无障碍、中文文案、深色模式和大字体。
- 性能、耗电、提醒可靠性和权限测试。
- 依赖与许可证审计。
- 用户文档、隐私政策和变更日志。
- GitHub Release、签名和可重复构建说明。
- 准备 F-Droid 元数据和提交。

验收：

- 阻断级和严重级缺陷清零。
- 核心流程自动化测试通过。
- Release APK 无调试日志和测试密钥。
- 完成源码、构建说明、许可证和隐私说明发布。

## 11. 版本路线

### `v0.1.0` 工程预览

- 完成 Fork、品牌占位、CI 和 DeepSeek 技术验证。

### `v0.3.0` 内部 Alpha

- 结构化模板、记录和动态表单可用。

### `v0.5.0` 功能 Alpha

- 统计图表、快捷录入和 CSV 导出可用。

### `v0.7.0` AI Alpha

- DeepSeek 提案、确认卡和核心工具可用。

### `v0.9.0` Beta

- 多提醒、安全存储、备份恢复和完整迁移可用。

### `v1.0.0` 首个正式版

- 完成质量加固、GitHub 发布和 F-Droid 提交。

### `v1.1.0`

- 数据库加密、加密备份及安全审计改进。

### `v1.2.0`

- CalDAV 调研与首个同步版本；是否实施取决于同步冲突原型结果。

## 12. 测试计划

### 单元测试

- 模板字段验证。
- 记录保存和软删除。
- 聚合、连续天数和时区归组。
- 相对提醒计算。
- AI Proposal 解析和拒绝非法输入。
- 数据库迁移。

### 集成测试

- Room DAO 与事务。
- Calendar Provider 创建和读取。
- AlarmManager 调度和取消。
- DeepSeek 模拟服务器及工具调用。
- 备份和恢复。

### UI 测试

- 创建任务和完成任务。
- 创建健身模板并记录胸、背和时长。
- 心情滑条录入。
- AI 确认、修改和取消。
- 权限拒绝及恢复路径。

### 设备矩阵

- Android 8：最低版本。
- Android 12：通知和后台行为基线。
- Android 14：精确提醒权限。
- Android 16：当前目标适配。
- 至少一台国产系统实体机进行通知和后台存活测试。

## 13. 性能与质量指标

- 冷启动目标：中端设备 2 秒级。
- 常用页面交互不出现明显主线程阻塞。
- 1 万条结构化记录下列表可流畅分页。
- 常用月度统计目标在 500 毫秒内完成，复杂图表允许后台加载。
- 无网络时除 AI 外所有核心功能可用。
- AI 不可用时不影响手动录入、提醒和统计。
- 关键业务模块目标单元测试覆盖率不低于 80%。

## 14. 工作量与时间

预计净工程量：**15.5 至 18 人周**。

参考排期：

- 1 名熟悉 Kotlin/Compose 的全职开发者：约 18 至 22 个日历周。
- 2 名开发者，其中 1 名负责数据/AI、1 名负责 UI/提醒：约 10 至 13 个日历周。
- 另建议安排 1 至 2 周真实设备 Beta 观察期。

估算不包含：

- 全新视觉品牌设计。
- 大范围重写 My Brain 原功能。
- 自建同步服务器。
- F-Droid 审核等待时间。
- 上游依赖发生破坏性升级。

## 15. 主要风险与控制

| 风险 | 影响 | 控制措施 |
|---|---|---|
| DeepSeek 模型工具调用格式变化 | AI 功能失败 | Provider 抽象、契约测试、模型可配置 |
| 模型误解时间或字段 | 错误写入 | 本地校验、歧义追问、强制确认卡 |
| 两个上游工程结构不同 | 合并成本失控 | 只选择性移植 Track & Graph 算法和 UI |
| Android 厂商限制后台提醒 | 提醒延迟 | 权限诊断、实体机测试、降级提示 |
| 模板修改破坏历史记录 | 数据不可读 | 软删除、选项停用、字段快照 |
| API Key 泄漏 | 账户和费用风险 | Keystore、日志脱敏、额度受限密钥建议 |
| GPL 归属遗漏 | 发布合规风险 | 来源台账、版权头、第三方声明和审计 |
| 上游 My Brain 持续更新 | Fork 漂移 | 独立功能模块、定期小批量同步 |

## 16. 项目治理

- `main`：始终保持可发布。
- `develop`：可选；若团队较小，使用短分支直接合入 `main`。
- 每项功能通过 Issue、设计说明、Pull Request 和验收清单管理。
- 数据库迁移必须同时提交迁移测试。
- AI 工具 schema 变更必须提交契约测试。
- 每两周检查 My Brain 上游安全修复和兼容更新。
- 不承诺与上游 UI 完全同步，以稳定性优先。

## 17. 审批门槛

批准本计划即表示同意：

1. 产品基于 My Brain Fork，并以 GPLv3 公开完整源代码。
2. Track & Graph 只进行有出处的选择性移植。
3. `v1.0` 不包含 CalDAV、数据库加密和内置语音识别。
4. AI 写操作必须经用户确认。
5. `v1.0` 预计需要 15.5 至 18 人周净工程量。
6. 项目先完成阶段 0 技术验证，再锁定最终开发排期。

阶段 0 完成后应提交：

- 可安装的技术预览 APK。
- DeepSeek 工具调用兼容性报告。
- 最终模块清单。
- 修订后的工期和风险清单。
