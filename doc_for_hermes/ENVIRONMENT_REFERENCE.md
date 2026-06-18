# Daily Flow 测试环境操作参考

> 更新：2026-06-18 | 供 Hermes Agent 和开发者使用

---

## 1. LDPlayer 模拟器

### 环境信息

| 项目 | 值 |
|------|-----|
| 软件 | LDPlayer 9.1.80.0 |
| 安装路径 | `C:\leidian\LDPlayer9` |
| 管理工具 | `C:\leidian\LDPlayer9\ldconsole.exe` |
| 实例名 | `Daily Flow Test` |
| 实例 index | 1 |
| ADB serial | `emulator-5556` |
| ADB endpoint | `127.0.0.1:5557`（自动检测，不固定） |
| 分辨率 | 1080×1920 @ 420dpi |
| Android | 9 / API 28, x86_64 |
| 语言/时区 | zh-CN / Asia/Shanghai |

### 启动方式（唯一正确方式）

使用项目自带的 PowerShell 脚本：

```powershell
# 在项目根目录执行
powershell -File "tools/android/ldplayer.ps1" -Action start
```

此脚本会：
1. 检查实例是否存在（index=1, name=Daily Flow Test）
2. 未启动时调用 `ldconsole.exe launch --index 1`
3. 等待 Android 启动完成（`sys.boot_completed=1`）
4. 自动检测 ADB endpoint 并连接
5. 输出设备状态（Index/Name/AdbState/Resolution）

**其他操作**：
```powershell
powershell -File "tools/android/ldplayer.ps1" -Action status   # 查看状态
powershell -File "tools/android/ldplayer.ps1" -Action connect  # 仅连接（已启动时）
powershell -File "tools/android/ldplayer.ps1" -Action stop     # 停止
```

### 设备检查

```bash
"C:/Users/10844/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
# 成功输出应包含：emulator-5556  device
```

如果输出只有 `List of devices attached`（空），说明 LDPlayer 未运行。**直接执行上述 start 命令，不手动尝试启动路径。**

---

## 2. ADB 操作速查

### 变量设置

```bash
ADB="C:/Users/10844/AppData/Local/Android/Sdk/platform-tools/adb.exe"
# 多设备时需要 -s：
ADB="$ADB -s emulator-5556"
```

### 安装 & 启动

```bash
# 强制停止旧版本
$ADB shell am force-stop com.dailyflow.app.debug

# 清日志（避免读到旧崩溃）
$ADB logcat -c

# 安装最新 APK
$ADB install -r "$LOCALAPPDATA/DailyFlow/build/app/outputs/apk/debug/app-debug.apk"
# 或从 releases 目录：
$ADB install -r "/c/Users/10844/Documents/BaiduSyncdisk/Codex Projects/Daily_Flow/releases/<filename>.apk"

# 启动应用
$ADB shell monkey -p com.dailyflow.app.debug -c android.intent.category.LAUNCHER 1
sleep 3  # 等待 UI 渲染
```

### 截图

```bash
# 截图到设备
$ADB shell "screencap -p /sdcard/test_<name>.png"

# 拉到本地（注意 MSYS_NO_PATHCONV=1 防止路径转换）
MSYS_NO_PATHCONV=1 $ADB pull /sdcard/test_<name>.png "$LOCALAPPDATA/Temp/test_<name>.png"
```

### 点击 & 滑动

```bash
# 底部导航栏 Y=1815（1080×1920 分辨率）
$ADB shell input tap 200 1815   # 概览
$ADB shell input tap 400 1815   # 日历
$ADB shell input tap 680 1815   # 空间
$ADB shell input tap 950 1815   # 设置

# 顶级返回按钮
$ADB shell input tap 55 106

# 滑动（从中间向上滑 = 向下滚动内容）
$ADB shell input swipe 540 1200 540 200 300

# 输入文本
$ADB shell input text "测试内容"
```

### 崩溃检查

```bash
$ADB logcat -d | grep -iE "crash|fatal|ANR|AndroidRuntime.*FATAL"
# 预期输出为空
```

---

## 3. 构建命令

### Debug APK（最常用）

```bash
cd "/c/Users/10844/Documents/BaiduSyncdisk/Codex Projects/Daily_Flow"

# 使用外部 build root（必须，避免百度同步盘文件锁）
./gradlew.bat -PdailyFlow.buildRoot="$LOCALAPPDATA/DailyFlow/build" :app:assembleDebug
# 加 --rerun-tasks 可跳过缓存强制全量构建
```

输出路径：`$LOCALAPPDATA/DailyFlow/build/app/outputs/apk/debug/app-debug.apk`

### 模块级编译检查

```bash
./gradlew.bat -PdailyFlow.buildRoot="$LOCALAPPDATA/DailyFlow/build" :app:compileDebugKotlin
```

### 完整 Smoke Test

```powershell
powershell -File "tools/android/smoke.ps1"
```

---

## 4. 发布 APK 存档

所有发布 APK 存放在：

```
Daily_Flow/releases/
```

命名规范：`Daily_Flow_v<version>_<variant>_YYYYMMDD.apk`

---

## 5. GitHub 推送

### 前提条件

- 远程仓库：`https://github.com/GovyBen/Daily-Flow`
- 需要 Clash 代理运行（端口 7890）
- 需要有效的 GitHub PAT

### 推送命令

```bash
# 设置代理（仅在推送前）
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890

git push origin main
git push origin --tags

# 推送后清理
git config --global --unset http.proxy
git config --global --unset https.proxy
```

---

## 6. Git 提交规范

格式：`DF-NNN <简短描述>`

提交信息模板：
```
DF-NNN: <摘要>

<详细说明>

Reuse: <从何处复用的代码>
New code: <全新编写的部分>
Verification: <验证方式，如 BUILD SUCCESSFUL>
```

---

## 7. 关键约束

- ⚠️ 工作区在百度同步盘，**必须使用外部 build root**
- ⚠️ 雷电为 API 28，**无法验证** Android 12+ exact alarm 和 Android 13+ notification 权限
- ⚠️ 测试坐标基于 1080×1920，不同分辨率需重算
- ⚠️ 系统返回键（`input keyevent 4`）2-3次会退出应用，优先用底部导航栏
- ⚠️ `BETA_TEST_PLAN.md` 中硬编码坐标不准确，实际应以视觉校准为准
- ⚠️ LDPlayer 启动**必须**使用 `tools/android/ldplayer.ps1`，不可手动猜路径
