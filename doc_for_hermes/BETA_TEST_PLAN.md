# Daily Flow v0.9 Beta — 全面功能测试方案

> 编制日期：2026-06-17  
> 测试环境：LDPlayer 9 (1080×1920, Android 9, API 28)  
> 应用包名：`com.dailyflow.app.debug`  
> ADB 路径：`C:/Users/10844/AppData/Local/Android/Sdk/platform-tools/adb.exe -s emulator-5556`

---

## 约定

- 所有坐标基于 **1080×1920** 屏幕分辨率。如屏幕尺寸不同，需按比例缩放。
- 每步操作后 **sleep 2** 等待 UI 渲染。
- 用 `vision_analyze` 验证每步预期结果。失败则截图留证。
- 全程监控 logcat：`adb logcat -d | grep -iE "crash|fatal|ANR|exception"`
- ADB 变量简写：下文 `$ADB` = `"C:/Users/10844/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s emulator-5556`

---

## 0. 环境准备

```bash
# 强制停止 + 清日志 + 安装最新 Debug APK
$ADB shell am force-stop com.dailyflow.app.debug
$ADB logcat -c
$ADB install -r "$LOCALAPPDATA/DailyFlow/build/app/outputs/apk/debug/app-debug.apk"

# 启动应用
$ADB shell monkey -p com.dailyflow.app.debug -c android.intent.category.LAUNCHER 1
sleep 3
# 截图验证：应显示"空间"页或启动页，无崩溃弹窗
$ADB shell "screencap -p /sdcard/test_00_launch.png"
```

---

## 1. 主导航与底栏

### 1.1 四栏切换

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1.1a | 点击 **概览** 标签 `$ADB shell input tap 200 1815` | 标题显示"概览"，搜索图标在右上角 |
| 1.1b | 点击 **日历** 标签 `$ADB shell input tap 400 1815` | 标题显示"日历"，月视图可见 |
| 1.1c | 点击 **空间** 标签 `$ADB shell input tap 680 1815` | 标题显示"空间"，2×2 卡片网格 |
| 1.1d | 点击 **设置** 标签 `$ADB shell input tap 950 1815` | 标题显示"设置"，设置列表可见 |

每步后截图：`$ADB shell "screencap -p /sdcard/test_01_<tab>.png"` → pull → vision_analyze

### 1.2 概览页全部组件

```
操作：点击概览 tab → 滚动到底查看所有卡片
```

```bash
# 截图概览顶部
$ADB shell "screencap -p /sdcard/test_01_dashboard_top.png"
# 向下滚动
$ADB shell input swipe 540 1200 540 200 300 ; sleep 1
# 截图概览底部
$ADB shell "screencap -p /sdcard/test_01_dashboard_bottom.png"
```

验证清单：
- [ ] 顶栏标题"概览" + 搜索 🔍 按钮
- [ ] "自定义记录"卡片：健身/心情/习惯次数快捷按钮 + "今天 0 条记录"
- [ ] "日历"卡片（可能需要权限授权提示）
- [ ] "待办事项"卡片 + 添加按钮
- [ ] "情绪统计" + "事项摘要"行
- [ ] "Pending Reminders"卡片（空态："No upcoming reminders"）
- [ ] 全程无崩溃

---

## 2. 空间页与子模块入口

### 2.1 空间页入口卡片

```
操作：导航到空间 tab → 依次点击每个卡片
```

| 步骤 | 点击目标 | 坐标 | 预期目标页面 |
|------|---------|------|-------------|
| 2.1a | **内容库** | `tap 270 500` | 进入内容库（笔记/日记/书签筛选） |
| 2.1b | back 返回 | `tap 55 106` | 回到空间页 |
| 2.1c | **自定义记录** | `tap 810 500` | 进入模板列表页 |
| 2.1d | back 返回 | `tap 55 106` | 回到空间页 |
| 2.1e | **待办事项** | `tap 270 840` | 进入任务列表页 |
| 2.1f | back 返回 | `tap 55 106` | 回到空间页 |
| 2.1g | **Assistant** | `tap 810 840` | 进入 AI 助手页 |

