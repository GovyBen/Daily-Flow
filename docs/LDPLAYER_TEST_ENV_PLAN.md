# Daily Flow：雷电模拟器测试环境配置计划

版本：v1.0  
编制日期：2026-06-13  
用途：作为独立 Codex 对话的执行上下文  
状态：仅规划，尚未配置

## 1. 目标

把本机已有雷电模拟器配置为 Daily Flow 的快速 Android 测试设备，并建立稳定、可重复的以下流程：

1. 启动指定模拟器实例。
2. 使用同一套 `adb` 连接。
3. 安装或覆盖安装 Debug APK。
4. 启动、停止、清除日志和收集崩溃信息。
5. 验证日历、通知、提醒、备份、网络和基础 UI。
6. 用脚本减少每次测试的手工步骤。

本计划不负责开发 Daily Flow 功能，也不在当前对话实际修改雷电配置。

## 2. 当前已知主机信息

工作区：

```text
C:\Users\10844\Documents\BaiduSyncdisk\Codex Projects\Daily_Flow
```

已知环境：

- 操作系统：Windows。
- Shell：PowerShell。
- 已安装 Git。
- 已发现 JDK 17 和 JDK 21。
- My Brain 上游使用 Gradle Wrapper，CI 基线为 JDK 17。
- 当前未确认官方 Android SDK 是否安装完整。
- 当前未确认 `adb` 是否已加入 `PATH`。
- 已发现雷电模拟器 9，目录：

```text
C:\leidian\LDPlayer9
```

该目录预期包含：

```text
adb.exe
ldconsole.exe
dnconsole.exe
dnplayer.exe
```

以上信息在新对话开始时必须重新检查，不应假设路径和版本始终不变。

## 3. 雷电模拟器是否方便

结论：方便用于日常快速测试，但不能作为唯一发布验收环境。

适合：

- 快速安装 APK。
- Compose 页面和导航回归。
- Room 数据增删改查。
- 网络请求和 DeepSeek 联调。
- 普通通知。
- Calendar Provider 基础读写。
- 备份导入导出。
- 截图、录屏和重复执行冒烟流程。
- 使用实例快照快速恢复测试状态。

局限：

- 不是 Google 官方 AVD，系统镜像和厂商行为可能不同。
- x86/x86_64 环境不能代替真实 ARM 设备。
- 精确闹钟、Doze、电池优化和后台限制可能与真机不同。
- Calendar Provider 的预装应用和账户行为可能不同。
- 生物识别和硬件级 Keystore 不能可靠代表真机。
- 输入法语音、麦克风路由和通知权限界面可能受模拟器设置影响。
- 不能单独证明 F-Droid 构建和真实设备兼容性。

发布前仍需：

- 至少一台真实 Android 设备。
- 最好增加一个官方 Android Studio AVD。
- 对提醒、重启恢复、Keystore 和生物识别做真机复测。

## 4. 配置原则

1. 构建使用官方 Android SDK 和项目 Gradle Wrapper。
2. 雷电只作为运行设备，不使用其内部文件替代 compileSdk/build-tools。
3. 优先统一使用官方 `platform-tools\adb.exe`。
4. 若暂时只能使用雷电自带 `adb.exe`，必须记录版本并避免两个 adb server 互相抢占。
5. 项目默认使用 JDK 17，除非基线构建证明上游需要其他版本。
6. 不把 DeepSeek API Key 写入 PowerShell 脚本、Gradle 文件或 Git。
7. 不在未经确认的情况下清除用户现有雷电实例数据。
8. 不修改其他模拟器实例。
9. 自动化脚本必须接收实例名或端口，不写死未经确认的值。
10. 对危险操作如 `pm clear`、删除快照和重建实例，执行前必须明确提示。

## 5. 独立对话的执行阶段

### LDP-001 只读盘点

目标：确认工具、实例、版本和当前运行状态，不做配置更改。

检查命令示例：

```powershell
Get-Command java, git, adb -ErrorAction SilentlyContinue
java -version
git --version
Get-ChildItem 'C:\leidian\LDPlayer9' -Filter '*.exe'
& 'C:\leidian\LDPlayer9\ldconsole.exe' list2
& 'C:\leidian\LDPlayer9\adb.exe' version
```

还需检查：

- `ANDROID_HOME`
- `ANDROID_SDK_ROOT`
- `JAVA_HOME`
- 用户和系统 `PATH`
- Android SDK Manager、platform-tools、platform 36、build-tools。
- 雷电实例的名称、索引、Android 版本、是否已启动。

验收：

