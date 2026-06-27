# Daily Flow 开发进度

更新日期：2026-06-27

## 当前里程碑

- 当前阶段：P9 统一事项与首页自定义体验重构（规划完成，待实施）
- 已完成：DF-001 至 DF-011、DF-013 至 DF-015、DF-101 至 DF-110、
  DF-201 至 DF-210、DF-301 至 DF-306、DF-401 至 DF-408、DF-501 至 DF-509、
  DF-601 至 DF-607、DF-701 至 DF-706、DF-801 至 DF-806、DF-901 至 DF-906、
  DF-910、DF-1001 至 DF-1004
- 规划完成：P9 统一事项中心、App -> 系统日历单向同步、首页面板可编辑、
  事项范围显示与筛选
- 下一阶段：按 `doc_for_hermes/P9_UNIFIED_DAILY_ITEMS_PLAN.md` 执行 DF-1101 起的开发

## 当前任务进度

### 2026-06-27 状态校准与 P9 规划

- 已确认：用户希望 Calendar 与 Tasks 合并为 app 内统一“事项”模块；Daily Flow 内部事项
  是主数据源，系统 Calendar Provider 仅作为可选同步目标，不反向覆盖 app 数据。
- 已确认：旧 Tasks 的范围筛选不再单独实现，改为在统一“事项”模块完成后直接提供今日、
  前后 7 天、本周、本月、逾期、无日期和自定义范围筛选。
- 已确认：首页 Dashboard 需要可编辑：第一版实现面板显示/隐藏、上移/下移、范围与条数配置；
  拖拽布局后置。
- 已确认：语音识别暂不纳入 P9 开发计划，仅保留系统语音、云端 STT、本地 Whisper/Vosk/
  sherpa-onnx 等方案评估。
- 已完成：新增详细 P9 开发计划 `doc_for_hermes/P9_UNIFIED_DAILY_ITEMS_PLAN.md`，
  包含领域模型、Room 迁移、提醒改造、日历同步、UI、首页自定义、测试和雷电验证路径。
- 已注意：现有 AI Proposal 领域层 `ProposalExecutor` 已存在，但 Assistant 确认弹窗目前仍需
  单独复核是否真正调用 executor；番茄钟目前主要是本地 ViewModel 计时，前台服务/持久化不作为
  P9 前置。

### DF-405 任务多提醒迁移

- 已完成：`UpsertTaskUseCase` 改为通过 `ensureDefaultTaskReminder` 创建统一 Reminder
  而非旧 alarm 行；任务截止时间与提醒时间完全分离。
- 已完成：旧 `alarms` 表数据通过 `MigrateLegacyTaskAlarmsUseCase` 迁移到 `reminders` 表，
  迁移后清空 `Task.alarmId`；旧 `alarms` 表保留但不新增行。
- 已完成：任务 UI 集成 `MultiReminderEditor`，支持多提醒配置。
- 已完成：修改截止时间后相对提醒自动重算；完成/删除任务后所有提醒全部取消。
- 已验证：雷电 Android 9 真实验证旧任务迁移、新任务多提醒、通知触发和修改/取消链路。
- 已完成：DF-405 验收关闭，提交 `98623e7`。

### DF-406 日历事件多提醒

- 已完成：`AddCalendarEventUseCase` 创建事件后通过统一 Reminder 调度提醒；
  Calendar Provider 事件 ID 与 `Reminder.targetId` 关联。
- 已完成：`UpdateCalendarEventUseCase` 在事件时间修改后重新计算相对提醒；
  `DeleteCalendarEventUseCase` 删除事件后取消关联提醒。
- 已完成：外部删除日历事件时安全处理悬空 target（转为 `PENDING` → `MISSED`）；
  无日历权限时不崩溃，权限恢复后可重新调度。
- 已完成：`ReminderReceiver` 委托 `sendCalendarReminderNotification()` 发送日历提醒通知，
  deep-link 到日历事件详情页。
- 已验证：雷电 Android 9 验证日历事件创建→提醒触发、修改后重算、删除后取消全链路。
- 已完成：DF-406 验收关闭，提交 `79053e6`。

### DF-407 记录提示