每步截图验证。

---

## 3. Tracking（自定义记录）全流程

### 3.1 模板列表

```
操作：空间 → 自定义记录
```

验证清单：
- [ ] 显示"自定义记录"标题
- [ ] 默认显示 3 个预置模板：健身、心情、习惯次数
- [ ] 每个模板显示名称 + 字段数 + 最后记录时间
- [ ] 右上角有 **+** 创建按钮

截图：`test_03_templates.png`

### 3.2 快速记录

```bash
# 点击"健身"模板
$ADB shell input tap 540 400 ; sleep 2
```

验证清单：
- [ ] 进入快速记录页，显示模板名称
- [ ] 显示所有字段（部位多选、时长、备注等）
- [ ] 发生时间可修改
- [ ] 保存按钮可见

```bash
# 选择"胸"部位（如果可见）
$ADB shell input tap 200 500 ; sleep 1
# 点击保存按钮（通常在底部）
$ADB shell input tap 540 1700 ; sleep 2
```

验证清单：
- [ ] 保存成功提示（显示 session ID）
- [ ] 可继续记录或返回

截图：`test_03_quickrecord.png`

### 3.3 记录历史

```bash
# 返回模板列表 → 点击右上角历史图标
$ADB shell input tap 55 106 ; sleep 1  # back
# 在概览页点击"历史记录"
$ADB shell input tap 200 1815 ; sleep 1  # 概览 tab
# 滚动到自定义记录区域，点击"历史记录"链接
# 或：空间→自定义记录→点击某个模板→查看历史
```

验证清单：
- [ ] 显示历史记录列表
- [ ] 可按模板/日期筛选
- [ ] 每条显示发生时间 + 字段值
- [ ] 点击可编辑、可删除（确认对话框+撤销 Snackbar）

截图：`test_03_history.png`

### 3.4 统计页

```bash
# 在模板列表页，点击统计入口（如果可见）
# 或通过：空间 → 自定义记录 → 点击模板卡片 → 点统计 tab
$ADB shell input tap 540 700 ; sleep 2
```

验证清单：
- [ ] 统计页显示 tracker 选择器
- [ ] 日/周/月范围切换
- [ ] 折线图/柱状图/饼图/分布切换
- [ ] 今日摘要 + 连续天数
- [ ] 空数据时不崩溃（显示空态说明）

截图：`test_03_stats.png`

### 3.5 模板编辑器

```bash
# 在模板列表页，点击 + 创建新模板
$ADB shell input tap 1000 150 ; sleep 2  # 右上角 +
```

验证清单：
- [ ] 进入模板编辑器
- [ ] 可编辑名称、图标、颜色
- [ ] 可添加字段（8 种类型可选）
- [ ] 可添加/排序选项
- [ ] 预览功能
- [ ] 保存/取消

```bash
# 输入模板名称（需要键盘输入，用 input text）
$ADB shell input text "测试模板" ; sleep 1
# 点击保存/确认
$ADB shell input tap 540 1700 ; sleep 2
```

截图：`test_03_editor.png`

### 3.6 CSV 导入导出

```
操作：模板列表页 → 寻找 CSV 导入导出入口
```

验证清单：
- [ ] 可导出 CSV 到下载目录
- [ ] 可选择文件导入
- [ ] 导入前显示预览计数
- [ ] 确认后成功导入

截图：`test_03_csv.png`

---

## 4. 日历模块

### 4.1 月视图

```bash
$ADB shell input tap 400 1815 ; sleep 2  # 日历 tab
$ADB shell "screencap -p /sdcard/test_04_calendar.png"
```

验证清单：
- [ ] 显示完整月视图 + 日期格
- [ ] 日期格显示事件圆点 + 本地记录指示条
- [ ] 可左右滑动切换月份
- [ ] 点击某天显示当日详情

### 4.2 新增日历事件

```bash
# 点击日历中的 + 按钮
$ADB shell input tap 1000 150 ; sleep 2
```

