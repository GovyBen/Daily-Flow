# Build Baseline

Date: 2026-06-13

## Result

The Daily Flow P0 source tree builds successfully without an Android device or
DeepSeek API key.

| Check | Result |
|---|---|
| Gradle Wrapper | Pass |
| Debug APK | Pass |
| JVM unit tests | Pass |
| Full debug lint | Pass |
| Emulator/device launch | Deferred by user instruction |

## Environment

- OS: Windows 11 amd64
- JDK: Oracle JDK 17.0.12
- Gradle Wrapper: 8.13
- Android Gradle Plugin: 8.13.2
- Project Kotlin plugin: 2.3.10
- Gradle embedded Kotlin: 2.0.21
- Android SDK Platform: API 36, revision 2
- Android Build-Tools: 36.0.0
- Android Platform-Tools: 37.0.0
- `compileSdk`: 36
- `targetSdk`: 35
- `minSdk`: 26

## Verification

```powershell
.\gradlew.bat --version
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" lintDebug
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" `
  testDebugUnitTest :app:assembleDebug
```

The external build root avoids file-lock races caused by the synchronized
workspace. CI does not need this property and continues to use standard Gradle
build directories.

## Debug Artifact

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 42,587,792 bytes
- SHA-256: `51E42887D7AA712E0C93D31B560842CC9A9E22D7EEC8E26FCDFB0C38FAB1355F`
- Package: `com.dailyflow.app.debug`
- Version: `0.1.0` (`versionCode` 1)
- Label: `Daily Flow Debug`
- Compile SDK: 36
- Target SDK: 35

## Baseline Corrections

The imported My Brain baseline compiled and tested, but current Android lint
reported errors that would have blocked the new CI checks. P0 corrected:

- Flow operators created inside Composition in three widget classes and
  `MainActivity`
- An unremembered `FocusRequester` in a Compose preview
- `android:tint` used instead of `app:tint` in a preview layout

## Remaining Warnings

- Gradle reports deprecated features that will need review before Gradle 9.
- Device installation, launch, provider authority, calendar permission and
  notification behavior remain unverified until device testing is enabled.
- The GitHub Actions workflow is configured but has not run on a remote runner
  because no Daily Flow `origin` remote is configured yet.