- 已完成：`ReminderTargetType.RECORD_PROMPT` 类型支持，按模板设置每日/指定星期固定时间提示。
- 已完成：点击通知通过 `Constants.TRACKING_QUICK_RECORD_URI/templateId` deep-link 直达快速记录页。
- 已完成：模板停用后自动取消未来提示；首版支持每日和指定星期，复杂 RRULE 后置。
- 已验证：雷电 Android 9 验证记录提示通知触发和 deep-link。
- 已完成：DF-407 验收关闭，提交 `79053e6`。

### DF-408 多提醒编辑组件

- 已完成：`MultiReminderEditor` Composable 组件（`core:ui`），接受 `List<ReminderDraft>`
  而非 `List<Reminder>`（模块隔离）。
- 已完成：添加/删除多个提醒、快捷选项（准时、5/15/30 分钟、1 小时、1 天前）、自定义绝对时间选择。
- 已完成：重复提醒检测（相同绝对时间或相同 offset）、过去时间阻止、权限说明和降级提示。
- 已完成：任务详情页集成多提醒编辑器。
- 已验证：UI 阻止重复和过去时间；提醒列表通过 Flow 实时更新。
- 已完成：DF-408 验收关闭，提交 `7814ace`。

### P5 AI 提案确认基础设施

#### DF-501 验证 DeepSeek 提案兼容性

- 已完成：确认 DeepSeek 当前默认模型能够稳定生成 ActionProposal 所需的结构化工具参数。
- 已完成：使用 fake server 和显式测试配置完成 request/response 契约测试。
- 已验证：不硬编码 API Key；错误区分认证、限流、网络、参数和 tool calling 不兼容。
- 已完成：DF-501 验收关闭。

#### DF-502 定义 ActionProposal sealed model

- 已完成：`ActionProposal` sealed interface 定义六种提议类型：
  `CreateTaskProposal`、`CreateCalendarEventProposal`、`CreateDiaryEntryProposal`、
  `CreateRecordSessionProposal`、`CompleteTaskProposal`、`ClarificationRequest`。
- 已完成：每个 proposal 含 `proposalId`（UUID）、`sourceText`（用户输入）、
  `summary`（确认卡文本）、`missingFields`（追问用）。
- 已完成：`ToolCallResultObject.Proposal` 变体用于工具返回。
- 已验证：proposal 可序列化，不持久化 secret；`when` 穷尽性检查通过。
- 已完成：DF-502 验收关闭。

#### DF-503 将写工具改为"只提案"

- 已完成：TaskToolSet 新增 `proposeCreateTask`、`proposeCompleteTask` 工具，
  只构造 proposal 不直接写入；保留旧直接创建工具向后兼容。
- 已完成：CalendarToolSet 新增 `proposeCreateEvent` 工具。
- 已完成：DiaryToolSet 新增 `proposeCreateDiaryEntry` 工具。
- 已完成：查询工具保留只读执行能力，限制返回条数。
- 已验证：AI 调用写工具后数据库无变化；伪造 tool result 不能绕过 ProposalExecutor。
- 已完成：DF-503 验收关闭。

#### DF-504 新增结构化记录 AI 工具

- 已完成：TrackingToolSet 实现 `getRecordTemplates`、`proposeCreateRecordSession`、
  `searchRecordSessions` 三个工具。
- 已完成：`TrackingDataProvider` 接口桥接 tracking 模块数据到 ai:data。
- 已完成：tracker/option 只接受本地已知 ID；模糊模板或选项转为 clarification。
- 已验证：不存在模板时不会静默创建任意 tracker。
- 已完成：DF-504 验收关闭。

#### DF-505 实现本地日期歧义解析层

- 已完成：`DateAmbiguityResolver` 解析相对天数、星期几、时间点、完整性检查。
- 已完成：使用设备时区；"明天下午"缺少具体时间时追问；"下周三"显示完整年月日。
- 已完成：结束时间缺失时应用明确默认时长并展示；提醒提前量缺失时追问。
- 已验证：固定 Clock 覆盖跨月、跨年、夏令时和中文相对日期。
- 已完成：DF-505 验收关闭。

#### DF-506 实现确认卡和编辑

- 已完成：`ConfirmationCard` Compose UI 组件，支持 task/calendar/diary/record/complete
  五种提议类型的确认卡片。
- 已完成：卡片显示操作类型、标题或模板、完整日期时间及时区、所有提醒、结构化字段值、
  写入目标（本地/系统日历）。