验证清单：
- [ ] 显示"选择目标"对话框：Calendar Provider / 本地 tracking
- [ ] 选择 Calendar Provider → 进入事件编辑页
- [ ] 可设置标题、时间、多提醒
- [ ] 保存后在月视图中可见

截图：`test_04_addevent.png`

### 4.3 日历权限无授权处理

```bash
# 到概览页，查看日历 widget 在无权限时的表现
$ADB shell input tap 200 1815 ; sleep 1  # 概览
```

验证清单：
- [ ] 日历卡片显示权限请求提示
- [ ] "获取权限"链接可点击
- [ ] 不崩溃

---

## 5. 任务模块

### 5.1 任务列表

```bash
# 空间 → 待办事项
$ADB shell input tap 680 1815 ; sleep 1  # 空间
$ADB shell input tap 270 840 ; sleep 2    # 待办事项
```

验证清单：
- [ ] 显示任务列表（空态或已有任务）
- [ ] 排序选项可用
- [ ] + 按钮新建任务

截图：`test_05_tasks.png`

### 5.2 创建任务（含多提醒）

```bash
$ADB shell input tap 1000 150 ; sleep 2  # + 新建
$ADB shell input text "测试任务" ; sleep 1
# 点击截止日期字段
$ADB shell input tap 540 500 ; sleep 1
# 选择未来日期
$ADB shell input tap 700 900 ; sleep 1
# 确认
$ADB shell input tap 900 1300 ; sleep 1
# 添加提醒
$ADB shell input tap 540 800 ; sleep 1     # 提醒区域
$ADB shell input tap 540 1000 ; sleep 1    # "添加提醒"按钮
# 选择快捷选项"提前 5 分钟"
$ADB shell input tap 300 1100 ; sleep 1
# 保存任务
$ADB shell input tap 540 1700 ; sleep 2
```

验证清单：
- [ ] 任务创建成功，返回列表可见
- [ ] 点击任务进入详情，可见多提醒
- [ ] 完成/取消任务
- [ ] 编辑任务可修改提醒

截图：`test_05_taskdetail.png`

---

## 6. 内容库（笔记/日记/书签）

### 6.1 统一内容库

```bash
$ADB shell input tap 680 1815 ; sleep 1  # 空间
$ADB shell input tap 270 500 ; sleep 2    # 内容库
```

验证清单：
- [ ] 顶部筛选栏：全部/笔记/日记/书签
- [ ] 搜索框可用
- [ ] + 新建按钮 → 弹出类型选择
- [ ] 空态不崩溃

截图：`test_06_contentlib.png`

### 6.2 新建日记

```bash
# 点击 + → 选择日记
$ADB shell input tap 1000 106 ; sleep 1
$ADB shell input tap 540 700 ; sleep 2
$ADB shell input text "今天天气不错" ; sleep 1
$ADB shell input tap 540 1700 ; sleep 2  # 保存
```

验证清单：
- [ ] 日记保存成功
- [ ] 内容库中出现新日记条目

### 6.3 新建笔记 + 书签

同上流程，分别测试笔记和书签的创建。

---

## 7. 全局搜索

### 7.1 搜索入口

```bash
$ADB shell input tap 200 1815 ; sleep 1  # 概览
$ADB shell input tap 1030 106 ; sleep 2   # 搜索图标
$ADB shell "screencap -p /sdcard/test_07_search.png"
```

验证清单：
- [ ] 搜索页显示搜索框（自动聚焦）+ 键盘
- [ ] 搜索建议文字可见
- [ ] 输入部分文字后显示按类型分组的结果

```bash
$ADB shell input text "test" ; sleep 2
$ADB shell "screencap -p /sdcard/test_07_search_results.png"
```

验证清单：
- [ ] 结果按 Tasks/Events/Records/Diary/Notes 分组
- [ ] 每组显示类型图标
- [ ] 无结果时显示空状态
- [ ] 返回按钮可用

---

## 8. 设置页

