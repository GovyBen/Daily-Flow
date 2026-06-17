# Daily Flow Third-Party Notices

Last updated: 2026-06-17

This document is the exhaustive dependency and license audit for the Daily Flow
Android application (DF-801). It covers all declared dependencies in
`gradle/libs.versions.toml`, their licenses, F-Droid compatibility, and GPLv3
compatibility.

## License Audit Summary

All 17 direct dependencies are compatible with both GPLv3 (the project license)
and F-Droid inclusion. No closed-source AARs, binary blobs, or telemetry SDKs
are present in the project.

### F-Droid & GPLv3 Compatibility

- **Apache 2.0** is compatible with GPLv3 (one-way: Apache 2.0 code can be
  included in GPLv3 projects).
- **MIT** is compatible with both GPLv3 and F-Droid.
- All dependencies are obtained from Maven Central (standard open-source
  repositories).

---

## Direct Dependencies

### AI Agent Framework

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Koog Agents | `ai.koog:koog-agents` | 0.6.3 | Apache 2.0 | ✅ | ✅ |

- **Source**: <https://github.com/JetBrains/koog>
- **License text**: <https://github.com/JetBrains/koog/blob/develop/LICENSE.txt>
- **Note**: Open-source JetBrains framework for building AI agents. Uses
  Kotlin Coroutines, Kotlinx Serialization, and Kotlinx Datetime internally.

### UI & Compose

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Compose BOM | `androidx.compose:compose-bom` | 2026.02.01 | Apache 2.0 | ✅ | ✅ |
| Material Icons Extended | `androidx.compose.material:material-icons-extended` | (BOM) | Apache 2.0 | ✅ | ✅ |
| Material3 | `androidx.compose.material3:material3` | (BOM) | Apache 2.0 | ✅ | ✅ |
| Compose UI | `androidx.compose.ui:ui` | (BOM) | Apache 2.0 | ✅ | ✅ |
| Navigation Compose | `androidx.navigation:navigation-compose` | 2.9.7 | Apache 2.0 | ✅ | ✅ |
| Activity Compose | `androidx.activity:activity-compose` | 1.12.4 | Apache 2.0 | ✅ | ✅ |
| Lifecycle Runtime Compose | `androidx.lifecycle:lifecycle-runtime-compose` | 2.10.0 | Apache 2.0 | ✅ | ✅ |

- **Source**: AndroidX libraries — part of Android Open Source Project
- **License**: All AndroidX libraries are Apache 2.0 licensed

### UI Effects & Shapes

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Squircle Shape | `io.github.stoyan-vuchev:squircle-shape` | 4.0.0 | **MIT** | ✅ | ✅ |
| Liquid | `io.github.fletchmckee.liquid:liquid` | 1.1.1 | Apache 2.0 | ✅ | ✅ |

- **Squircle Shape**: <https://github.com/stoyan-vuchev/squircle-shape>
  (MIT License)
- **Liquid**: <https://github.com/FletchMcKee/liquid> (Apache 2.0)

### Markdown Rendering

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Markdown Renderer M3 | `com.mikepenz:multiplatform-markdown-renderer-m3` | 0.39.2 | Apache 2.0 | ✅ | ✅ |
| Markdown Renderer Coil2 | `com.mikepenz:multiplatform-markdown-renderer-coil2` | 0.39.2 | Apache 2.0 | ✅ | ✅ |

- **Source**: <https://github.com/mikepenz/multiplatform-markdown-renderer>
- **License**: Apache 2.0

### Charts & Data

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| AndroidPlot Core | `com.androidplot:androidplot-core` | 1.5.11 | Apache 2.0 | ✅ | ✅ |
| Apache Commons CSV | `org.apache.commons:commons-csv` | 1.14.1 | Apache 2.0 | ✅ | ✅ |

- **AndroidPlot**: <https://github.com/halfhp/androidplot> (Apache 2.0)
- **Commons CSV**: <https://commons.apache.org/proper/commons-csv/> (Apache 2.0)

### Networking

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Ktor Client Core | `io.ktor:ktor-client-core` | 3.4.0 | Apache 2.0 | ✅ | ✅ |
| Ktor Client OkHttp | `io.ktor:ktor-client-okhttp` | 3.4.0 | Apache 2.0 | ✅ | ✅ |
| Ktor Client CIO | `io.ktor:ktor-client-cio` | 3.4.0 | Apache 2.0 | ✅ | ✅ |
| Ktor Logging | `io.ktor:ktor-client-logging` | 3.4.0 | Apache 2.0 | ✅ | ✅ |
| Ktor Serialization | `io.ktor:ktor-serialization-kotlinx-json` | 3.4.0 | Apache 2.0 | ✅ | ✅ |
| Ktor Content Negotiation | `io.ktor:ktor-client-content-negotiation` | 3.4.0 | Apache 2.0 | ✅ | ✅ |

