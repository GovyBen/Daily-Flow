# Daily Flow — 数据看板与日历增强开发计划

> 版本：v0.2.0-draft | 编制日期：2026-06-18  
> 状态：草稿，待审批

---

## 1. 背景与现状分析

### 1.1 当前统计模块能力

| 能力 | 现状 | 文件 |
|------|------|------|
| 单 Tracker 选择 | ✅ 下拉菜单选择 | `TrackingAnalyticsScreen.kt` |
| 时间范围 | ✅ DAY / WEEK / MONTH | `TrackingAnalyticsRange` |
| 聚合方式 | ✅ SUM / COUNT / AVG / MIN / MAX | `AggregationOperation` |
| 图表类型 | ✅ LINE / BAR（单 tracker） | `TrackingLineChart`, `TrackingBarChart` |
| 饼图 | ✅ 选项分布 | `TrackingPieChart` |
| 连续天数 | ✅ 当前/最长 | `StreakCards` |
| 今日摘要 | ✅ 样本数/和/均值/最大/最小 | `DailySummaryCard` |
| **多 Tracker 叠加** | ❌ **不支持** — 一次只看一个 tracker | — |
| **年范围** | ❌ 最大 MONTH = 12个月 | — |
| **仪表盘首页** | ❌ 概览页的情绪统计显示"暂无数据" | — |

### 1.2 当前日历 Tracking 显示

| 现状 | 问题 |
|------|------|
| `CalendarDayCell` 显示"是否有记录"布尔指示条（3px 横线） | 信息量太少 |
| 不显示具体 tracker 名称或数值 | 用户不知道今天记录了什么 |
| `hasTrackingRecords` 来自 `observeCalendarRecords` | 但未关联数值 |

### 1.3 情绪统计"暂无数据"问题

概览页的情绪统计（`DashboardScreen` 中的情绪卡片）依赖 `diary` 模块的日记 mood 数据，而非 tracking 模块的"心情"模板数据。这是两个独立系统，需要统一。

---

## 2. 开发任务

### 阶段 D1：多 Tracker 叠加波形图（DF-1001）

**目标**：用户可以同时选择多个 tracker（如"心情"+"社交次数"），在同一张折线图上以不同颜色叠加显示，直观对比趋势。

**实施**：
1. **Analytics 模型扩展**：
   - `TrackingAnalyticsUiState` 新增 `selectedTrackers: List<String>`（多选）
   - `TrackingAnalyticsRequest` 改为接受 tracker 列表
2. **多系列数据加载**：
   - `TrackingAnalyticsLoader` 支持并行加载多个 tracker 的 series 数据
   - 新增 `MultiSeriesData` 包装多组 series
3. **图表组件**：
   - `TrackingLineChart` 改造为支持多系列（AndroidPlot `XYPlot` 本身支持多 `SimpleXYSeries`）
   - 每条线不同的颜色，图例自动显示 tracker 名称
   - 新增 `TrackingMultiLineChart` Composable
4. **UI 改造**：
   - Tracker 选择器从单选 `DropdownMenu` 改为多选 `FilterChip` 组
   - 选中多个后，图表自动切换到多系列模式

**验收**：选择"心情"和"社交次数"两个 tracker → 折线图上两条不同颜色的线 → 可切换日/周/月范围 → 图例正确标注。

### 阶段 D2：年范围 + 仪表盘增强（DF-1002）

**目标**：在概览页建立真正的数据仪表盘，替代当前「暂无数据」的空态。

**实施**：
1. **新增 YEAR 范围**：
   - `TrackingAnalyticsRange` 新增 `YEAR` 枚举值
   - `dateRange()` 方法返回过去 12 个月
   - 月度 bin（`DateTimeUnit.MONTH`）
2. **仪表盘 Dashboard 组件**：
   - 创建 `TrackingDashboardChart` — 概览页嵌入的小型多系列折线图
   - 用户可选定"关注的 tracker"（固定模板设置），自动在仪表盘显示
   - 替换当前概览页的空情绪统计卡片
   - 保留原有日记 mood 图表作为独立卡片
