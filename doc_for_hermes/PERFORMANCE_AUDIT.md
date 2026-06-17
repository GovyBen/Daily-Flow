# DF-704: Performance Audit

**Date:** 2026-06-17  
**Auditor:** Hermes Agent  
**Project:** Daily Flow (Android / Jetpack Compose + Room)

---

## 1. Room DAO Calls on Main Thread

### 1.1 `runBlocking` in Production Code

**CRITICAL FINDING:** The `MyBrainApplication.onCreate()` method uses `runBlocking` **twice**, blocking the main thread during app startup.

| File | Line | Context |
|------|------|---------|
| `app/.../MyBrainApplication.kt` | 106 | `runBlocking { legacySecretMigration.migrate(); defaultTrackingTemplateInitializer.initialize(...); migrateLegacyTaskAlarms() }` |
| `app/.../MyBrainApplication.kt` | 143 | `private fun loadNotesModule() = runBlocking { ... }` |

**Impact:** Blocks the UI thread during application startup. The blocked operations include:
1. Legacy secret migration (potentially crypto operations)
2. Default tracking template initialization (Room DB writes)
3. Legacy task alarm migration
4. Loading notes module (DataStore reads + Koin module loading)

**Severity: HIGH** — While these run once at startup, they risk ANR on slow devices or with large datasets.

**Recommendation:** Move to a coroutine launched from `applicationScope` with a startup `Mutex` to ensure one-time execution. Alternatively, use `GlobalScope.launch(Dispatchers.IO)` if safety guarantees are in place.

### 1.2 Direct DAO Calls in @Composable Functions

**Finding:** No direct DAO calls were found in `@Composable` functions. DAOs are accessed through ViewModels and use cases, which is the correct pattern.

**Positive:** The architecture follows Clean Architecture with proper separation — DAO → Repository → UseCase → ViewModel → Composable.

### 1.3 Suspending DAO Functions Called Without Proper Coroutine Scope

#### 1.3.1 TaskDetailsViewModel — `observeReminders()`
```kotlin
// Line 59-66
private fun observeReminders(taskId: String) {
    viewModelScope.launch {
        if (taskId.isBlank()) return@launch
        _reminders.value = reminderRepository.getByTarget(
            ReminderTargetType.TASK, taskId
        )
    }
}
```
**Status: OK** — Uses `viewModelScope.launch`.

#### 1.3.2 ReminderRepository — Flow vs Suspend
`ReminderDao` methods are all `suspend` functions (not Flow). The repository wraps these as one-shot calls, which is appropriate.

### 1.4 Flow Usage and Collection Scoping

**Positive Findings:**
- All `Flow` collections in ViewModels use `.launchIn(viewModelScope)` or `.onEach { }.launchIn(viewModelScope)` — correct pattern.
- `CalendarViewModel` properly cancels and recreates collection Jobs when configuration changes.
- No leaked `Flow` collectors detected.

**Minor Concern:**
```kotlin
// CalendarViewModel.kt line 162
observeTemplates().onEach { templates ->
    _uiState.update { it.copy(trackingTemplates = templates) }
}.launchIn(viewModelScope)
```
This is fine but could benefit from `stateIn(viewModelScope)` to share a single subscription.

---

## 2. Room DAO Index Usage

### 2.1 Index Audit by Table

| Table | Indexed Columns | Status |
|-------|----------------|--------|
| `reminders` | `(target_type, target_id)`, `(enabled, status)` | ✅ Well-indexed |
| `tracking_templates` | `(display_order, created_at_epoch_milli, id)` | ✅ Good |
| `tracking_trackers` | `(type, is_active)` | ✅ Good |
| `tracking_template_fields` | `(template_id)`, `(tracker_id)`, `(template_id, display_order, id)` | ✅ Well-indexed |
| `tracking_tracker_options` | `(tracker_id)`, `(tracker_id, display_order, id)` | ✅ Good |
| `tracking_record_sessions` | `(template_id, occurred_at_epoch_milli)` | ✅ Critical for range queries |
| `tracking_data_points` | `(session_id)`, `(tracker_id, epoch_milli)`, `(option_id)` | ✅ Well-indexed |

### 2.2 Tables MISSING Indexes

| Table | Missing Index | Queries Affected | Severity |
|-------|--------------|------------------|----------|
| `tasks` | **None defined** (`@Entity` has no `indices` param) | `getTaskByAlarm(alarmId)`, `getTasksByTitle(title)` | **Medium** |
| `notes` | **None defined** | `getNotesByTitle(query)` — full table scan on LIKE, `getNotesByFolder(folderId)` | **Medium** |
| `diary` | **None defined** | `getEntriesByTitle(query)` — full table scan on LIKE | **Low** |
| `bookmarks` | **None defined** | `getBookmarksByQuery(query)` — full table scan on LIKE | **Low** |
| `alarms` | **None defined** | Simple queries, low row count | **Low** |

### 2.3 Recommendation
Add `@Entity(indices = [...])` annotations:
```kotlin
// TaskEntity
@Entity(tableName = "tasks", indices = [Index("alarmId"), Index("title")])

// NoteEntity
@Entity(tableName = "notes", indices = [Index("folder_id"), Index("title")])

// DiaryEntryEntity
@Entity(tableName = "diary", indices = [Index("title")])

// BookmarkEntity
@Entity(tableName = "bookmarks", indices = [Index("title")])
```

Note: Adding indexes to text columns used with LIKE queries provides limited benefit on SQLite (requires FTS for full-text search). However, `folder_id` and `alarmId` indexes are clearly beneficial.

---

## 3. Other Performance Concerns

### 3.1 CalendarViewModel — Async Loading Strategy
```kotlin
// CalendarViewModel.kt lines 88-116
fun loadMonth(month: YearMonth, forceRefresh: Boolean = false) {
    // Loads current + prev + next months concurrently via async {}
}
```
**Status: GOOD** — Uses structured concurrency with `async` inside `viewModelScope.launch`. Three months are loaded concurrently, and a 4-month LRU cache is maintained.

### 3.2 TrackingAnalyticsQueryEngine
Complex analytics queries with `INNER JOIN` across `tracking_data_points`, `tracking_record_sessions`, and `tracking_trackers`. The `(tracker_id, epoch_milli)` composite index on `tracking_data_points` covers these well.

### 3.3 CSV Export (`TrackingCsvManager`)
Potentially heavy I/O operations. Writes to external storage in a coroutine — should ensure `Dispatchers.IO` is used.

---

## 4. Summary

| Issue | Severity | Recommendation |
|-------|----------|---------------|
| `runBlocking` in `Application.onCreate()` | **HIGH** | Replace with non-blocking startup |
| Missing indexes on `tasks`, `notes`, `diary`, `bookmarks` | **MEDIUM** | Add `@Entity(indices = [...])` |
| LIKE queries without FTS | **LOW** | Consider FTS4/FTS5 for search-heavy tables |
| No DAO calls on main thread elsewhere | ✅ PASS | Architecture is sound |

**Overall Performance Grade: B-** — Good architecture but the `runBlocking` in Application.onCreate() is a significant issue that should be fixed.
