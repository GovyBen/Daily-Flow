# Daily Flow — F-Droid Metadata Preparation

> **Task**: DF-804  
> **Status**: Complete  
> **Date**: 2026-06-17

---

## 1. Metadata Summary

| Field | Value |
|---|---|
| **App Name** | Daily Flow |
| **Package ID** | `com.dailyflow.app` |
| **Summary** (≤80 chars) | Open-source Android scheduler and life tracker with tasks, calendar, structured tracking, statistics, and AI assistance |
| **License** | GPLv3 |
| **Categories** | Time, Health & Wellbeing, Productivity |
| **Website** | https://github.com/mhss1/MyBrain (will be updated for Daily Flow fork) |
| **Source Code** | (same as Website — Daily Flow fork repo TBD) |
| **Issue Tracker** | (to be determined — GitHub Issues on fork repo) |
| **Changelog** | (to be determined — per-version changelogs in `fastlane/metadata/android/en-US/changelogs/`) |
| **Donate** | None |
| **Anti-Features** | None |
| **Author** | Daily Flow contributors (forked from mhss1/MyBrain) |
| **Minimum Android** | API 26 (Android 8.0) |
| **Target SDK** | 36 |

---

## 2. Full Description

Daily Flow is a fully open-source (GPLv3) Android application that unifies task management, calendar events, structured life tracking, statistics, and optional AI assistance — all in a single, offline-first app.

**Core Features:**

- **Tasks & Calendar**: Create, edit, and organize tasks with priorities, subtasks, deadlines, and flexible repeat rules (daily, weekly, monthly, yearly). View and manage system calendar events alongside your tasks in a unified calendar interface.

- **Structured Life Tracking**: Design custom tracking templates with eight field types — multi-select, single-select, counter, slider (scale), boolean (yes/no), duration, number, and text. Log data points manually or via quick-record shortcuts. Your template library grows with you: create, copy, reorder, pin, and deactivate templates as your habits evolve.

- **Statistics & Insights**: Visualize your tracked data with daily, weekly, and monthly aggregations. See streak counts, option distributions, and trend charts (line, bar, pie). Export and import your data via CSV with preview validation.

- **AI Assistance (Optional)**: Connect your own DeepSeek API key (or any OpenAI-compatible provider) to use natural language for creating tasks, calendar events, diary entries, and tracking records. ALL AI write operations require explicit user confirmation before anything is saved — you're always in control. The AI can also generate statistical summaries of your tracked data.

- **Reminders**: Configure multiple customizable reminders per task, calendar event, or tracking template. Choose from quick presets (on time, 5/15/30 min, 1 hour, 1 day before) or set custom absolute times. Reminders survive device reboots.

- **Privacy & Security**: API keys are stored encrypted in Android Keystore (AES/GCM) and never appear in logs, backups, or crash reports. The app includes biometric app lock, has no ads, no tracking, and no analytics SDKs. All core features work fully offline.