3. **数据桥接**：
   - 将 tracking 的"心情" tracker 数据同步到 diary mood 统计
   - 或在概览页同时展示两种来源（tracking 心情 + diary 心情）

**验收**：概览页仪表盘显示选定 tracker 的趋势折线图（小型）→ 可点击进入完整统计页 → 有记录时显示数据，无记录时显示"暂无数据"。

### 阶段 D3：日历日期格显示数值（DF-1003）

**目标**：用户可配置某些 tracker（如吸烟次数）在日历日期格中直接显示数值，而非仅 3px 横线。

**实施**：
1. **Tracker 配置**：
   - 模板编辑器中新增选项：`showOnCalendar: Boolean`
   - 启用的 tracker 标记为"日历显示"
2. **数据加载**：
   - `ObserveCalendarRecordsUseCase` 扩展为返回 `<LocalDate, Map<TrackerId, Value>>`
   - `CalendarViewModel` 新增 `calendarTrackingValues: Map<LocalDate, List<CalendarTrackingValue>>`
3. **UI 改造**：
   - `CalendarDayCell` 接收 `trackingValues: List<CalendarTrackingValue>` 参数
   - 在日期数字下方显示小号数值文字（如 "🚬3" 或仅数字 "3"）
   - 最多显示 2 个 tracker 值，超出显示 "+N" 省略
   - 颜色跟随 tracker 模板颜色
4. **配置界面**：
   - 单独一个设置页"日历显示设置"（或在模板编辑器中）
   - 列出所有数值型 tracker，勾选是否在日历显示

**验收**：在日历月视图中，标记为"日历显示"的 tracker 数值出现在对应日期格中 → 格式清晰不溢出 → 切换月份数据正确更新。

### 阶段 D4：仪表盘综合增强（DF-1004）

**目标**：概览页成为真正的"数据驾驶舱"。

**实施**：
1. **今日摘要增强**：
   - 当前 `TrackingDashboardSection` 只显示"今天 N 条记录"，扩展为：
   - 每个模板一行：名称 + 今日次数 + 迷你趋势图（sparkline）
2. **周趋势**：
   - 新增 7 天迷你折线图（sparkline），无需坐标轴
3. **情绪统计桥接**：
   - 统一 tracking"心情"和 diary mood 数据源
   - 概览页情绪卡片接收 tracking 数据后正确显示
4. **AI 总结**：
   - 复用 DF-509 的 `summarizeStatistics` — 发送仪表盘摘要给 AI 生成文字总结

**验收**：概览页各卡片有实际数据展示 → sparkline 趋势可见 → 点击可深入详情。

---

## 3. 执行顺序

```text
DF-1001 (多系列波形图)  ────→  DF-1002 (年范围+仪表盘)
                                      │
                                      ▼
                               DF-1003 (日历数值显示)
                                      │
                                      ▼
                               DF-1004 (仪表盘综合)
```

---

## 4. 技术依赖

| 依赖 | 说明 |
|------|------|
| AndroidPlot 1.5.11 | 已集成，支持多系列 XY Plot |
| `ObserveCalendarRecordsUseCase` | 需扩展返回具体数值 |
| `TrackingAnalyticsLoader` | 需支持并行请求 |
| `CalendarDayCell` | 需接收新数据模型 |

---

## 5. 风险评估

| 风险 | 影响 | 缓解 |
|------|------|------|
| 多系列数据量大导致 ANR | 日历滑动卡顿 | 分页加载 + 后台线程计算 |
| 日历日期格空间不足 | 数值溢出 | 限制显示数量和字号 |
| 情绪数据源不一致 | 两张表数据不同步 | 明确数据归属，显示时合并 |
| 图表库限制 | 多系列某些聚合不支持 | 先评估再实现，必要时降级 |