- **Source**: <https://github.com/ktorio/ktor> (Apache 2.0)

### Dependency Injection

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Koin BOM | `io.insert-koin:koin-bom` | 4.1.1 | Apache 2.0 | ✅ | ✅ |
| Koin Android | `io.insert-koin:koin-android` | (BOM) | Apache 2.0 | ✅ | ✅ |
| Koin Core | `io.insert-koin:koin-core` | (BOM) | Apache 2.0 | ✅ | ✅ |
| Koin Compose | `io.insert-koin:koin-androidx-compose` | (BOM) | Apache 2.0 | ✅ | ✅ |
| Koin WorkManager | `io.insert-koin:koin-androidx-workmanager` | (BOM) | Apache 2.0 | ✅ | ✅ |
| Koin Annotations | `io.insert-koin:koin-annotations` | 2.3.1 | Apache 2.0 | ✅ | ✅ |
| Koin KSP Compiler | `io.insert-koin:koin-ksp-compiler` | 2.3.1 | Apache 2.0 | ✅ | ✅ |

- **Source**: <https://github.com/InsertKoinIO/koin> (Apache 2.0)

### Persistence

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Room Runtime | `androidx.room:room-runtime` | 2.8.4 | Apache 2.0 | ✅ | ✅ |
| Room KTX | `androidx.room:room-ktx` | 2.8.4 | Apache 2.0 | ✅ | ✅ |
| Room Compiler (KSP) | `androidx.room:room-compiler` | 2.8.4 | Apache 2.0 | ✅ | ✅ |
| DataStore Preferences | `androidx.datastore:datastore-preferences` | 1.2.0 | Apache 2.0 | ✅ | ✅ |

- **Source**: AndroidX — part of Android Open Source Project (Apache 2.0)

### Kotlin Standard & Extensions

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| Kotlin Stdlib | `org.jetbrains.kotlin:kotlin-stdlib` | 2.3.10 | Apache 2.0 | ✅ | ✅ |
| Coroutines Core | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.10.2 | Apache 2.0 | ✅ | ✅ |
| Coroutines Android | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | Apache 2.0 | ✅ | ✅ |
| Serialization JSON | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.10.0 | Apache 2.0 | ✅ | ✅ |
| Datetime | `org.jetbrains.kotlinx:kotlinx-datetime` | 0.7.1-0.6.x-compat | Apache 2.0 | ✅ | ✅ |

- **Source**: JetBrains — part of Kotlin project (Apache 2.0)

### Utilities

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| UUID | `com.benasher44:uuid` | 0.8.4 | **MIT** | ✅ | ✅ |
| Calf File Picker | `com.mohamedrejeb.calf:calf-file-picker` | 0.9.0 | Apache 2.0 | ✅ | ✅ |

- **UUID**: <https://github.com/benasher44/uuid> — Kotlin multiplatform UUID
  implementation (MIT)
- **Calf File Picker**: <https://github.com/mohamedrejeb/Calf> — Compose
  Multiplatform file picker (Apache 2.0)

### Android Core

| Library | Group | Version | License | F-Droid | GPLv3 |
|---|---|---|---|---|---|
| AppCompat | `androidx.appcompat:appcompat` | 1.7.1 | Apache 2.0 | ✅ | ✅ |
| Core KTX | `androidx.core:core-ktx` | 1.17.0 | Apache 2.0 | ✅ | ✅ |
| Biometric | `androidx.biometric:biometric` | 1.4.0-alpha05 | Apache 2.0 | ✅ | ✅ |
| WorkManager | `androidx.work:work-runtime-ktx` | 2.11.1 | Apache 2.0 | ✅ | ✅ |
| DocumentFile | `androidx.documentfile:documentfile` | 1.1.0 | Apache 2.0 | ✅ | ✅ |
| Glance AppWidget | `androidx.glance:glance-appwidget` | 1.1.1 | Apache 2.0 | ✅ | ✅ |
| Material (Google) | `com.google.android.material:material` | 1.13.0 | Apache 2.0 | ✅ | ✅ |