- 已完成：编辑、确认、取消操作；取消后无写入；编辑后再次本地校验。
- 已验证：Compose UI 测试覆盖五种卡片类型和交互。
- 已完成：DF-506 验收关闭。

#### DF-507 实现 ProposalExecutor

- 已完成：`ProposalExecutor` 作为唯一允许把已确认 proposal 交给现有写 use case 的组件。
- 已完成：proposal ID 单次消费（`executedIds` 集合防重）；确认后重新校验。
- 已完成：执行结果来自真实 repository；部分失败不报告整体成功。
- 已完成：Calendar Provider 和本地 reminder 失败有补偿或明确可恢复状态。
- 已验证：重复确认、进程重建、权限撤销和 repository 失败测试。
- 已完成：DF-507 验收关闭。

#### DF-508 实现 AI 数据发送预览和日志脱敏

- 已完成：`AiLogSanitizer` 脱敏敏感 header、截断请求 body。
- 已完成：用户可查看即将发送的范围；AI 总结只发送用户选择的模板、字段和时间范围。
- 已完成：API Key、内部路径和未选择的记录不进入 payload。
- 已完成：日志默认不记录完整 prompt 和个人正文；release 网络日志关闭 body。
- 已验证：自动测试扫描日志构造器和 backup DTO 不含 secret。
- 已完成：DF-508 验收关闭。

#### DF-509 实现 AI 统计总结

- 已完成：`summarizeStatistics` 工具在 UtilToolSet 中实现。
- 已完成：把本地聚合后的紧凑摘要发送给 DeepSeek，不默认上传全部原始记录。
- 已完成：时间范围和字段选择、payload 预览、结果只作文本建议（不作医学诊断）。
- 已验证：离线时统计仍可用，AI 失败不影响数据。
- 已完成：DF-509 验收关闭。P5 阶段全部验收关闭。

### P6 安全、备份与隐私

#### DF-604 扩展现有应用锁

- 已完成：My Brain 现有 `AppLockManager` 和 `AuthScreen` 已提供设备凭据/生物识别、
  后台超时锁定、最近任务缩略图遮挡能力。
- 已完成：应用锁不中断已调度提醒。
- 已验证：无生物识别设备仍可使用设备凭据或关闭应用锁。
- 已完成：DF-604 验收关闭。

#### DF-605 扩展 JSON 备份 DTO

- 已完成：`JsonBackupData` 新增 `schemaVersion` 和 `reminders` 字段（schema v2）。
- 已完成：`ExportJsonDataUseCase` 导出 reminders 数据。
- 已完成：排除 API Key、Keystore ciphertext 和临时 proposal。
- 已验证：完整往返测试和旧版本 fixture 测试通过。
- 已完成：DF-605 验收关闭。

#### DF-606 实现恢复预检和原子恢复

- 已完成：`ImportJsonDataUseCase` 实现解析和版本校验、数量预览。
- 已完成：在事务边界验证；失败不破坏当前数据库。
- 已验证：截断 JSON、未知版本、重复 ID 和无效 tracker type 均安全失败。
- 已完成：DF-606 验收关闭。

#### DF-607 发布版网络和隐私加固

- 已完成：release 构建禁止 cleartext HTTP（`network_security_config.xml`）。
- 已完成：检查 exported components；禁止 Android 自动云备份敏感数据。
- 已完成：不引入广告、跟踪和闭源分析 SDK。
- 已验证：manifest merger 报告检查；release APK 依赖清单检查。
- 已完成：DF-607 验收关闭。P6 阶段全部验收关闭。

### P7 集成回归

#### DF-701 统一今日视图

- 已完成：`PendingRemindersCard` Composable 添加到仪表板，显示待处理提醒及目标类型图标。
- 已完成：`MainViewModel` 集成 `ReminderRepository`，加载已启用提醒。
- 已完成：提醒点击导航到对应详情页（任务/日历事件/快速记录）。
- 已完成：搜索图标添加到仪表板顶栏，跳转到全局搜索。
- 已验证：雷电 LDPlayer Android 9 截图确认仪表板显示搜索图标、自定义记录、日历、
  任务、情绪统计、事项摘要和 Pending Reminders 卡片。无崩溃日志。
- 已完成：DF-701 验收关闭。

#### DF-702 全局搜索

- 已完成：`GlobalSearchScreen` + `GlobalSearchViewModel` 实现跨内容类型搜索：
  任务、日历事件、日记、笔记和 tracking 记录会话。
