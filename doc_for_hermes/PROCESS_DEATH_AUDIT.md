# DF-705: Process Death Audit

**Date:** 2026-06-17  
**Auditor:** Hermes Agent  
**Project:** Daily Flow (Android / Jetpack Compose + Koin)

---

## 1. SavedStateHandle Usage

### 1.1 Critical Finding: ZERO ViewModels Use SavedStateHandle

A codebase-wide search for `SavedStateHandle` returned **0 results**. None of the 24 ViewModels in the project use `SavedStateHandle`.

| ViewModel | Module | SavedStateHandle | Constructor Args |
|-----------|--------|:---:|-------------------|
| `MainViewModel` | app | ❌ | 6 use cases + repository |
| `CalendarViewModel` | calendar/presentation | ❌ | 7 use cases |
| `CalendarEventDetailsViewModel` | calendar/presentation | ❌ | use cases |
| `TaskDetailsViewModel` | tasks/presentation | ❌ | use cases + `taskId: String` |
| `TasksViewModel` | tasks/presentation | ❌ | use cases |
| `DiaryViewModel` | diary/presentation | ❌ | use cases |
| `DiaryDetailsViewModel` | diary/presentation | ❌ | use cases |
| `NotesViewModel` | notes/presentation | ❌ | use cases |
| `NoteDetailsViewModel` | notes/presentation | ❌ | use cases |
| `NoteFolderDetailsViewModel` | notes/presentation | ❌ | use cases |
| `BookmarksViewModel` | bookmarks/presentation | ❌ | use cases |
| `BookmarkDetailsViewModel` | bookmarks/presentation | ❌ | use cases |
| `AssistantViewModel` | ai/presentation | ❌ | use cases |
| `SettingsViewModel` | settings/presentation | ❌ | use cases |
| `BackupViewModel` | settings/presentation | ❌ | use cases |
| `IntegrationsViewModel` | settings/presentation | ❌ | use cases |
| `ContentLibraryViewModel` | app/content | ❌ | use cases |
| `TrackingQuickRecordViewModel` | tracking | ❌ | use cases |
| `TrackingTemplateListViewModel` | tracking | ❌ | use cases |
| `TrackingTemplateEditorViewModel` | tracking | ❌ | use cases |
| `TrackingHistoryViewModel` | tracking | ❌ | use cases |
| `TrackingDashboardViewModel` | tracking | ❌ | use cases |
| `TrackingCsvViewModel` | tracking | ❌ | use cases |
| `TrackingAnalyticsViewModel` | tracking | ❌ | use cases |

### 1.2 Constructor-Argument Pattern

`TaskDetailsViewModel` receives `taskId: String` via constructor injection (Koin):
```kotlin
class TaskDetailsViewModel(
    // ... use cases
    taskId: String
) : ViewModel()
```

Without `SavedStateHandle`, when the system kills the process and recreates it, Koin will re-inject the `taskId` from the saved navigation arguments. This works **only if** the navigation framework (Compose Navigation or similar) properly restores the navigation backstack. If the backstack is lost, the `taskId` will be blank/default.

### 1.3 What Survives Process Death Today
| Component | Survives? | Mechanism |
|-----------|:---:|-----------|
| Room database data | ✅ Yes | Persistent SQLite storage |
| DataStore preferences | ✅ Yes | Persistent key-value storage |
| Navigation backstack | ⚠️ Partially | Compose Navigation saves/restores backstack entries (API 34+) |
| UI scroll position | ❌ No | No `SavedStateHandle` to save scroll state |
| Calendar selected date/month | ❌ No | Reset to current date on recreation |
| Task detail — editing state | ❌ No | Unsaved edits lost |
| Form input state | ❌ No | Any partially filled forms reset |
| Tracking quick record partial input | ❌ No | Unsaved tracking data lost |

---

## 2. Unsubmitted Drafts (Quick Record)