- **Source**: AndroidX and Google Material — part of Android Open Source
  Project (Apache 2.0)

---

## Test-Only Dependencies

| Library | Group | Version | License | Notes |
|---|---|---|---|---|
| JUnit | `junit:junit` | 4.13.2 | EPL 1.0 | Test only; not shipped in APK |
| AndroidX Test JUnit | `androidx.test.ext:junit` | 1.3.0 | Apache 2.0 | Test only |
| Espresso Core | `androidx.test.espresso:espresso-core` | 3.7.0 | Apache 2.0 | Test only |
| Compose UI Test | `androidx.compose.ui:ui-test-junit4` | (BOM) | Apache 2.0 | Test only |
| Room Testing | `androidx.room:room-testing` | 2.8.4 | Apache 2.0 | Test only |
| MockWebServer | `com.squareup.okhttp3:mockwebserver3` | 5.4.0 | Apache 2.0 | Test only |

---

## Source-Derived Code

### Track & Graph

- **Repository**: <https://github.com/SamAmco/track-and-graph>
- **Fixed commit**: `4bb925a731e0537f6330971853770e9aafb51983`
- **License**: GPL-3.0-or-later
- **Use**: Selective adaptation of tracking sampling, aggregation, chart and
  CSV behavior. Track & Graph is not included as a runtime dependency.
- **CSV source**:
  `app/data/src/main/java/com/samco/trackandgraph/data/csvreadwriter/CSVReadWriterImpl.kt`
- **Daily Flow CSV destination**:
  `tracking/src/main/java/com/mhss/app/tracking/data/csv/TrackingCsvCodec.kt`
- **Modification summary**: Replaced the feature-oriented CSV with a versioned
  six-table snapshot, Kotlin datetime, UUID string IDs, full-file preview
  validation, row-numbered errors, Room transaction import and Android SAF
  file handling.

The complete file-by-file migration ledger is maintained in
`docs/UPSTREAM_PROVENANCE.md`. Adapted source files retain the applicable GPL
notice and identify Daily Flow modifications.

---

## Binary Blob & Telemetry Audit

- **Closed-source AARs**: None found in the project.
- **Native libraries (.so)**: None shipped in the project.
- **Telemetry SDKs**: No Firebase, Google Analytics, Crashlytics, or any
  third-party telemetry/analytics SDKs are included.
- **Network Security**: Release builds enforce cleartext HTTP prohibition via
  `network_security_config.xml`. Only HTTPS connections to user-configured
  AI API endpoints are permitted.

---

## F-Droid Inclusion Checklist

| Criterion | Status |
|---|---|
| All dependencies are open-source | ✅ |
| No proprietary/closed-source libraries | ✅ |
| No tracking or analytics SDKs | ✅ |
| No binary blobs or prebuilt AARs | ✅ |
| Licenses compatible with F-Droid | ✅ (all Apache 2.0 or MIT) |
| Anti-features: None | ✅ |
| Builds with standard Gradle from Maven Central | ✅ |

---

## License Texts

### Apache License 2.0

The majority of dependencies listed above are licensed under the Apache
License, Version 2.0. The full license text is available at:
<https://www.apache.org/licenses/LICENSE-2.0>

### MIT License

The following dependencies use the MIT License:
- `io.github.stoyan-vuchev:squircle-shape` (4.0.0)
- `com.benasher44:uuid` (0.8.4)

MIT License text:
```
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

### GPLv3 (Project License)

Daily Flow is licensed under GPL-3.0-or-later. The full license text is
available at: <https://www.gnu.org/licenses/gpl-3.0.html>

---

## Official Project Pages

- **AndroidPlot**: <https://github.com/halfhp/androidplot>
- **Apache Commons CSV**: <https://commons.apache.org/proper/commons-csv/>
- **Ktor**: <https://ktor.io/>
- **Koin**: <https://insert-koin.io/>
- **Koog**: <https://github.com/JetBrains/koog>
- **Liquid**: <https://github.com/FletchMcKee/liquid>
- **Squircle Shape**: <https://github.com/stoyan-vuchev/squircle-shape>
- **Markdown Renderer**: <https://github.com/mikepenz/multiplatform-markdown-renderer>
- **UUID**: <https://github.com/benasher44/uuid>
- **Calf**: <https://github.com/mohamedrejeb/Calf>

---

*Audit completed: 2026-06-17 for Daily Flow P8 release preparation (DF-801).*