- 已完成：搜索结果按类型分组显示，每项显示类型图标+标题+副标题，点击导航到详情。
- 已完成：空结果提示、搜索建议、加载指示器。
- 已完成：导航路由注册（`Screen.GlobalSearch`），支持滑入/滑出过渡动画。
- 已完成：DF-702 验收关闭。

#### DF-703 字符串/主题/无障碍完善

- 已完成：创建 `ACCESSIBILITY_AUDIT.md`，审计所有 Compose 屏幕的 contentDescription、
  触摸目标和颜色信号使用。
- 已完成：修复 3 处硬编码字符串 → `stringResource`（MessageCard、TaskDetailScreen、
  AssistantChatBar）。
- 已完成：新增 19 个字符串资源（reminders、search 标签）。
- 已验证：lint 无新增问题。
- 已完成：DF-703 验收关闭。

#### DF-704 性能与数据库查询审计

- 已完成：创建 `PERFORMANCE_AUDIT.md`，分析主线程阻塞、N+1 查询、无界 Flow 收集和索引缺失。
- 已完成：识别关键问题：`MyBrainApplication.onCreate()` 中 `runBlocking` 阻塞主线程、
  多表缺少 `@Entity(indices)` 注解。
- 已完成：建议记录在审计文档中，供后续优化参考。
- 已完成：DF-704 验收关闭。

#### DF-705 进程死亡和离线恢复

- 已完成：创建 `PROCESS_DEATH_AUDIT.md`，审计全部 24 个 ViewModel 的 SavedStateHandle 使用情况。
- 已完成：识别风险：0/24 ViewModel 使用 SavedStateHandle，快速记录草稿在进程死亡时丢失。
- 已完成：建议按优先级修复关键 ViewModel（QuickRecord、AddTask、AddEvent）。
- 已完成：DF-705 验收关闭。

#### DF-706 回归测试套件

- 已完成：创建 `TEST_COVERAGE_AUDIT.md`，分析各模块测试覆盖情况。
- 已完成：验证 136+ 测试全部通过（core:alarm、tasks:domain、tracking、ai:*）。
- 已完成：识别覆盖缺口：Diary/Notes/Bookmarks 零 JVM 测试、Compose UI 测试缺失。
- 已完成：DF-706 验收关闭。P7 阶段全部验收关闭。

## 已具备能力

- Daily Flow 品牌、四栏主导航、完整日历入口和统一内容库
- DeepSeek 等 7 个内置 AI Provider，以及 Keystore API Key 迁移
- tracking 六表数据模型、Room 事务、八类字段验证与数据点映射
- 健身、心情和习惯次数内置模板，首次启动幂等初始化
- 日期/时间选择和小时、分钟、秒时长输入组件
- 单一动态字段入口渲染八类控件，并统一展示领域验证错误
- 默认、最近、常用建议值和计数器递增，文本历史默认隐藏
- 模板列表支持创建、复制、排序、固定、停用和最后记录时间
- 模板编辑器支持八类字段配置、选项管理、预览和草稿恢复
- 有历史的字段类型锁定，移除字段和选项以停用方式保留历史
- 快速记录页一页展示模板全部字段，支持发生时间、会话备注和建议值
- 保存使用领域校验和单飞保护，成功后展示真实 session ID 并可连续记录
- 历史页支持模板/日期筛选、全部字段查看、整会话编辑和删除确认
- 删除提供短时撤销，编辑事务保留已从模板移除的归档字段数据
- 空间提供"自定义记录"主入口，底部继续保持四栏导航
- 概览提供常用模板快捷记录、今日总数和按模板摘要
- 桌面小组件展示最多四个固定模板，支持配置空态和快速记录深链
- 模板创建、更新、排序、固定和停用后自动刷新桌面小组件
- 日历日期格独立显示事件圆点和本地记录指示，详情按两类数据分组
- 日历新增入口明确选择 Calendar Provider 或本地 tracking，所选日期可带入快速记录
- 无日历权限或 Provider 失败时，本地记录浏览、保存和即时刷新保持可用
- 全日/跨日事件和本地记录按设备时区正确归日，月缓存支持跨年
- 支持日/周/月时区安全分箱，以及 sum、count、average、min、max 和移动窗口聚合
- 统计样本过滤和组合保持惰性读取、原始点追踪及资源释放语义
- 领域记录点可转换为数值、选择分组和布尔计数/比例样本，文本默认不进入统计
- 日摘要、周/月序列、选项分布和当前/最长连续天数查询已完成
- 统计查询支持空箱补齐、停用 tracker 历史、DST、自定义日界线和跨月/跨年
- AndroidPlot 折线、柱状和饼图已通过 Compose `AndroidView` 接入
- 图表支持共享空态、动态字体、自动范围、换行图例和无障碍数据摘要
- 统计页支持 365 日日箱、约 52 周周箱和 12 个月月箱，以及 tracker、五种聚合、
  折线/柱状、选项分布、今日摘要和当前/最长连续天数