- 列出所有雷电实例。
- 标记将用于 Daily Flow 的唯一实例。
- 记录 adb 版本和路径。
- 未修改任何环境变量和实例数据。

### LDP-002 准备官方 Android SDK

目标：让 Gradle 能可靠构建 My Brain / Daily Flow。

需要的最小组件：

```text
cmdline-tools
platform-tools
platforms;android-36
build-tools;36.x
```

具体 build-tools 版本以项目 Gradle 同步要求为准，不盲目安装全部版本。

建议 SDK 路径：

```text
C:\Users\10844\AppData\Local\Android\Sdk
```

配置：

```text
ANDROID_SDK_ROOT=<SDK 路径>
JAVA_HOME=<JDK 17 路径>
PATH += <SDK>\platform-tools
PATH += <SDK>\cmdline-tools\latest\bin
```

执行要求：

- 优先检查 Android Studio 是否已有 SDK，避免重复下载。
- 接受 licenses 前展示将执行的 sdkmanager 命令。
- 配置后新开 PowerShell 验证环境变量。
- 项目内可使用不含私密信息的 `local.properties` 指向 SDK；不得提交个人绝对路径。

验收：

```powershell
adb version
sdkmanager --list
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
```

### LDP-003 选定并启动专用实例

目标：使用一个明确的雷电实例测试 Daily Flow。

步骤：

1. 使用 `ldconsole list2` 获取实例索引和名称。
2. 若已有合适实例，先询问是否复用。
3. 若需新建实例，记录 Android 版本、CPU、内存、分辨率和存储。
4. 启动实例。
5. 等待 `sys.boot_completed=1`，不要仅以窗口出现判断启动完成。

建议配置起点：

```text
CPU: 4 cores
Memory: 4096 MB
Resolution: 1080 x 1920 或接近常见手机比例
Storage: 至少 8 GB 可用
Root: 默认关闭
ADB debugging: 开启
Timezone: Asia/Shanghai
Language: 中文，另做英文/大字体测试
```

实际配置应根据主机资源和现有实例决定，不应直接覆盖用户配置。

验收：

- 只启动选定实例。
- Android 桌面可用。
- 时区和系统时间正确。
- 重启实例后配置保持。

### LDP-004 统一 adb

目标：避免官方 adb 与雷电 adb 的 server/client 版本冲突。

步骤：

1. 分别记录两个 `adb version`。
2. 查明雷电实例实际 adb endpoint，不假设固定为 `5555`。
3. 优先使用 `ldconsole list2`、雷电控制台输出或当前 `adb devices` 确认端口。
4. 停止冲突的 adb server。
5. 用选定 adb 启动 server 并连接 endpoint。
6. 确认设备状态为 `device`，而不是 `offline` 或 `unauthorized`。

命令形式：

```powershell
adb kill-server
adb start-server
adb connect 127.0.0.1:<实际端口>
adb devices -l
adb -s 127.0.0.1:<实际端口> shell getprop ro.build.version.release
adb -s 127.0.0.1:<实际端口> shell getprop ro.product.cpu.abi
adb -s 127.0.0.1:<实际端口> shell getprop sys.boot_completed
```

故障处理顺序：

1. 检查实例是否完成启动。
2. 检查 endpoint 和端口。
3. 检查是否有多个 `adb.exe` 进程。
4. 统一 adb 版本后重启 server。
5. 仍失败时重启指定实例，而不是重启或删除全部实例。

验收：

- 连续三次 `adb devices -l` 都显示同一设备。
- 新 PowerShell 会话可使用同一 adb。
- 不需要手工切换两个 adb 可执行文件。

### LDP-005 构建、安装和启动 Debug APK

目标：建立最小运行闭环。

步骤：

```powershell
.\gradlew.bat :app:assembleDebug
adb -s <serial> install -r .\app\build\outputs\apk\debug\app-debug.apk
adb -s <serial> shell monkey -p <applicationId> 1
```

需从 Gradle/manifest 读取真实：

- APK 路径。
- applicationId。
- launcher activity。

不要长期依赖 `monkey`；确认 activity 后可使用：

```powershell
adb -s <serial> shell am start -n <applicationId>/<activity>
```

验收：

- APK 安装成功。
- launcher 启动。
- 首次启动无崩溃。
- `adb logcat` 中没有 fatal exception。

### LDP-006 权限和系统能力准备

需要检查：

- Android 13+ 通知权限。
- Calendar 读写权限。
- 精确闹钟特殊访问权限。
- 电池优化。
- 文件创建/Storage Access Framework。
- 生物识别或设备凭据可用性。

原则：

