# DF-703: Accessibility Audit

**Date:** 2026-06-17  
**Auditor:** Hermes Agent  
**Project:** Daily Flow (Android / Jetpack Compose)

---

## 1. Missing `contentDescription` on Image/Icon Composables

### 1.1 Scope & Methodology
- Searched all Compose files under `app/src/main`, `tracking/presentation`, `calendar/presentation`, and `tasks/presentation` for `contentDescription` usage.
- Used regex search across the entire project (100+ matches) to find all `contentDescription` assignments.

### 1.2 Findings: Decorative Icons with `contentDescription = null`
The following patterns use `contentDescription = null` — these are acceptable for **purely decorative** icons, but should be reviewed since many appear to convey semantic meaning:

| File | Line | Context | Verdict |
|------|------|---------|---------|
| `CalendarScreen.kt` | 507 | `Icon(Icons.Default.ArrowDropDown, contentDescription = null)` | **Acceptable** — dropdown indicator on a button that already has a label |
| `SettingsScreen.kt` | 426,505,566,629 | `Icon(Icons.Default.ArrowDropDown, contentDescription = null)` (x4) | **Acceptable** — decorative dropdown arrows |
| `ContentLibraryScreen.kt` | 107,215,318 | Search icon, cancel icon, type icon | **NEEDS FIX** — search icon should have `contentDescription = stringResource(R.string.search)`; type icon should convey meaning |
| `TrackingTemplateListScreen.kt` | 426,435,445,467 | Edit/History/Chart/PushPin icons in dropdown menu | **Acceptable** — icons inside menu items that have text labels |
| `TaskSmallCard.kt` | 129 | `contentDescription = null` on an icon | **NEEDS REVIEW** |
| `AuthScreen.kt` | 34 | `contentDescription = null` on app lock icon | **NEEDS FIX** |
| `AiProviderSection.kt` | 163,212,252 | Provider icons | **NEEDS FIX** — provider logos should have descriptive contentDescription |
| `AssistantChatBar.kt` | 178,232 | Attachment and voice icons | **NEEDS FIX** — should have labeled contentDescriptions |
| `ImportExportScreen.kt` | 174,208 | Backup/restore icons | **NEEDS FIX** |
| `SectionHeader.kt` | 30 | Section header icon | **NEEDS REVIEW** |
| `BackupFormatCard.kt` | 92,119 | Format icons | **NEEDS FIX** |

### 1.3 Findings: Hardcoded English `contentDescription` Strings
These use literal hardcoded strings instead of `stringResource()`:

| File | Line | Value | Severity |
|------|------|-------|----------|
| `AssistantChatBar.kt` | 245 | `"Send"` | **High** — user-facing action |
| `TasksHomeScreenWidget.kt` | 63 | `"Add task"` | **High** |
| `NotesHomeScreenWidget.kt` | 63 | `"Add note"` | **High** |
| `CalendarHomeScreenWidget.kt` | 73 | `"refresh"` | **High** |
| `CalendarHomeScreenWidget.kt` | 83 | `"add event"` | **High** |
| `LiquidFloatingActionButton.kt` | 89 | `"Add"` | **High** |
| `TaskWidgetItem.kt` | 142 | `""` (empty string) | **Medium** |
| `CalendarEventWidgetItem.kt` | 87 | `""` (empty string) | **Medium** |
| `AiProviderSection.kt` | 163 | `""` (empty string) | **Medium** |

### 1.4 Findings: Hardcoded User-Visible Text
These use hardcoded strings in `Text()` composables instead of `stringResource()`:

| File | Line | Text | Severity |
|------|------|------|----------|
| `MessageCard.kt` | 366 | `"Tap to review and confirm"` | **High** |
| `TaskDetailsViewModel.kt` | 113 | `"Untitled"` (default title) | **High** |
| `TaskDetailScreen.kt` | 252 | `"Untitled"` (displayed title) | **High** |
| `NoteWidgetItem.kt` | 76 | `"Untitled"` (widget) | **Medium** |
| `ConfirmationCard.kt` | 54 | `"Missing: ${proposal.missingFields.joinToString()}"` | **Medium** |

---

## 2. Minimum Touch Target Size (48dp)

### 2.1 Methodology
- Review of key interactive elements across screens.
- The Compose Material3 library enforces minimum touch targets for many components (IconButton minimum is 48dp by default), but custom composables need manual verification.

### 2.2 Findings
- **Material3 defaults are generally respected** — `IconButton`, `FloatingActionButton`, and `NavigationBarItem` all enforce 48dp minimums.
- **Widget layouts** (Glance widgets) are a potential concern — the widget task items and note items may have smaller touch areas. This is inherent to widget constraints but worth noting for accessibility.
- **Calendar day cells** (`CalendarDayCell.kt`) — day-of-month cells should be verified to be at least 48dp × 48dp.
- **Tracking field inputs** (`TrackingFieldInput.kt`) — individual input components should be verified.

---

## 3. String Resource Usage

### 3.1 Positive Findings
- The vast majority of user-visible text uses `stringResource()` properly.
- `MainBottomBar`, `SettingsScreen`, `SpacesScreen`, `DashboardScreen`, `DiaryScreen`, `BookmarksScreen`, `TasksScreen` all use `stringResource` extensively.
- **Grade: B+** — good coverage, with specific exceptions noted in §1.4.

### 3.2 Exceptions
- 5 production-code hardcoded user-facing strings identified (see §1.4).
- Widget `contentDescription` strings hardcoded in English (see §1.3).

---

## 4. Lint Check

- **Command:** `./gradlew.bat lintDebug`
- **Result:** BUILD FAILED due to a Windows file-locking issue (`FileSystemException` on `:core:ui:bundleLibCompileToJarDebug`), not a code quality issue.
- **Attempted workaround:** `:app:compileDebugKotlin` also failed with same file-lock issue.
- **Recommendation:** Retry lint on a clean build or Linux CI environment.

---

## 5. Summary & Recommendations

| Area | Status | Action |
|------|--------|--------|
| Decorative icons with `null` contentDescription | ⚠️ Review needed | ~10 instances need proper labels |
| Hardcoded contentDescription strings | ❌ 9 instances | Migrate to `stringResource()` |
| Hardcoded user-visible Text | ❌ 5 instances | Migrate to `stringResource()` |
| 48dp touch targets | ✅ Largely met | Verify calendar cells and widget items |
| stringResource usage | ✅ Good | Fix 5 exceptions |
| Lint | ⚠️ Could not run | Retry on clean environment |

**Overall Accessibility Grade: B** — Mostly good with specific fixable issues.