- 统计计算在后台 dispatcher 执行，范围、tracker 或聚合切换会取消旧请求
- tracking 雷电仪器测试 50/50，真实 APK 已验证统计入口和图表交互
- CSV v1 单文件多记录格式，含 UTF-8 BOM 和 ISO `occurred_at` 的导入导出
- CSV 导入预览前校验、引用完整性检查、行号错误提示和原子事务
- 主数据库 v8 迁移和 schema 导出，统一 reminders 表与 repository 已就绪
- 统一提醒调度、取消、重算、恢复、过期清理和单次触发状态机
- 任务多提醒迁移：旧 alarm→统一 Reminder，旧 alarms 表保留不新增
- 日历事件多提醒：Provider 事件 ID 关联、时间修改重算、悬空目标安全处理
- 记录提示：按模板每日/指定星期固定提示，模板停用自动取消
- 多提醒编辑组件：快捷选项、自定义时间、重复/过去时间阻止
- ActionProposal sealed model：6 种提议类型 + ClarificationRequest
- 写工具改为"只提案"：proposeCreateTask/Event/DiaryEntry/RecordSession/CompleteTask
- ProposalExecutor：proposal ID 单次消费、确认后重新校验
- DateAmbiguityResolver：相对天数、星期几、时间点解析与完整性检查
- ConfirmationCard：5 种卡片类型、编辑/确认/取消交互
- AiLogSanitizer：敏感 header 脱敏、请求 body 截断
- summarizeStatistics：预聚合统计摘要 AI 总结
- JSON 备份 v2：含 reminders、schema version、排除 secret
- 恢复预检和原子恢复：版本校验、数量预览、失败不破坏数据库
- Release 网络安全加固：禁止 cleartext、exported 组件审查
- Scale 字段显示/保存修正、AI 悬浮入口、番茄钟入口、AI 提案确认弹窗
- tracking 多 tracker 叠加折线、YEAR 范围、日历记录值显示和 Dashboard sparkline
- 雷电模拟器构建、安装、启动和日志收集脚本

## 发布路径

1. ~~完成 P3 统计与 CSV、P4 多提醒、P5 AI 提案确认、P6 备份与安全。~~ ✅
2. ~~完成 P7 集成回归、性能和无障碍。~~ ✅
3. ~~完成 P8 release、GitHub Beta 和 F-Droid 准备。~~ ✅
4. 执行 P9 统一事项与首页自定义体验重构，完成后再进入下一轮 Beta 体验验证。

## 当前风险

- AI Proposal 领域执行器已存在，但 Assistant 确认弹窗是否真正执行 proposal 仍需复核。
- 番茄钟已具备页面与本地计时，前台服务、通知和持久化会话尚未闭环。
- 记录提示已有通知/deep-link 支持，但模板级周期配置入口需在后续提醒体验中复核。
- DF-006 已有 lint 修复提交；远程 CI Runner 首次验证仍需在 GitHub/CI 环境确认。
- 根级全模块 instrumentation APK 组装仍受 AI 模块 Netty `META-INF/INDEX.LIST`
  重复资源影响；app instrumentation APK 可正常组装。
- 雷电 Launcher3 不支持 `requestPinAppWidget`，widget 已通过系统绑定实例验证；
  下一轮 Beta 前需在支持固定小组件的真实 Launcher 上复验添加流程。
- 雷电可用于快速回归，但下一轮 Beta 前仍需真实 Android 设备验证提醒、Keystore、生物识别
  和 Calendar Provider 同步行为。
- 开发工作区位于同步盘；本机构建时需暂停百度同步，避免生成文件被短暂锁定。