### 2.1 TrackingQuickRecordViewModel Analysis

The `TrackingQuickRecordViewModel` manages the quick-record flow for tracking data:
```kotlin
class TrackingQuickRecordViewModel(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val saveRecordSession: SaveRecordSessionUseCase,
    private val getSuggestions: GetTrackingValueSuggestionsUseCase
) : ViewModel()
```

**State:** All input state lives in `MutableStateFlow<TrackingQuickRecordUiState>`:
- `fields: Map<String, TrackingQuickRecordFieldState>` — tracker input values
- `note: String` — session note
- `occurredAtEpochMilli: Long` — timestamp

**Process Death Behavior:**
- ❌ All in-memory state (`MutableStateFlow`, `AtomicBoolean saveInFlight`) is **lost** on process death.
- ❌ Unsaved quick-record drafts are **completely lost**.
- ✅ Only persisted data (sessions saved via `save()`) survive in Room.

### 2.2 Other Draft-Sensitive Screens
| Screen | Draft Risk | Impact |
|--------|:---:|--------|
| Task create/edit | HIGH | Partially typed task titles, descriptions, due dates lost |
| Diary entry compose | HIGH | Long-form diary entries lost |
| Note create/edit | HIGH | Note content lost |
| Bookmark create/edit | MEDIUM | URL/title lost |
| Calendar event create/edit | MEDIUM | Event details lost |
| Tracking template editor | HIGH | Complex template configurations lost |
| AI assistant draft message | LOW | Chat history reloaded from DB (persisted) |

---

## 3. Current State & Comparison

### 3.1 What the Architecture Does Well
- **Room database** — all persisted data survives process death.
- **DataStore** — preferences survive.
- **Clean Architecture** — domain layer is stateless; state is in ViewModels.
- **Koin** — dependency injection is re-initialized on process recreation.

### 3.2 What's Missing
- **No `SavedStateHandle` anywhere** — 0/24 ViewModels.
- **No draft persistence** — no mechanism to save partial user input.
- **No scroll/UI state restoration** — users return to default state.
- **No "restore on recreate" flow** — the app silently resets.

---

## 4. Recommendations

### 4.1 Priority: Add SavedStateHandle to Key ViewModels
```kotlin
// Example: TaskDetailsViewModel
class TaskDetailsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getTask: GetTaskByIdUseCase,
    // ...
) : ViewModel() {
    // Retrieve navigation argument
    val taskId: String = savedStateHandle["taskId"] ?: ""
}
```

### 4.2 Priority: Draft Persistence for Quick Record
Add a `SavedStateHandle`-backed draft mechanism:
```kotlin
// In TrackingQuickRecordViewModel
init {
    savedStateHandle.get<String>("draft_template_id")?.let { templateId ->
        load(templateId)
        // Restore field values from savedStateHandle
    }
}

fun save() {
    // On successful save, clear draft
    savedStateHandle.remove<String>("draft_template_id")
}
```

### 4.3 Medium: Form Drafts
Consider a `DraftRepository` backed by DataStore or a Room table for long-form content (diary entries, notes). Save drafts on each text change with debouncing.

### 4.4 Low: Scroll Position
Save `LazyListState` scroll positions in `SavedStateHandle` for list-heavy screens.

---

## 5. Summary

| Aspect | Status | Grade |
|--------|--------|:-----:|
| SavedStateHandle usage | **0 of 24 ViewModels** | **F** |
| Database persistence | Room + DataStore | **A** |
| Navigation state | Relies on Compose Navigation defaults | **C** |
| Draft survival (quick record) | Lost on process death | **F** |
| Draft survival (forms) | Lost on process death | **F** |
| Architecture for recovery | Clean Architecture helps | **B** |

**Overall Process Death Grade: D+** — The app has solid data persistence but completely neglects process death recovery. Users will lose all in-progress work if the system kills the process. This is the most significant gap across all four audits.
