# Daily Flow 开发进度

更新日期：2026-06-15

## 当前里程碑

- 当前阶段：P4 统一多重提醒
- 已完成：DF-001 至 DF-011、DF-013 至 DF-015、DF-101 至 DF-110、
  DF-201 至 DF-210、DF-301 至 DF-306、DF-401 至 DF-402、DF-601 至 DF-603
- 正在推进：DF-403 统一提醒数据模型
- 下一批：DF-404 提醒重算与同步、DF-405 任务多提醒迁移

## 当前任务进度

### DF-306 CSV 读写

- 已完成：确定版本 1 的单文件多记录格式，覆盖模板、跟踪项、选项、字段、会话和数据点，
  可完整表示无会话模板及从未使用的选项。
- 已完成：加入 Apache Commons CSV 1.14.1，并实现 UTF-8、BOM、ISO `occurred_at`
  和稳定表头的导入导出编解码。
- 已完成：导入预览前校验 schema version、记录类型、必填值、布尔/整数/有限数值、
  时区、tracker 配置 JSON、重复 ID 和六表引用完整性。
- 已完成：解析及引用错误携带 CSV 行号；JVM 测试覆盖逗号、换行、中文、引号、
  BOM、版本错误、字段错误和断裂引用。
- 已完成：Room 全量快照读取和单事务 upsert 导入；已加入成功等价和失败全回滚仪器测试。
- 已完成：Android 文件夹导出、文件导入、预览确认状态机、双语页面、模板页入口和类型安全导航。
- 已验证：tracking JVM 测试通过，app debug APK 构建通过。
- 已完成：Compose 页面测试覆盖导出/选文件回调、六类预览计数、显式确认、
  行号错误提示和模板页全局入口；Android 测试源码编译通过。
- 已验证：雷电 Android 9 上 tracking 事务与 Compose 仪器测试 50/50 通过，
  包含 CSV 成功等价、失败全回滚、预览确认和错误行号。
- 已验证：真实 APK 可从模板页进入 CSV 页面，并向下载目录导出 32 行、
  6.45 kB 的版本 1 CSV。
- 已修复：Calf 通用 `Document` 不接受 Android 9 的 `text/csv`；导入器改为
  显式允许 CSV MIME 后，DocumentsUI 可正常选择导出文件。
- 已验证：真实 APK 预览显示 3 模板、8 跟踪项、7 选项、8 字段、3 会话和
  3 数据点，确认后成功导入 32 行。
- 已验证：导入后再次导出的 6,449 字节 CSV 与导入前 SHA-256 完全一致：
  `3E2F8A10771D69936134CEBB1D7071E44AE3DA2259982C0A941D3E6A32FA5B11`。
- 已验证：真实流程日志无崩溃、ANR、Koin 或 Room 约束异常。
- 已验证：tracking 79 项 JVM 测试通过，lint 0 错误且新增资源无警告；
  app R8 release 构建通过。
- 已完成：新增 `THIRD_PARTY_NOTICES.md`，记录 Track & Graph 固定提交、
  CSV 改写范围、Commons CSV/IO/Codec 和 AndroidPlot/figlib 许可证。
- 已完成：实现计划和来源迁移账本已写入 DF-306 的 schema、事务、测试、
  真实 APK 与依赖验收结果。
- 已验证：最近的复数资源和 CSV MIME 修正合入后，最终雷电 Android 9
  tracking 仪器测试仍为 50/50 通过。
- 已完成：DF-306 验收关闭，P3 阶段完成，开发进入 P4。

### DF-401 提醒现状审计

- 已完成：追踪任务保存、alarm 表、AlarmManager、通知接收器和开机恢复链路。
- 已确认：alarm 自增 ID 同时作为任务引用、PendingIntent requestCode、广播参数
  和通知 ID；未来时间修改复用 ID，移除截止时间、完成或删除任务会取消提醒。
- 已确认：现有模型把截止时间直接当作唯一提醒时间；使用绝对 epoch 调度，
  重复任务仅在完成时按当时设备时区推进下一周期。
- 已确认：Android 12+ exact alarm 不可用时只返回失败并提示授权，没有非精确
  AlarmManager 或 WorkManager 降级。