### 8.1 设置项浏览

```bash
$ADB shell input tap 950 1815 ; sleep 2  # 设置
$ADB shell "screencap -p /sdcard/test_08_settings_top.png"
$ADB shell input swipe 540 1500 540 200 200 ; sleep 1
$ADB shell "screencap -p /sdcard/test_08_settings_bottom.png"
```

验证清单：
- [ ] 主题切换（亮/暗/自动）
- [ ] 字体选择
- [ ] Material You 开关（Android 12+）
- [ ] 应用锁开关
- [ ] 备份/恢复入口
- [ ] AI Provider 配置
- [ ] 关于信息 + 版本号
- [ ] 无崩溃

### 8.2 AI Provider 配置

```bash
# 在设置页找到"AI 集成"或"Provider"入口 → 点击
```

验证清单：
- [ ] 显示推荐 Provider：DeepSeek + OpenAI
- [ ] 显示中国大陆 Provider：通义千问、Kimi、智谱 GLM
- [ ] 显示国际 Provider：Gemini、Anthropic、OpenRouter
- [ ] 本地 Provider：Ollama、LM Studio
- [ ] 每个可展开配置 API Key + 模型名
- [ ] 默认选中 DeepSeek

截图：`test_08_aiproviders.png`

---

## 9. 备份与恢复

### 9.1 JSON 备份导出

```bash
# 设置页 → 备份 → 导出 JSON
```

验证清单：
- [ ] 导出成功提示
- [ ] 文件保存到下载目录
- [ ] 文件大小 > 0

### 9.2 备份恢复

```bash
# 设置页 → 备份 → 导入 → 选择刚才导出的文件
```

验证清单：
- [ ] 预览显示数据计数
- [ ] 确认后恢复成功
- [ ] 应用数据完整

---

## 10. 应用锁

### 10.1 应用锁开关

```bash
# 设置页 → 应用锁 → 开启
```

验证清单：
- [ ] 开启后切换到后台再返回 → 显示锁屏
- [ ] 设备凭据验证可用
- [ ] 关闭后不再显示锁屏

---

## 11. AI 助手

### 11.1 进入 AI 助手

```bash
$ADB shell input tap 680 1815 ; sleep 1  # 空间
$ADB shell input tap 810 840 ; sleep 2    # Assistant
$ADB shell "screencap -p /sdcard/test_11_assistant.png"
```

验证清单：
- [ ] 进入 AI 助手页面
- [ ] 输入框可见
- [ ] 发送消息按钮存在
- [ ] 无 API Key 时显示提示

> **注意**：由于无真实 API Key，仅验证 UI，不测试实际 AI 调用。

---

## 12. 桌面小组件

### 12.1 Widget 注册验证

```bash
# 检查 widget provider 是否注册
$ADB shell dumpsys appwidget | grep -A5 "dailyflow"
```

验证清单：
- [ ] Widget provider 已注册

> LDPlayer Launcher3 不支持 `requestPinAppWidget`，仅验证注册状态。

---

## 13. 崩溃与异常监控

全程每个测试步骤后执行：

```bash
$ADB logcat -d -t 200 | grep -iE "crash|fatal|ANR|AndroidRuntime.*FATAL|dailyflow.*exception"
```

预期：**每次输出为空**。如有输出，截图并记录对应的测试步骤。

---

## 14. 测试截图清单