- 普通 runtime permission 通过应用真实流程申请。
- 特殊访问权限优先手工确认界面，记录步骤。
- 不用 `pm grant` 掩盖错误的权限请求逻辑。
- `pm grant` 只用于重复回归的辅助路径，并同时保留首次授权测试。

验收：

- 拒绝、仅本次允许、允许和设置中撤销都有记录。
- 无权限时应用显示可恢复错误，不崩溃。

### LDP-007 创建 PowerShell 辅助脚本

建议后续在项目中增加：

```text
tools/android/env.ps1
tools/android/ldplayer.ps1
tools/android/smoke.ps1
```

`env.ps1`：

- 检查 JDK、SDK、Gradle、adb。
- 输出版本和缺失项。
- 不永久修改系统环境变量，除非用户明确批准。

`ldplayer.ps1`：

- 参数化实例索引、adb 路径和 endpoint。
- 支持 `status`、`start`、`connect`、`stop`。
- 默认不执行删除、重建或清数据。

`smoke.ps1`：

- 构建 Debug APK。
- 等待设备启动。
- 安装 APK。
- 清空 logcat。
- 启动应用。
- 等待并收集 crash/ANR。
- 输出明确退出码。

脚本要求：

- `Set-StrictMode -Version Latest`。
- `$ErrorActionPreference = 'Stop'`。
- 所有路径使用 `-LiteralPath` 或正确参数化。
- 不保存 API Key。
- 不在未确认时调用 `pm clear`。
- 出错时打印实际命令阶段，但不打印 secret。

### LDP-008 建立模拟器快照

目标：为重复测试创建可恢复状态。

建议快照：

1. `daily-flow-clean`：系统启动、无应用数据。
2. `daily-flow-permissions`：权限已设置。
3. `daily-flow-sample-data`：有非敏感测试数据。

要求：

- 创建前确认雷电版本是否支持对应实例快照。
- 不把真实 API Key 或私人日历数据放入快照。
- 每个快照记录创建日期、应用版本和数据库版本。

验收：

- 从 clean 快照恢复后可重新安装。
- sample-data 快照只含虚构数据。

## 6. Daily Flow 雷电测试矩阵

### 6.1 基础安装

- [ ] 全新安装。
- [ ] 覆盖安装。
- [ ] 升级安装并保留数据。
- [ ] 卸载后重装。
- [ ] applicationId 和 provider authority 无冲突。

### 6.2 结构化记录

- [ ] 创建模板。
- [ ] 多选同一时间保存多个选项。
- [ ] 计数、滑条、布尔、时长、数值、单选和文本。
- [ ] 编辑发生时间。
- [ ] 删除和撤销。
- [ ] 旋转、切后台和进程回收。
- [ ] 大量数据下统计页面。

### 6.3 任务和日历

- [ ] 创建、完成和重复任务。
- [ ] 创建系统日历事件。
- [ ] 拒绝 Calendar 权限。
- [ ] 在系统日历外部修改或删除事件。
- [ ] 一个目标配置多个提醒。

### 6.4 提醒

- [ ] 应用前台、后台和被划掉时通知。
- [ ] 模拟器重启后恢复。
- [ ] 修改系统时间和时区后重算。
- [ ] exact alarm 权限拒绝后的降级。
- [ ] 多个提醒不会覆盖或重复。
- [ ] 点击通知进入正确目标。

雷电上的提醒通过不代表真机通过，必须在真实设备复测 Doze 和厂商后台限制。

### 6.5 DeepSeek

- [ ] 本地输入法文本输入。
- [ ] 可选测试语音输入法。
- [ ] 正常 API Key。
- [ ] 错误 Key、限流、断网和超时。
- [ ] 模糊时间触发追问。
- [ ] 确认前数据库无变化。
- [ ] 取消不写入。
- [ ] 重复点击确认只执行一次。

API Key 应在应用 UI 中临时输入，不进入脚本、截图、日志或测试报告。

### 6.6 安全和备份

- [ ] API Key 设置、替换和删除。
- [ ] preferences 中无明文 Key。
- [ ] 应用锁。
- [ ] 最近任务缩略图。
- [ ] JSON 备份和恢复。
- [ ] 损坏备份安全失败。
- [ ] CSV 中文、逗号、引号和换行。

雷电不用于最终证明：

- 硬件级 Keystore。
- StrongBox。
- 真实生物识别安全等级。
- OEM 锁屏和后台策略。

### 6.7 显示和无障碍

- [ ] 竖屏和横屏。
- [ ] 深色模式。
- [ ] 大字体。
- [ ] 中文和英文。
- [ ] 不同分辨率。
- [ ] TalkBack 基础导航，若模拟器环境可用。

