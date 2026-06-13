# Daily Flow 产品结构修订计划

版本：v1.0

日期：2026-06-13

状态：待实施

需求来源：模拟器首轮体验反馈

## 1. 结论

本轮反馈不是单纯的界面微调，而是暴露了 My Brain 上游信息架构与
Daily Flow 产品目标之间的差异。修订后的实施原则如下：

1. 当前主导航固定为 **概览、日历、空间、设置** 四项。
2. “日历事件”和“自定义记录”使用清晰、不同的产品名称。
3. DeepSeek、OpenAI 置顶并显示“推荐”，同时加入常用中国大陆模型
   Provider 预设。
4. 笔记、日记、书签合并为一个用户可见的 **内容库** 功能。
5. 内容库先统一界面和领域适配层，不立即破坏性合并三个现有数据表。
6. 结构化自定义记录仍按 `tracking` 模块实现，完成后与日历日期视图关联。

## 2. 当前问题的代码原因

### 2.1 自定义记录尚未实现

当前 APK 是 My Brain `v3.1.0` 基线加 Daily Flow 的 P0 工程改造。
`tracking` 模块及模板、字段、记录会话、数据点尚未开始，因此当前首页
只能显示上游已有的：

- 系统日历摘要；
- 任务摘要；
- 日记心情统计；
- 事项统计。

原计划中的“自定义事件”更准确的名称应为 **自定义记录**。它对应
`DF-101` 至 `DF-209`，不是当前 `calendar` 模块里的系统日历事件。

### 2.2 完整日历入口过深

完整月历和日期下事件列表已经存在于 `CalendarScreen`，也支持从所选日期
创建事件。但当前底栏只有：

- 概览；
- 空间；
- 设置。

日历被放在“空间”卡片中，因此用户很难发现完整月历。

### 2.3 AI Provider 只有上游通用选项

当前 `AiProvider` 只有 OpenAI、Gemini、Anthropic、OpenRouter、LM Studio
和 Ollama。DeepSeek 尚未成为独立预设，启用 AI 时还会自动选择 OpenAI。

### 2.4 内容功能重复暴露

笔记、日记和书签目前分别拥有独立的模块、列表、编辑页、搜索和数据库表。
它们的数据差异仍然有意义：

- 笔记：正文、文件夹、置顶、外部 Markdown；
- 日记：正文、发生日期、心情；
- 书签：URL、标题、描述。

问题主要是用户入口和操作模型重复，而不是必须立刻把所有数据塞进一张表。

## 3. 修订后的信息架构

### 3.1 底部主导航

| 顺序 | 栏位 | 内容 |
|---|---|---|
| 1 | 概览 | 今日任务、近期事件、快捷入口和摘要 |
| 2 | 日历 | 完整月历、日期事件列表和新增事件 |
| 3 | 空间 | 任务、内容库、自定义记录、统计、AI 助手 |
| 4 | 设置 | 集成、安全、备份、主题和应用设置 |

日历成为一级栏位后，从“空间”移除重复的日历卡片。

### 3.2 日历栏位

日历默认打开月视图，页面包含：

- 月份切换；
- 每日事件指示；
- 选中日期的事件列表；
- 从选中日期新增事件；
- 列表/月历视图切换；
- 日历账户筛选。

结构化记录完成后，选中日期的下方时间线分成两个明确分组：

1. **日历事件**：写入 Android Calendar Provider；
2. **当日记录**：写入 Daily Flow 本地 `tracking` 数据库。

新增按钮弹出类型选择：

- 新增日历事件；
- 添加自定义记录。

在 `tracking` 尚未完成时，只显示“新增日历事件”，不放置无功能占位按钮。

### 3.3 内容库

“笔记、日记、书签”合并为一个“内容库”入口，统一提供：

- 全部、笔记、日记、链接筛选；
- 单一搜索框；
- 按创建时间、更新时间、置顶状态排序；
- 文件夹或集合；
- 统一新增按钮；
- 类型标记和类型特有字段。

新增内容时默认创建普通笔记，并允许切换：

- 添加心情后保存为日记；
- 添加 URL 后保存为链接；
- 纯标题和正文保存为笔记。