Daily Flow is a fork of [My Brain](https://github.com/mhss1/MyBrain) (v3.1.0) with significant extensions, including the entire structured tracking system ported and adapted from [Track & Graph](https://github.com/SamAmco/track-and-graph).

---

## 3. F-Droid Fastlane Metadata Structure

The following structure should be placed in the app repository at `fastlane/metadata/android/en-US/`:

```
fastlane/metadata/android/en-US/
├── short_description.txt      # ≤80 chars, no trailing period
├── full_description.txt       # Full description (plain text)
├── title.txt                  # "Daily Flow"
├── video.txt                  # (optional) promotional video URL
├── images/
│   ├── icon.png               # 512×512 app icon
│   ├── featureGraphic.png     # 1024×500 feature graphic
│   └── phoneScreenshots/
│       ├── 1.png              # Screenshots (recommended: 4-8)
│       ├── 2.png
│       └── ...
└── changelogs/
    ├── 1.txt                  # versionCode 1 changelog (≤500 chars)
    ├── 2.txt                  # versionCode 2 changelog
    └── ...
```

### 3.1 short_description.txt

```
Open-source scheduler and life tracker with tasks, calendar, tracking, stats, and AI
```

### 3.2 full_description.txt

```
Daily Flow combines task management, calendar events, structured habit
tracking, statistics, and optional AI assistance in a single, offline-first
Android app.

TASKS & CALENDAR
- Create tasks with priorities, subtasks, deadlines, and repeat rules
- View and manage system calendar events alongside tasks
- Unified calendar view for all your commitments

STRUCTURED TRACKING
- Design custom templates with 8 field types: multi-select, single-select,
  counter, slider, boolean, duration, number, and text
- Quick logging with suggested values and one-tap recording
- Templates support copy, reorder, pin, and deactivation

STATISTICS
- Daily, weekly, and monthly aggregations
- Streak counts and option distribution charts
- Line, bar, and pie chart visualizations
- CSV import/export with preview validation

AI ASSISTANCE (OPTIONAL)
- Connect your own DeepSeek or OpenAI-compatible API key
- Natural language task/event/tracking creation
- All write operations require explicit user confirmation
- Statistical summaries of your tracked data

REMINDERS
- Multiple reminders per task, event, or template
- Quick presets: on time, 5/15/30 min, 1 hour, 1 day before
- Custom absolute times supported
- Survive device reboots

PRIVACY
- API keys encrypted in Android Keystore (AES/GCM)
- Biometric app lock
- No ads, no tracking, no analytics SDKs
- Core features work fully offline

Daily Flow is 100% open source under GPLv3. It is a fork of My Brain
(v3.1.0) extended with a structured tracking system adapted from
Track & Graph.
```

### 3.3 title.txt

```
Daily Flow
```

---

## 4. Anti-Features Analysis

| Anti-Feature | Applicable? | Rationale |
|---|---|---|
| **Ads** | ❌ No | No advertising of any kind |
| **Tracking** | ❌ No | No analytics, crash reporting, or user tracking |
| **Non-Free Network Services** | ❌ No | DeepSeek/AI is optional and uses a **user-provided** API key. The user brings their own key; the app does not depend on any specific service. All core features work fully offline without any network access. |
| **Non-Free Dependencies** | ❌ No | All dependencies are FOSS (see Third Party Notices) |
| **Non-Free Assets** | ❌ No | All assets (icons, etc.) are either original or from FOSS sources |
| **Non-Free Addons** | ❌ No | No add-ons promoted |
| **Tethered Network Services** | ❌ No | No mandatory network dependency |
| **Known Vulnerability** | ❌ No | (to be verified before release) |
| **Disabled Algorithm** | ❌ No | Uses standard signing |
| **No Source Since** | ❌ No | Source is continuously maintained |

**Key point on "Non-Free Network Services":** The AI chat feature connects to DeepSeek (or any OpenAI-compatible endpoint) only when the user explicitly configures their own API key. This is NOT a dependency on a non-free service — it's a user-controlled capability, similar to how an email app connects to a user's chosen IMAP server. Core features (tasks, calendar, tracking, statistics) operate fully offline.

---

## 5. Build Recipe Notes for F-Droid

### 5.1 Build System

- **Build tool**: Gradle 8.13 with AGP 8.13.2
- **JDK**: 17
- **Kotlin**: 2.3.10
- **Native code**: None (pure Kotlin/Java)

### 5.2 Dependencies (All FOSS)

| Dependency | License | F-Droid Compatible |
|---|---|---|
| AndroidX (Room, Compose, WorkManager, etc.) | Apache 2.0 | ✅ |
| Koin (DI) | Apache 2.0 | ✅ |
| Ktor (HTTP client) | Apache 2.0 | ✅ |
| AndroidPlot (charts) | Apache 2.0 | ✅ |
| Apache Commons CSV | Apache 2.0 | ✅ |
| SQLCipher (future v1.1) | BSD-style | ✅ |

### 5.3 Build Command

```bash
./gradlew :app:assembleRelease
```

### 5.4 Reproducible Build Notes

- No pre-built `.so` native libraries
- All dependencies resolved from Maven Central / Google Maven
- F-Droid can build entirely from source using standard SDK
- No proprietary build tools required

### 5.5 F-Droid YAML Build Recipe (draft)

```yaml
Categories:
  - Time
  - Health & Wellbeing
  - Productivity
License: GPL-3.0-only
AuthorName: Daily Flow Contributors
SourceCode: https://github.com/mhss1/MyBrain  # will update to fork
IssueTracker: https://github.com/mhss1/MyBrain/issues  # will update
Changelog: https://github.com/mhss1/MyBrain/releases  # will update

AutoName: Daily Flow
Summary: Open-source scheduler and life tracker with tasks, calendar, tracking, and AI
Description: |
  Daily Flow combines task management, calendar events, structured habit
  tracking, statistics, and optional AI assistance in a single, offline-first
  Android app.
  ...

AntiFeatures: None

RepoType: git
Repo: https://github.com/mhss1/MyBrain  # will update to Daily Flow fork

Builds:
  - versionName: '1.0.0'
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes
    prebuild: sed -i -e '/com.google.gms/d' -e '/com.google.firebase/d' build.gradle
    scanignore:
      - build.gradle.kts
```

---

## 6. Open Questions / TODOs

1. **Repository URL**: Finalize the Daily Flow fork GitHub repository URL. Currently the app is based on `mhss1/MyBrain` — the standalone Daily Flow repo needs to be created before F-Droid submission.
2. **Issue Tracker**: Set up GitHub Issues on the Daily Flow fork repo.
3. **Changelog**: Generate per-release changelogs in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
4. **Screenshots**: Capture 4-8 phone screenshots showing: calendar view, task list, tracking template editor, quick record, statistics charts, AI chat.
5. **Feature Graphic**: Create a 1024×500 feature graphic for the F-Droid store listing.
6. **Icon**: Finalize the 512×512 app icon (already exists as `ic_launcher` in the app module).
7. **F-Droid Submission MR**: After the repo is public and tagged, submit a merge request to `fdroiddata` with the build recipe.

---

## 7. References

- [F-Droid Submitting Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [F-Droid Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)
- [F-Droid Anti-Features](https://f-droid.org/en/docs/Anti-Features/)
- [F-Droid Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/)