## 7. 日常冒烟测试

每次可安装里程碑执行：

1. 构建 Debug APK。
2. 确认模拟器 boot completed。
3. 覆盖安装。
4. 启动并观察 10 秒。
5. 创建一个任务并标记完成。
6. 创建一个健身记录。
7. 查看今日页和统计页。
8. 创建一个 2 分钟后的提醒。
9. 导出备份。
10. 切后台并等待提醒。
11. 收集 fatal、ANR 和 SecurityException。

建议日志命令：

```powershell
adb -s <serial> logcat -c
adb -s <serial> logcat -v threadtime
adb -s <serial> shell dumpsys package <applicationId>
adb -s <serial> shell dumpsys alarm
adb -s <serial> shell dumpsys notification
```

日志保存前应检查是否含 API Key、Authorization header、完整 prompt 或私人记录。

## 8. 配置完成的验收标准

- [ ] JDK 17 被 Gradle 正确使用。
- [ ] 官方 Android SDK 可用。
- [ ] `platform-tools`、目标 platform 和 build-tools 已安装。
- [ ] 项目能生成 Debug APK。
- [ ] 已选定唯一雷电实例。
- [ ] 单一 adb 客户端可稳定连接。
- [ ] 可脚本化安装和启动。
- [ ] 权限测试路径已记录。
- [ ] 可收集 logcat、alarm 和 notification 状态。
- [ ] clean 和 sample-data 测试状态可恢复。
- [ ] 没有真实 API Key 或个人数据进入脚本和快照。
- [ ] 文档明确列出仍需真机验证的项目。

## 9. 常见问题处理

### adb 显示 offline

1. 等待 `sys.boot_completed`。
2. 确认 endpoint。
3. 结束冲突 adb server。
4. 用统一 adb 重新 `start-server` 和 `connect`。
5. 只重启目标实例。

### Gradle 找不到 SDK

1. 检查 `ANDROID_SDK_ROOT`。
2. 检查项目 `local.properties`。
3. 确认路径无错误转义。
4. 运行 `sdkmanager --list`。
5. 确认项目所需 platform 已安装。

### APK 安装失败

按错误分类：

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`：签名或 applicationId 冲突。
- `INSTALL_FAILED_VERSION_DOWNGRADE`：版本号降低。
- `NO_MATCHING_ABIS`：原生库 ABI 不匹配。
- 存储不足：清理目标实例而不是删除其他实例。

### 通知不触发

检查：

1. 通知 runtime permission。
2. notification channel 是否被关闭。
3. exact alarm 权限。
4. `dumpsys alarm` 是否存在目标 alarm。
5. 应用是否被雷电后台策略限制。
6. WorkManager fallback 状态。

### Calendar Provider 不可用

1. 检查模拟器是否包含系统日历 provider。
2. 检查 Calendar runtime permission。
3. 使用 `content query` 只读检查 provider。
4. 若镜像缺少完整日历能力，记录为雷电限制并转官方 AVD/真机验证。

## 10. 新对话交接提示词

可在单独对话中直接使用以下内容：

```text
请读取：
C:\Users\10844\Documents\BaiduSyncdisk\Codex Projects\Daily_Flow\docs\LDPLAYER_TEST_ENV_PLAN.md

按文档从 LDP-001 开始配置 Daily Flow 的雷电模拟器测试环境。

要求：
1. 先只读盘点本机 JDK、Android SDK、adb、雷电安装目录和实例。
2. 当前已知雷电可能位于 C:\leidian\LDPlayer9，但必须重新验证。
3. 不删除、重建或清空任何现有模拟器实例，除非先得到我的明确同意。
4. 构建使用官方 Android SDK 和项目 Gradle Wrapper；优先统一使用官方 adb。
5. 使用 JDK 17 作为初始基线。
6. 不把 DeepSeek API Key 写入脚本、Gradle、日志或仓库。
7. 每完成一个 LDP 任务，更新计划状态并记录验证命令。
8. 最终建立可重复的构建、连接、安装、启动和日志收集流程。
```

## 11. 新对话应交付的结果

独立配置对话结束时应在项目内留下：

- 环境盘点结果。
- SDK/JDK/adb 最终路径说明。
- 选定雷电实例和 endpoint。
- 不含 secret 的辅助脚本。
- 构建、安装、启动验证记录。
- 权限与提醒手工测试说明。
- 雷电已验证项和仍需真机验证项。

除非用户另行批准，该对话不应开始 Daily Flow 业务功能开发。