| 编号 | 内容 | 文件 |
|------|------|------|
| 00 | 启动页 | `test_00_launch.png` |
| 01a | 概览-顶部 | `test_01_dashboard_top.png` |
| 01b | 概览-底部（含 PendingReminders） | `test_01_dashboard_bottom.png` |
| 01c | 日历-月视图 | `test_01_calendar.png` |
| 01d | 空间-网格 | `test_01_spaces.png` |
| 01e | 设置-顶部 | `test_01_settings.png` |
| 03a | 模板列表 | `test_03_templates.png` |
| 03b | 快速记录 | `test_03_quickrecord.png` |
| 03c | 历史页 | `test_03_history.png` |
| 03d | 统计页 | `test_03_stats.png` |
| 03e | 模板编辑器 | `test_03_editor.png` |
| 03f | CSV | `test_03_csv.png` |
| 04a | 日历月视图 | `test_04_calendar.png` |
| 04b | 新增事件 | `test_04_addevent.png` |
| 05a | 任务列表 | `test_05_tasks.png` |
| 05b | 任务详情+多提醒 | `test_05_taskdetail.png` |
| 06 | 内容库 | `test_06_contentlib.png` |
| 07a | 搜索页（空） | `test_07_search.png` |
| 07b | 搜索结果 | `test_07_search_results.png` |
| 08a | 设置页 | `test_08_settings_top.png` |
| 08b | AI Provider 配置 | `test_08_aiproviders.png` |
| 11 | AI 助手 | `test_11_assistant.png` |

---

## 15. 自动化测试脚本（批量执行）

将以下保存为 `test_suite.sh`，在 Git Bash 中运行：

```bash
#!/bin/bash
ADB="C:/Users/10844/AppData/Local/Android/Sdk/platform-tools/adb.exe -s emulator-5556"
TMP="C:/Users/10844/AppData/Local/Temp"

fail() { echo "FAIL: $1"; exit 1; }

echo "=== 0. Setup ==="
$ADB shell am force-stop com.dailyflow.app.debug
$ADB logcat -c
$ADB install -r "$LOCALAPPDATA/DailyFlow/build/app/outputs/apk/debug/app-debug.apk"
$ADB shell monkey -p com.dailyflow.app.debug -c android.intent.category.LAUNCHER 1
sleep 3
$ADB shell "screencap -p /sdcard/test_00_launch.png"
MSYS_NO_PATHCONV=1 $ADB pull /sdcard/test_00_launch.png "$TMP/test_00_launch.png"

echo "=== 1. Navigation ==="
# Dashboard
$ADB shell input tap 200 1815 ; sleep 2
$ADB shell "screencap -p /sdcard/test_01_dash_top.png"
MSYS_NO_PATHCONV=1 $ADB pull /sdcard/test_01_dash_top.png "$TMP/test_01_dash_top.png"

# Scroll down for PendingReminders
$ADB shell input swipe 540 1200 540 200 300 ; sleep 1
$ADB shell "screencap -p /sdcard/test_01_dash_bottom.png"
MSYS_NO_PATHCONV=1 $ADB pull /sdcard/test_01_dash_bottom.png "$TMP/test_01_dash_bottom.png"

# Calendar
$ADB shell input tap 400 1815 ; sleep 2
$ADB shell "screencap -p /sdcard/test_01_calendar.png"
MSYS_NO_PATHCONV=1 $ADB pull /sdcard/test_01_calendar.png "$TMP/test_01_calendar.png"

# Spaces
$ADB shell input tap 680 1815 ; sleep 2
$ADB shell "screencap -p /sdcard/test_01_spaces.png"
MSYS_NO_PATHCONV=1 $ADB pull /sdcard/test_01_spaces.png "$TMP/test_01_spaces.png"

# Settings
$ADB shell input tap 950 1815 ; sleep 2
$ADB shell "screencap -p /sdcard/test_01_settings.png"
MSYS_NO_PATHCONV=1 $ADB pull /sdcard/test_01_settings.png "$TMP/test_01_settings.png"

# Check crashes
CRASHES=$($ADB logcat -d -t 200 | grep -ciE "crash|fatal|ANR")
if [ "$CRASHES" -gt 0 ]; then
    echo "WARNING: $CRASHES crash lines in logcat"
    $ADB logcat -d -t 200 | grep -iE "crash|fatal|ANR"
fi

echo "=== 1. Navigation — DONE ==="
echo "Continue with Sections 2-11 manually, using vision_analyze after each screenshot."
```

> **按此方案执行时**，每截取一张图就调用 `vision_analyze` 验证预期内容。如发现异常，截图留证并记录到日志。全部通过后汇总测试报告。