第一阶段保留 `NoteRepository`、`DiaryRepository`、`BookmarkRepository`
和现有数据表，通过 `ContentLibraryRepository` 聚合为统一只读模型：

```text
ContentItem
  id
  type: NOTE | DIARY | LINK
  title
  body
  url?
  mood?
  folderId?
  pinned
  createdAt
  updatedAt
```

写操作由类型适配器转发到原 repository。首版不执行三表合一，原因是：

- 外部 Markdown 笔记不一定存储在 Room；
- 日记心情和书签 URL 有不同约束；
- 直接合表会扩大备份、迁移和回滚风险；
- 统一体验不依赖统一物理存储。

待内容库稳定并完成完整迁移测试后，再单独评估是否建立统一存储表。

## 4. AI Provider 预设

### 4.1 排序与标签

Provider 选择器改为分组列表：

1. **推荐**
   - DeepSeek，推荐排序 1；
   - OpenAI，推荐排序 2。
2. **中国大陆**
   - 通义千问；
   - Kimi；
   - 智谱 GLM。
3. **国际**
   - Gemini；
   - Anthropic；
   - OpenRouter。
4. **本地**
   - Ollama；
   - LM Studio。

DeepSeek 和 OpenAI 在折叠选择器、列表项和已选状态中都显示“推荐”标签。
首次启用 AI 时默认选中 DeepSeek，但不自动启用网络请求，也不预置 API Key。

### 4.2 Provider 注册表

不继续把所有差异堆在 Compose 页面中。新增稳定的 Provider 注册表：

```text
AiProviderDefinition
  stableId
  displayName
  category
  recommendedRank?
  defaultBaseUrl
  defaultModel
  apiKeyHelpUrl
  modelHelpUrl
  protocol
  capabilities
```

`stableId` 不使用 enum ordinal，避免今后增加 Provider 后破坏已有偏好。

### 4.3 初始默认值

以下值按 2026-06-13 官方文档建立，并允许用户编辑：

