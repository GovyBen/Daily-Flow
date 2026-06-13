# Daily Flow App Test Environment

Date: 2026-06-13

## Status

The local LDPlayer test environment is configured and the automated debug APK
smoke test passes.

| Task | Result |
|---|---|
| LDP-001 inventory | Complete |
| LDP-002 official Android SDK | Complete |
| LDP-003 dedicated LDPlayer instance | Complete |
| LDP-004 unified official ADB | Complete |
| LDP-005 build, install and launch | Complete |
| LDP-006 permission/system capability preparation | Complete for Android 9; newer API checks remain |
| LDP-007 PowerShell helpers | Complete |
| LDP-008 reusable snapshots | Deferred; LDPlayer 9.1.80 CLI exposes backup/restore but no snapshot command |

No existing emulator instance was deleted, rebuilt, cleared or reconfigured.
The original instance named `雷电模拟器` remains index 0.

## Host Toolchain

- JDK: `C:\Program Files\Java\jdk-17`
- Java version: Oracle JDK 17.0.12
- Android SDK: `C:\Users\10844\AppData\Local\Android\Sdk`
- SDK command-line tools: 19.0
- Platform Tools: 37.0.0
- Android Platform: API 36 revision 2
- Build Tools: 36.0.0
- Gradle Wrapper: 8.13

The following user environment variables are configured:

```text
JAVA_HOME=C:\Program Files\Java\jdk-17
ANDROID_HOME=C:\Users\10844\AppData\Local\Android\Sdk
ANDROID_SDK_ROOT=C:\Users\10844\AppData\Local\Android\Sdk
```

The user `PATH` starts with the JDK 17 `bin`, official `platform-tools`, and
official `cmdline-tools\latest\bin` directories. Restart already-open terminals
and IDEs before relying on the updated user environment.

## LDPlayer Device

- LDPlayer: 9.1.80.0
- Home: `C:\leidian\LDPlayer9`
- Instance: `Daily Flow Test`
- Index: 1
- Android: 9 / API 28
- ABI: x86_64
- Model: MI 9
- CPU: 4 cores
- Memory: 4096 MB
- Resolution: 1080 x 1920
- Density: 420 dpi
- Available data storage at setup: about 22 GB
- Root mode: off
- ADB debugging: on
- Locale: zh-CN
- Time zone: Asia/Shanghai

ADB identity:

```text
Official ADB: C:\Users\10844\AppData\Local\Android\Sdk\platform-tools\adb.exe
Serial: emulator-5556
Observed endpoint: 127.0.0.1:5557
State: device
```

The official Platform Tools 37.0.0 ADB server is used. LDPlayer's bundled ADB
34.0.4 is not used by the project scripts.

## Project Helpers

Check and configure the current PowerShell process:

```powershell
.\tools\android\env.ps1
```

Inspect, start, connect or stop the dedicated instance:

```powershell
.\tools\android\ldplayer.ps1 status
.\tools\android\ldplayer.ps1 start
.\tools\android\ldplayer.ps1 connect
.\tools\android\ldplayer.ps1 stop
```

Run the complete debug smoke test:

```powershell
.\tools\android\smoke.ps1
```

The smoke test uses the synchronized-folder-safe external build root:

```text
C:\Users\10844\AppData\Local\DailyFlow\build
```

Logs and dumps are written outside the repository:

```text
C:\Users\10844\AppData\Local\DailyFlow\test-artifacts\<timestamp>
```

The test builds the APK, starts the selected instance, waits for
`sys.boot_completed=1`, performs an `install -r`, clears logcat, starts the
launcher activity, observes the process, and captures package, activity, alarm,
notification and logcat state. It does not call `pm clear`.

## Verified Artifact

- APK:
  `C:\Users\10844\AppData\Local\DailyFlow\build\app\outputs\apk\debug\app-debug.apk`
- Package: `com.dailyflow.app.debug`
- Version: 0.1.0 (`versionCode` 1)
- Label: `Daily Flow Debug`
- Launcher:
  `com.mhss.app.mybrain.presentation.main.MainActivity`
- Size: 42,587,792 bytes
- SHA-256:
  `4C80CFC16CAE11D2A28CBB8AD41F0F66A71D742FFDD8E331CAF4AD3E9ECCF29D`

Verification result:

- Gradle `:app:assembleDebug`: pass
- APK streamed install: pass
- Launcher start: pass
- Initial launch time: about 2.5 seconds
- Launch after emulator restart: pass
- App process alive after observation: pass
- Fatal exception markers: none
- ANR markers: none
- App `SecurityException` markers: none
- Possible API key/Authorization markers in captured logcat: none
- Notification channel `reminders_notification_channel`: present
- System Calendar Provider: present

The successful run artifacts are stored at:

```text
C:\Users\10844\AppData\Local\DailyFlow\test-artifacts\20260613-112639
```

## Permission Test Notes

The dedicated instance is Android 9. Calendar read/write permissions are
currently ungranted and must be exercised through the app's real permission UI
for allow, deny and settings-revocation tests. The setup intentionally does not
use `pm grant` to bypass that flow.

Android 9 cannot validate:

- Android 13+ notification runtime permission
- Android 12+ exact alarm special access
- modern background execution and Doze behavior

Those checks require an Android 12/13+ official AVD or a physical device.
Reminder delivery, reboot recovery, OEM background limits, biometric security,
hardware-backed Keystore and release compatibility still require physical
device verification before release.

No DeepSeek API key was used or stored in scripts, Gradle files, logs, emulator
configuration or test artifacts.
