# Release Signing Configuration

Last updated: 2026-06-17 (DF-802)

This document describes how to sign the Daily Flow release APK. The signing
configuration reads credentials from environment variables (CI) or
`local.properties` (local development). The keystore path is **never**
hardcoded in `build.gradle.kts`.

## Quick Start

### 1. Generate a keystore (one-time)

```bash
keytool -genkey -v \
  -keystore ~/.android/dailyflow-release.jks \
  -alias dailyflow \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Daily Flow, OU=Development, O=Daily Flow, L=Unknown, S=Unknown, C=US"
```

### 2. Configure signing credentials

**Option A: Environment variables (CI)**

```bash
export DAILYFLOW_KEYSTORE_PATH="$HOME/.android/dailyflow-release.jks"
export DAILYFLOW_KEYSTORE_PASSWORD="YOUR_STORE_PASSWORD"
export DAILYFLOW_KEY_ALIAS="dailyflow"
export DAILYFLOW_KEY_PASSWORD="YOUR_KEY_PASSWORD"
```

**Option B: local.properties (local dev)**

Add to `local.properties` (this file is `.gitignore`'d, never committed):

```properties
dailyFlow.keystorePath=/home/user/.android/dailyflow-release.jks
dailyFlow.keystorePassword=YOUR_STORE_PASSWORD
dailyFlow.keyAlias=dailyflow
dailyFlow.keyPassword=YOUR_KEY_PASSWORD
```

### 3. Build signed release

```bash
./gradlew.bat :app:assembleRelease
```

The Gradle build reads credentials in this order:
1. Environment variables (preferred for CI)
2. `local.properties` (for local development)
3. If neither is present, the build produces an **unsigned** release APK

### 4. Verify signing

```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

---

## Signing Configuration in build.gradle.kts

The signing config uses Gradle property providers and falls back gracefully:

```kotlin
val keystorePath: String? by project
val keystorePassword: String? by project
val keyAlias: String? by project
val keyPassword: String? by project

signingConfigs {
    create("release") {
        storeFile = keystorePath?.let { file(it) }
        storePassword = keystorePassword
        keyAlias = keyAlias
        keyPassword = keyPassword
        // If storeFile is null (no credentials), signing is skipped
    }
}
```

When credentials are provided, the release build type references the signing
config. When credentials are absent, the special value
`signingConfigs["release"]` is used (which returns `null`/no signing),
producing an unsigned APK that must be signed manually before distribution.

---

## CI Pipeline Integration

For GitHub Actions or similar CI:

```yaml
- name: Build Release APK
  run: ./gradlew.bat :app:assembleRelease
  env:
    DAILYFLOW_KEYSTORE_PATH: ${{ secrets.DAILYFLOW_KEYSTORE_PATH }}
    DAILYFLOW_KEYSTORE_PASSWORD: ${{ secrets.DAILYFLOW_KEYSTORE_PASSWORD }}
    DAILYFLOW_KEY_ALIAS: ${{ secrets.DAILYFLOW_KEY_ALIAS }}
    DAILYFLOW_KEY_PASSWORD: ${{ secrets.DAILYFLOW_KEY_PASSWORD }}
```

---

## F-Droid Signing Notes

F-Droid will re-sign the APK with its own key. For F-Droid submission:
1. Submit the source repository URL
2. The unsigned release APK can be built from source
3. F-Droid builds from source and signs automatically

---

## Security Best Practices

- **Never commit keystore files** to version control
- **Never commit `local.properties`** with real passwords
- Use CI secrets for automated builds
- Rotate keystore passwords periodically
- Keep the keystore backed up securely (losing it means losing the ability
  to publish updates)