| Provider | 默认 Base URL | 初始模型 |
|---|---|---|
| DeepSeek | `https://api.deepseek.com` | `deepseek-v4-flash` |
| OpenAI | `https://api.openai.com/v1` | `gpt-5.5` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |
| Kimi | `https://api.moonshot.cn/v1` | `kimi-k2.6` |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4/` | `glm-5.1` |

模型名属于可更新配置。每次发布前重新核对官方文档，不在数据库迁移中强制覆盖
用户已经修改的模型。

### 4.4 兼容策略

DeepSeek、通义千问、Kimi 和智谱 GLM 均通过 OpenAI-compatible 客户端接入，
但不能假设所有兼容端点支持完全相同的参数。每个预设必须分别测试：

- 最小文本请求；
- 流式响应；
- Tool Calling；
- 多工具调用；
- 不支持参数的降级；
- 认证、限流、网络和模型不存在错误。

连接测试不得写入任务、日历或记录。Tool Calling 测试只使用无副作用的 fake tool。

API Key 进入生产设置前，必须先完成 `DF-601` 至 `DF-603`，禁止继续把新增
Provider Key 明文写入 DataStore。

## 5. 实施任务

### 批次 A：导航纠偏

#### DF-008 日历升级为主导航栏位

- 在内部主导航增加 Calendar tab。
- 区分 tab 导航控制器和应用级详情导航控制器。
- 月历留在底栏作用域内；事件详情仍由应用级导航承载。
- 概览日历卡片切换到 Calendar tab。
- 从空间移除日历卡片。
- 更新默认启动页选项和进程恢复测试。

验收：

- 底栏依次显示概览、日历、空间、设置；
- 月历、日期选择、新增事件和详情返回正常；
- 从 Deep Link 打开日历后仍能回到带底栏的主界面；
- 当前 Android 9 / API 28 雷电实例完成导航冒烟测试；
- Android 12+ 权限和后台行为按 `docs/APP_TEST_ENVIRONMENT.md` 的缺口，
  后续使用官方 AVD 或实体设备补测。

### 批次 B：Provider 预设

#### DF-009 提前完成 SecretStore 审计与迁移

- 执行原 `DF-601` 至 `DF-603`。
- 所有云 Provider 通过同一 SecretStore 存取 Key。
- 备份和日志不包含 Key。

#### DF-010 建立 Provider 注册表

- 引入稳定 Provider ID 和分类。
- 保留现有用户 Provider 选择的迁移映射。
- OpenAI-compatible Provider 复用 Koog/OpenAI 客户端。
- 不复制网络栈。

#### DF-011 增加推荐和中国大陆预设

- 增加 DeepSeek、通义千问、Kimi、智谱 GLM。
- DeepSeek 和 OpenAI 置顶并显示“推荐”。
- 首次启用默认选择 DeepSeek。
- Provider、模型和 Base URL 均可编辑或恢复默认。

#### DF-012 增加连接和能力测试

- fake server 契约测试；
- 官方端点手工连接测试；
- Tool Calling 能力探测；
- 错误分类和用户提示；
- 禁止测试工具产生真实写操作。

### 批次 C：内容功能合并

#### DF-013 建立内容库统一入口

- 空间只显示一个“内容库”卡片。
- 内容库提供全部、笔记、日记、链接筛选。
- 旧入口暂时保留内部路由，外部不再重复展示。

#### DF-014 建立聚合领域模型和查询

- 新增 `ContentItem` 和 `ContentLibraryRepository`。
- 聚合三个现有 repository 的 Flow。
- 统一搜索、排序、空状态和错误状态。
- 不让 Compose 直接访问三个 DAO。

#### DF-015 统一新增、编辑和深链

- 统一新增入口，根据字段确定内容类型。
- 保留心情、URL、文件夹和外部 Markdown 特性。
- 旧深链重定向到内容库详情。
- JSON 备份继续兼容旧结构。

### 批次 D：自定义记录与日历关联

`DF-101` 至 `DF-209` 继续实现结构化记录。新增：

#### DF-210 按日期整合日历事件和自定义记录

- 前置：`DF-206`、`DF-207`、`DF-008`。
- 日历日期格分别显示事件和记录指示。
- 日期详情按“日历事件”和“当日记录”分组。
- 两类数据保留各自写入边界和权限处理。
- 全日事件、跨日事件和记录发生时间使用设备时区正确归日。

## 6. 建议执行顺序

1. `DF-008`：先修正最影响体验的日历主入口。
2. `DF-601` 至 `DF-603`：先保护现有和新增 API Key。
3. `DF-010` 至 `DF-012`：完成 AI Provider 预设。
4. `DF-013` 至 `DF-015`：合并内容库用户入口。
5. `DF-101` 至 `DF-110`：实现结构化记录数据层。
6. `DF-201` 至 `DF-207`：实现自定义记录 UI。
7. `DF-210`：把自定义记录接入日期视图。
8. 再进入统计、提醒和 AI 提案执行阶段。

每个批次均生成 Debug APK，并在已经配置的雷电模拟器环境执行对应的冒烟测试。

## 7. 非目标

本次修订不包含：

- 立即删除 notes、diary、bookmarks Gradle 模块；
- 未经迁移测试删除旧表；
- 把日历事件和自定义记录写入同一张表；
- 自动上传本地内容到任一模型 Provider；
- 为每个 Provider 新建一套网络客户端；
- 因模型更新覆盖用户自定义模型名。

## 8. 官方接口参考

以下文档仅用于建立预设和兼容测试，实施时仍需在发布前复核：

- OpenAI latest model：
  <https://developers.openai.com/api/docs/guides/latest-model>
- OpenAI models：
  <https://developers.openai.com/api/docs/models>
- DeepSeek API：
  <https://api-docs.deepseek.com/>
- DeepSeek Tool Calls：
  <https://api-docs.deepseek.com/guides/tool_calls>
- 通义千问 OpenAI 兼容接口：
  <https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope>
- Kimi API：
  <https://platform.moonshot.cn/docs/api/chat>
- Kimi Tool Calls：
  <https://platform.moonshot.cn/docs/guide/use-kimi-api-to-complete-tool-calls>
- 智谱 GLM OpenAI SDK：
  <https://docs.bigmodel.cn/cn/guide/develop/openai/introduction>
- 智谱模型概览：
  <https://docs.bigmodel.cn/cn/guide/start/model-overview>