- 已确认：开机恢复只监听 `BOOT_COMPLETED`，未覆盖时区、系统时间、应用升级，
  也未过滤过期提醒或隔离单条调度失败。
- 已完成：新增 `REMINDER_CURRENT_STATE.md`，记录通知 channel、权限、重启恢复、
  已知风险、自动测试范围和雷电复验步骤。
- 已完成：新增五条纯 JVM 基线测试，覆盖新建、修改、移除、完成和 exact 权限拒绝。
- 已验证：任务 domain 5/5 测试和 app debug 构建通过。
- 已验证：雷电 Android 9 创建任务后登记单一 RTC_WAKEUP，到点通知正常；
  修改未来时间复用 alarm ID/requestCode 1，关闭截止时间后 alarm 表和系统调度均清空。
- 已验证：真实流程复现“触发后任务仍保留旧 alarm ID”风险，日志无崩溃、ANR、
  SQLite 约束或调度权限异常。
- 已完成：DF-401 验收关闭，下一步移植 DF-402 调度降级策略。

### DF-402 调度降级模式

- 已完成：提取纯 Kotlin 调度策略，exact 可用时使用精确 alarm 和 5 分钟后备，
  不可用时使用非精确 alarm 和 15 分钟后备。
- 已完成：现有 `AlarmSchedulerImpl` 改为 AlarmManager 与唯一 WorkManager
  双调度，修改和取消会替换/清理同 alarm ID 的两条系统路径。
- 已完成：新增 fallback worker，在 alarm 行仍匹配预期时间时复用现有
  `AlarmReceiver` 发出通知，不建立第二套通知基础设施。
- 已完成：现有 boot receiver 改为触发唯一恢复 worker，并扩展到日期、系统时间、
  时区和应用升级广播，不新增第二个 boot receiver。
- 已修复：exact 权限不可用时仍持久化并调度降级提醒；调度异常会回滚 alarm 行。
- 已修复：`AlarmReceiver` 所有路径通过 `finally` 结束异步广播，悬空 alarm 也会清理。
- 已验证：策略 2/2、任务 domain 6/6 测试通过；notification lint 无问题，
  app debug 与 R8 release 构建通过。
- 已验证：雷电 Android 9 中 alarm ID 2 同时登记 RTC_WAKEUP 和唯一
  `alarm_fallback_2`，后备延迟为目标时间加 5 分钟。
- 已验证：带活动提醒覆盖安装后 restore worker 成功，原 alarm ID/requestCode
  被重建且后备 work UUID 被替换，没有重复提醒。
- 已验证：关闭截止时间后 alarm 表为空、任务 alarm ID 清空、后备 work 取消，
  系统无活动 Daily Flow alarm；日志无崩溃、ANR、Koin、worker 或权限异常。
- 已完成：DF-402 验收关闭，下一步新增统一提醒实体。

## 已具备能力

- Daily Flow 品牌、四栏主导航、完整日历入口和统一内容库
- DeepSeek 等内置 AI Provider，以及 Keystore API Key 迁移
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
- 空间提供“自定义记录”主入口，底部继续保持四栏导航
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
- 主数据库 v7 迁移和 schema 导出
- 雷电模拟器构建、安装、启动和日志收集脚本

## 发布路径

1. 完成 P3 统计与 CSV、P4 多提醒、P5 AI 提案确认、P6 备份与安全。
2. 完成 P7 集成回归、性能和无障碍。
3. 完成 P8 release、GitHub Beta 和 F-Droid 准备。

## 当前风险

- DF-012 Provider 契约测试尚未补齐。
- 根级全模块 instrumentation APK 组装仍受 AI 模块 Netty `META-INF/INDEX.LIST`
  重复资源影响；app instrumentation APK 可正常组装。
- 雷电 Launcher3 不支持 `requestPinAppWidget`，widget 已通过系统绑定实例验证；
  Beta 前需在支持固定小组件的真实 Launcher 上复验添加流程。
- 雷电可用于快速回归，但 Beta 前仍需真实 Android 设备验证提醒、Keystore 和生物识别。
- CI 配置尚待远程 Runner 首次验证。
- 开发工作区位于同步盘；本机构建时需暂停百度同步，避免生成文件被短暂锁定。
