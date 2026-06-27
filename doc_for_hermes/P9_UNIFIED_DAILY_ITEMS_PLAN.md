# Daily Flow P9 — Unified Daily Items and Custom Dashboard Plan

> Created: 2026-06-27
> Status: Approved direction, not yet implemented
> Owner: Codex / Hermes development agents
> Scope: UX restructuring after v0.1.0 beta

---

## 0. Executive Summary

P9 changes Daily Flow from a set of parallel feature modules into a daily-use
workflow centered on one internal object: the **Daily Item**.

User decisions confirmed on 2026-06-27:

1. Calendar and Tasks should be merged into one module.
2. Daily Flow internal data is the source of truth.
3. System Calendar Provider is a one-way optional sync target: Daily Flow -> system calendar.
4. Dashboard customization is required, but drag-and-drop layout can wait.
5. Voice recognition remains an evaluation topic only and must not enter the P9 implementation plan.
6. Task range filtering should not be implemented in the legacy Tasks screen; it should be implemented after the unified Daily Items module exists.

The new primary user flow:

```text
Open Daily Flow
  -> Home dashboard shows editable panels
  -> Daily Items panel shows today / +/- 7 days / overdue
  -> User creates or completes an item inside Daily Flow
  -> Optional: Daily Flow syncs that item to system calendar
  -> Reminders remain owned by Daily Flow
```

Primary deliverables:

- `DailyItem` domain model.
- `daily_items` Room table and repository.
- migration from legacy Tasks to Daily Items.
- `ReminderTargetType.DAILY_ITEM`.
- one-way Calendar Provider sync.
- unified Daily Items list/month view.
- Daily Item create/edit/complete screen.
- range filters: today, +/- 7 days, future 7 days, this week, this month, overdue, no date, completed, custom.
- editable Dashboard panels.
- backup/restore support.
- LDPlayer end-to-end regression.

---

## 1. Current Project State

### 1.1 Relevant current code facts

- App package:
  - release: `com.dailyflow.app`
  - debug: `com.dailyflow.app.debug`
- Database:
  - current Room version: `8`
  - current schema file: `core/database/schemas/com.mhss.app.database.MyBrainDatabase/8.json`
- Task storage:
  - table: `tasks`
  - entity: `TaskEntity`
  - key fields:
    - `id`
    - `title`
    - `description`
    - `is_completed`
    - `priority`
    - `dueDate`
    - `recurring`
    - `frequency`
    - `frequency_amount`
    - `alarmId`
- Reminder storage:
  - table: `reminders`
  - target fields:
    - `target_type`
    - `target_id`
  - current target types:
    - `TASK`
    - `CALENDAR_EVENT`
    - `RECORD_PROMPT`
- Calendar event data:
  - currently lives in Android Calendar Provider, not in Room.
  - Daily Flow has repository/use cases around provider reads and writes.
- Dashboard:
  - currently composed from fixed panels.
  - no user-owned panel configuration table exists.
- Voice:
  - no implementation should be scheduled in P9.

### 1.2 Current UX problems P9 addresses

- Users must choose between Tasks and Calendar too early.
- Calendar events and tasks do not share completion, list, or filter behavior.
- Daily Flow reminders are already unified internally, but the UI still exposes split targets.
- Dashboard panels are useful but not configurable.
- Custom Tracking is pleasant and fast; the Home screen should be able to surface it more prominently.
- Tasks are useful but should become a Daily Items view with range filters.

### 1.3 Known current non-blockers

These are important, but they are not P9 prerequisites:

- AI proposal confirmation dialog needs a separate audit to verify it invokes `ProposalExecutor`.
- Pomodoro has a screen/local timer but no full foreground service/session persistence.
- Record prompt creation/configuration should be audited later.

---

## 2. Product Decisions

### 2.1 Source of truth

Daily Flow is the source of truth.

System calendar is not the source of truth. It is a sync target.

Do:

- create Daily Items in Daily Flow first.
- sync selected items to Calendar Provider.
- keep completion state inside Daily Flow.
- keep reminders inside Daily Flow.

Do not:

- import all system calendar events into Daily Items by default.
- let external calendar edits silently overwrite Daily Flow items.
- rely on Calendar Provider reminders.
- treat system calendar event existence as proof that a Daily Item still exists.

### 2.2 Calendar Provider behavior

P9 supports one-way sync:

```text
DailyItem -> Calendar Provider Event
```

External Calendar Provider modifications are handled as sync conflicts or external drift, not as authoritative updates.

### 2.3 Legacy modules

Legacy Tasks and Calendar should not be deleted immediately.

P9 should:

- preserve old data.
- migrate visible workflows to Daily Items.
- keep old routes available during transition if needed for rollback.
- stop adding new UX features to legacy Tasks.

### 2.4 Dashboard customization

First version:

- enabled/disabled panels.
- up/down ordering.
- panel-specific configuration.
- persistent settings.

Not in first version:

- drag-and-drop free layout.
- arbitrary grid resizing.
- cross-device dashboard sync.

---

## 3. Non-Goals

P9 must not include:

- built-in voice recognition implementation.
- bidirectional calendar sync.
- CalDAV.
- database encryption.
- replacement of Custom Tracking data model.
- large AI proposal/executor refactor unless needed to keep builds passing.
- Pomodoro foreground service work unless required by a compile/runtime issue.
- drag-and-drop dashboard layout.

---

## 4. Architecture

### 4.1 Preferred module layout

Use one new Android library module:

```text
daily/
  src/main/java/com/mhss/app/daily/domain/
  src/main/java/com/mhss/app/daily/data/
  src/main/java/com/mhss/app/daily/presentation/
  src/test/
  src/androidTest/
```

Rationale:

- P9 is large enough to deserve a feature module.
- A single `daily` module avoids three extra Gradle modules.
- It matches the existing `tracking` module style.
- It can depend on:
  - `core:database`
  - `core:alarm`
  - `core:notification`
  - `core:preferences`
  - `core:ui`
  - `core:util`
  - `calendar:domain` for Calendar Provider sync abstractions if useful
  - `widget` only through existing widget updater interfaces where needed

Alternative if module setup becomes risky:

- put P9 domain/data/presentation under `app/.../daily`.
- still keep package boundaries clean.
- extract to a module later.

The preferred path is the new `daily` module.

### 4.2 Dependency direction

```text
daily:presentation
  -> daily:domain
  -> daily:data interfaces

daily:data
  -> core:database
  -> calendar provider adapter
  -> core:alarm use cases

app
  -> daily
  -> navigation, Koin module registration, startup migration calls
```

No Compose screen should talk directly to DAOs or Calendar Provider.

### 4.3 Navigation target

New primary route:

```kotlin
Screen.DailyItemsScreen
Screen.DailyItemDetailsScreen(itemId: String)
Screen.DailyItemEditorScreen(itemId: String?)
```

Eventually main bottom navigation should become:

```text
Overview | Items | Records | Settings
```

During transition:

- keep old Tasks route hidden or reachable from a debug/legacy entry.
- keep old Calendar route available until Daily Items month view is stable.

---

## 5. Domain Model

### 5.1 `DailyItem`

```kotlin
data class DailyItem(
    val id: String,
    val title: String,
    val description: String,
    val kind: DailyItemKind,
    val schedule: DailyItemSchedule,
    val isCompletable: Boolean,
    val completedAtEpochMilli: Long?,
    val status: DailyItemStatus,
    val priority: DailyItemPriority,
    val recurrence: DailyItemRecurrence?,
    val color: Long?,
    val calendarSync: DailyItemCalendarSync,
    val legacySource: DailyItemLegacySource?,
    val createdAtEpochMilli: Long,
    val updatedAtEpochMilli: Long
)
```

### 5.2 `DailyItemKind`

```kotlin
enum class DailyItemKind {
    TASK,
    EVENT,
    PLAN
}
```

Meaning:

- `TASK`: due-oriented, usually completable.
- `EVENT`: start/end-oriented, may or may not be completable.
- `PLAN`: internal plan without a strict due/start time, optionally completable.

The kind is a UI semantic hint. The schedule fields define actual behavior.

### 5.3 `DailyItemSchedule`

```kotlin
data class DailyItemSchedule(
    val startAtEpochMilli: Long?,
    val endAtEpochMilli: Long?,
    val dueAtEpochMilli: Long?,
    val allDay: Boolean,
    val timeZoneId: String
)
```

Validation:

- `timeZoneId` must be non-blank and resolvable if possible.
- `endAtEpochMilli` requires `startAtEpochMilli`.
- if both start and end exist, `endAtEpochMilli >= startAtEpochMilli`.
- `dueAtEpochMilli` may coexist with start/end.
- at least one of start/end/due may be null; no-date items are valid.
- all-day items must be rendered by local date boundaries.
- all persisted times are epoch milliseconds.

### 5.4 `DailyItemStatus`

```kotlin
enum class DailyItemStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    ARCHIVED
}
```

Rules:

- `COMPLETED` requires `completedAtEpochMilli != null`.
- `ACTIVE` requires `completedAtEpochMilli == null`.
- `CANCELLED` cancels future reminders but remains visible in history if filters allow.
- `ARCHIVED` hides from normal views.

### 5.5 `DailyItemPriority`

Reuse semantics from existing `Priority`:

```kotlin
enum class DailyItemPriority {
    LOW,
    MEDIUM,
    HIGH
}
```

Mapping from legacy task priority:

```text
Priority.LOW -> DailyItemPriority.LOW
Priority.MEDIUM -> DailyItemPriority.MEDIUM
Priority.HIGH -> DailyItemPriority.HIGH
```

### 5.6 Recurrence

Initial P9 recurrence should reuse existing behavior instead of inventing a new recurrence engine.

```kotlin
data class DailyItemRecurrence(
    val frequency: DailyItemFrequency,
    val interval: Int,
    val weekDays: Set<DayOfWeek> = emptySet()
)

enum class DailyItemFrequency {
    EVERY_MINUTES,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
```

Notes:

- migrated Tasks use existing `TaskFrequency`.
- synced Calendar Provider events use current Calendar RRULE helper.
- P9 should not add complex RRULE editing beyond current app capability.

### 5.7 Calendar sync model

```kotlin
data class DailyItemCalendarSync(
    val enabled: Boolean,
    val systemCalendarId: Long?,
    val systemEventId: Long?,
    val state: CalendarSyncState,
    val lastSyncedAtEpochMilli: Long?,
    val lastLocalFingerprint: String?,
    val lastProviderFingerprint: String?,
    val lastError: String?
)

enum class CalendarSyncState {
    NOT_SYNCED,
    SYNCED,
    DIRTY,
    FAILED,
    UNLINKED,
    EXTERNAL_DRIFT
}
```

State rules:

- `NOT_SYNCED`: sync disabled or never attempted.
- `SYNCED`: provider event matches last synced fingerprint.
- `DIRTY`: local item changed after last sync.
- `FAILED`: attempted sync failed.
- `UNLINKED`: provider event was removed or user chose to disconnect.
- `EXTERNAL_DRIFT`: provider event still exists but differs from last synced provider fingerprint.

### 5.8 Legacy source

```kotlin
data class DailyItemLegacySource(
    val type: DailyItemLegacySourceType,
    val id: String
)

enum class DailyItemLegacySourceType {
    TASK,
    CALENDAR_PROVIDER_EVENT,
    MANUAL
}
```

P9 migration uses only `TASK`.

System calendar events are not imported by default. `CALENDAR_PROVIDER_EVENT` is reserved for future explicit import.

---

## 6. Storage Design

### 6.1 Database version

Increase Room database version:

```text
8 -> 9
```

Add:

- `DailyItemEntity`
- `DailyItemCalendarSyncEntity`
- `DashboardPanelEntity`
- optional `DailyItemSubTaskEntity` if preserving subtasks structurally is desired

Recommended first implementation:

- store legacy subtasks in `daily_items.sub_tasks_json`.
- avoid a separate subtasks table unless UI needs independent subtask operations.

### 6.2 `daily_items` table

Proposed columns:

```text
id TEXT PRIMARY KEY NOT NULL
title TEXT NOT NULL
description TEXT NOT NULL DEFAULT ''
kind TEXT NOT NULL
start_at INTEGER
end_at INTEGER
due_at INTEGER
all_day INTEGER NOT NULL DEFAULT 0
time_zone_id TEXT NOT NULL
is_completable INTEGER NOT NULL DEFAULT 1
completed_at INTEGER
status TEXT NOT NULL
priority TEXT NOT NULL
recurrence_frequency TEXT
recurrence_interval INTEGER
recurrence_week_days TEXT NOT NULL DEFAULT '[]'
color INTEGER
sub_tasks_json TEXT NOT NULL DEFAULT '[]'
legacy_source_type TEXT
legacy_source_id TEXT
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

Indices:

```kotlin
Index(value = ["status", "due_at"])
Index(value = ["status", "start_at"])
Index(value = ["completed_at"])
Index(value = ["legacy_source_type", "legacy_source_id"], unique = true)
```

### 6.3 `daily_item_calendar_sync` table

Proposed columns:

```text
item_id TEXT PRIMARY KEY NOT NULL
enabled INTEGER NOT NULL DEFAULT 0
system_calendar_id INTEGER
system_event_id INTEGER
state TEXT NOT NULL
last_synced_at INTEGER
last_local_fingerprint TEXT
last_provider_fingerprint TEXT
last_error TEXT
updated_at INTEGER NOT NULL
FOREIGN KEY(item_id) REFERENCES daily_items(id) ON DELETE CASCADE
```

Indices:

```kotlin
Index(value = ["enabled", "state"])
Index(value = ["system_event_id"], unique = true)
```

### 6.4 `dashboard_panels` table

Proposed columns:

```text
id TEXT PRIMARY KEY NOT NULL
type TEXT NOT NULL
enabled INTEGER NOT NULL
display_order INTEGER NOT NULL
size TEXT NOT NULL
config_json TEXT NOT NULL
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

Indices:

```kotlin
Index(value = ["enabled", "display_order"])
Index(value = ["type"])
```

### 6.5 Default dashboard panel rows

Create on first launch after migration:

```text
1. QUICK_RECORD
2. DAILY_ITEMS
3. OVERDUE_ITEMS
4. TRACKING_SUMMARY
5. PENDING_REMINDERS
6. POMODORO
```

Do not overwrite user changes once defaults are initialized.

### 6.6 Migration SQL outline

`MIGRATION_8_9` should:

1. create `daily_items`.
2. create `daily_item_calendar_sync`.
3. create `dashboard_panels`.
4. migrate `tasks` into `daily_items` using the same IDs.
5. migrate existing task reminders to target `DAILY_ITEM` because item IDs match task IDs.
6. leave `tasks` table intact for rollback/legacy reads.

Pseudo-SQL:

```sql
CREATE TABLE IF NOT EXISTS daily_items (...);
CREATE TABLE IF NOT EXISTS daily_item_calendar_sync (...);
CREATE TABLE IF NOT EXISTS dashboard_panels (...);

INSERT OR IGNORE INTO daily_items (
  id,
  title,
  description,
  kind,
  due_at,
  all_day,
  time_zone_id,
  is_completable,
  completed_at,
  status,
  priority,
  recurrence_frequency,
  recurrence_interval,
  sub_tasks_json,
  legacy_source_type,
  legacy_source_id,
  created_at,
  updated_at
)
SELECT
  id,
  title,
  description,
  'TASK',
  CASE WHEN dueDate = 0 THEN NULL ELSE dueDate END,
  0,
  'SYSTEM',
  1,
  CASE WHEN is_completed = 1 THEN updated_date ELSE NULL END,
  CASE WHEN is_completed = 1 THEN 'COMPLETED' ELSE 'ACTIVE' END,
  CASE priority WHEN 2 THEN 'HIGH' WHEN 1 THEN 'MEDIUM' ELSE 'LOW' END,
  CASE WHEN recurring = 1 THEN CAST(frequency AS TEXT) ELSE NULL END,
  CASE WHEN recurring = 1 THEN frequency_amount ELSE NULL END,
  sub_tasks,
  'TASK',
  id,
  created_date,
  updated_date
FROM tasks;

UPDATE reminders
SET target_type = 'DAILY_ITEM'
WHERE target_type = 'TASK'
  AND target_id IN (SELECT id FROM daily_items WHERE legacy_source_type = 'TASK');
```

Important:

- The migration must not delete old task rows.
- If `time_zone_id = 'SYSTEM'` is too ambiguous, a post-migration startup use case can rewrite it to `TimeZone.currentSystemDefault().id`. Room SQL migration cannot reliably ask Kotlin for the current time zone.
- Reminder retargeting must only happen after `ReminderTargetType.DAILY_ITEM` exists in Kotlin.

### 6.7 Schema validation

Add Android migration tests:

- empty v8 -> v9.
- v8 with incomplete task.
- v8 with completed task.
- v8 with task due date and reminder.
- v8 with task no due date.
- idempotence when daily item already exists.

---

## 7. Repository and Use Cases

### 7.1 Repository interface

```kotlin
interface DailyItemRepository {
    fun observeItems(filter: DailyItemFilter): Flow<List<DailyItem>>
    fun observeItem(id: String): Flow<DailyItem?>
    suspend fun getItem(id: String): DailyItem?
    suspend fun upsert(item: DailyItem): DailyItem
    suspend fun update(item: DailyItem)
    suspend fun delete(id: String)
    suspend fun markCompleted(id: String, completedAt: Long)
    suspend fun reopen(id: String, updatedAt: Long)
    suspend fun archive(id: String, updatedAt: Long)
}
```

### 7.2 Filter model

```kotlin
data class DailyItemFilter(
    val range: DailyItemRange = DailyItemRange.Today,
    val status: DailyItemStatusFilter = DailyItemStatusFilter.Active,
    val includeNoDate: Boolean = false,
    val includeCompleted: Boolean = false,
    val syncState: CalendarSyncState? = null,
    val priority: DailyItemPriority? = null,
    val query: String = ""
)
```

### 7.3 Range model

```kotlin
sealed interface DailyItemRange {
    data object Today : DailyItemRange
    data object Tomorrow : DailyItemRange
    data object SurroundingSevenDays : DailyItemRange
    data object FutureSevenDays : DailyItemRange
    data object ThisWeek : DailyItemRange
    data object ThisMonth : DailyItemRange
    data object Overdue : DailyItemRange
    data object NoDate : DailyItemRange
    data object All : DailyItemRange
    data class Custom(val startInclusive: LocalDate, val endInclusive: LocalDate) : DailyItemRange
}
```

### 7.4 Range semantics

`SurroundingSevenDays`:

```text
start = today - 7 days at local start of day
end = today + 7 days exclusive local end boundary
include:
  - items with startAt in range
  - items with dueAt in range
  - overdue active items before start if includeOverdueCarry = true
default includeOverdueCarry = true
```

This is the replacement for the requested "Tasks +/- 1 week" view.

### 7.5 Core use cases

```text
CreateDailyItemUseCase
UpdateDailyItemUseCase
CompleteDailyItemUseCase
DeleteDailyItemUseCase
ObserveDailyItemsUseCase
GetDailyItemUseCase
ArchiveDailyItemUseCase
ReopenDailyItemUseCase
SyncDailyItemToCalendarUseCase
DisableDailyItemCalendarSyncUseCase
ReconcileDailyItemCalendarSyncUseCase
MigrateLegacyTasksToDailyItemsUseCase
EnsureDefaultDashboardPanelsUseCase
SaveDashboardPanelConfigUseCase
```

### 7.6 Create flow

Pseudo-code:

```kotlin
suspend fun create(input: DailyItemInput): Result<DailyItem> {
    validate(input)
    val now = clock.now()
    val item = input.toDailyItem(id = uuid(), createdAt = now, updatedAt = now)
    repository.upsert(item)
    reminderDrafts.saveForDailyItem(item.id)
    if (item.calendarSync.enabled) {
        syncDailyItemToCalendar(item.id)
    }
    widgetUpdater.updateAll(...)
    return Result.success(item)
}
```

### 7.7 Update flow

Pseudo-code:

```kotlin
suspend fun update(id: String, input: DailyItemInput): Result<DailyItem> {
    val old = repository.getItem(id) ?: return notFound()
    validate(input)
    val now = clock.now()
    val new = input.merge(old).copy(updatedAtEpochMilli = now)
    repository.update(new)
    rescheduleDailyItemReminders(id, now)
    markSyncDirtyIfSynced(old, new)
    if (new.calendarSync.enabled) syncDailyItemToCalendar(id)
    widgetUpdater.updateAll(...)
    return Result.success(new)
}
```

### 7.8 Complete flow

Completing a Daily Item should:

1. update `status = COMPLETED`.
2. set `completedAtEpochMilli`.
3. cancel future reminders.
4. leave delivered reminder history unchanged.
5. update dashboard/widgets.
6. optionally sync a provider description/title marker if configured.

Do not delete provider event merely because item was completed.

Pseudo-code:

```kotlin
suspend fun complete(id: String, completedAt: Long) {
    repository.markCompleted(id, completedAt)
    cancelFutureDailyItemReminders(id, completedAt)
    if (syncPolicy.writeCompletionMarker) {
        syncDailyItemToCalendar(id)
    }
}
```

### 7.9 Delete flow

Delete options:

```kotlin
enum class DailyItemDeleteMode {
    DELETE_APP_ONLY_KEEP_PROVIDER_EVENT,
    DELETE_APP_AND_PROVIDER_EVENT,
    ARCHIVE_APP_ONLY
}
```

Default behavior:

- if not synced: delete app item.
- if synced: ask user.
- if provider delete fails: keep app item archived or mark sync `FAILED`, depending on user choice.

### 7.10 Legacy task migration use case

Even if SQL migration creates daily items, keep a startup reconciliation use case:

```text
MigrateLegacyTasksToDailyItemsUseCase
```

Responsibilities:

- fill missing timezone from `TimeZone.currentSystemDefault().id`.
- fix old task rows that were added after migration by a legacy path.
- retarget any remaining `TASK` reminders to `DAILY_ITEM`.
- log counts without exposing personal content.

It must be idempotent.

---

## 8. Calendar Provider One-Way Sync

### 8.1 Sync policy

Daily Flow should expose one switch:

```text
Sync to system calendar
```

When disabled:

- no provider event is created.
- existing provider link can be kept but marked disabled, or user can unlink.

When enabled:

- `SyncDailyItemToCalendarUseCase` creates or updates provider event.
- sync state becomes `SYNCED` or `FAILED`.

### 8.2 Provider event mapping

Mapping:

```text
DailyItem.title -> Events.TITLE
DailyItem.description -> Events.DESCRIPTION
DailyItem.schedule.startAt -> Events.DTSTART
DailyItem.schedule.endAt -> Events.DTEND or duration
DailyItem.schedule.allDay -> Events.ALL_DAY
DailyItem.schedule.timeZoneId -> Events.EVENT_TIMEZONE
DailyItem.recurrence -> Events.RRULE
DailyItem.color -> best effort only if supported
```

Due-only items:

Default:

- do not sync unless the user enables sync.

If sync enabled:

Options in editor:

```text
Calendar representation:
  - All-day event on due date
  - Timed event at due time
  - Do not sync this due-only item
```

P9 default:

- if `dueAt` includes a time, create a short timed event.
- if due is date-only/all-day, create all-day event.

### 8.3 Provider fingerprints

Use fingerprints to avoid silently overwriting external changes.

Local fingerprint includes:

```text
title
description
startAt
endAt
dueAt
allDay
timeZoneId
recurrence
```

Provider fingerprint includes:

```text
Events.TITLE
Events.DESCRIPTION
Events.DTSTART
Events.DTEND
Events.ALL_DAY
Events.EVENT_TIMEZONE
Events.RRULE
```

On sync:

- calculate current provider fingerprint if event exists.
- if provider fingerprint differs from `lastProviderFingerprint` and local state did not initiate it, mark `EXTERNAL_DRIFT`.
- show user choices:
  - overwrite system calendar with Daily Flow item.
  - unlink provider event.
  - ignore for now.

### 8.4 Sync error handling

Common failures:

- no calendar permission.
- provider event deleted.
- selected calendar no longer exists.
- write permission revoked.
- provider insert/update returns null.

Behavior:

- do not lose Daily Item.
- set `syncState = FAILED` or `UNLINKED`.
- persist `lastError` using a stable short error code, not raw stack traces.
- show recoverable UI message.

### 8.5 No reverse sync

P9 must not:

- observe all provider events and create Daily Items.
- update Daily Items from provider events automatically.
- auto-delete Daily Items when provider events disappear.

Optional future idea:

- explicit "Import from system calendar" wizard, not in P9.

---

## 9. Reminders

### 9.1 Extend target type

Add:

```kotlin
enum class ReminderTargetType {
    TASK,
    CALENDAR_EVENT,
    RECORD_PROMPT,
    DAILY_ITEM
}
```

### 9.2 Resolver behavior

For `DAILY_ITEM`, target time resolution:

```text
if item.status is COMPLETED/CANCELLED/ARCHIVED:
    return null
if item.schedule.startAt != null:
    return startAt
else if item.schedule.dueAt != null:
    return dueAt
else:
    return null
```

Absolute reminders:

- use `absoluteTriggerAt` directly.

Relative reminders:

- use resolved target time.

No target time:

- reminder remains `PENDING`, but the editor should prevent creating relative reminders for no-date items unless the item later receives a date.

### 9.3 Migration of reminders

Because migrated Daily Item ID equals legacy Task ID:

```sql
UPDATE reminders
SET target_type = 'DAILY_ITEM'
WHERE target_type = 'TASK'
  AND target_id IN (SELECT id FROM daily_items WHERE legacy_source_type = 'TASK');
```

Calendar event reminders should not be automatically migrated because Provider events are not imported into Daily Items.

New Daily Items should use `DAILY_ITEM` target.

### 9.4 Notification behavior

New notification content:

```text
title: item.title
text:
  - if timed: formatted start/end
  - if due-only: "Due <time>"
  - if no description: item kind/status hint
actions:
  - Complete, if item.isCompletable and status ACTIVE
  - Open
```

Notification tap:

```text
app://com.dailyflow.app.daily_item/{id}
```

Completion action:

- calls `CompleteDailyItemUseCase`.
- cancels future reminders for the item.
- cancels all visible notifications with reminder IDs for that item if practical.

### 9.5 Reminder editor reuse

Reuse `MultiReminderEditor` but adapt labels:

- "At start"
- "At due time"
- "5 min before"
- "15 min before"
- "30 min before"
- "1 hour before"
- "1 day before"
- "Custom time"

Validation:

- duplicate absolute time blocked.
- duplicate relative offset blocked.
- past absolute time blocked.
- relative reminders require target time.

---

## 10. Unified Daily Items UI

### 10.1 Main screen

`DailyItemsScreen`

Header:

- title: "Items" / localized Chinese "事项".
- view mode switch:
  - List
  - Calendar
- search icon.
- filter icon.

Primary chips:

```text
Today
+/- 7 days
Future 7 days
This week
This month
Overdue
No date
Completed
All
```

Default:

- `Today` on first launch.
- remember last selected filter in preferences.

### 10.2 List grouping

List groups:

```text
Overdue
Today
Tomorrow
This week
Later
No date
Completed
```

Each row:

- checkbox if completable.
- title.
- time/due label.
- sync icon if synced/dirty/failed.
- reminder icon count.
- priority indicator.

Row actions:

- tap opens details.
- checkbox completes/reopens.
- overflow:
  - edit.
  - duplicate.
  - toggle calendar sync.
  - archive/delete.

### 10.3 Calendar view

Daily Items month view should replace the old split mental model.

Date cells show:

- event dots for timed items.
- due badges for due-only items.
- completion indicators.
- local tracking values can remain from DF-1003 if space allows.

Bottom day detail:

```text
Selected date
  Daily Items
  Tracking records
```

System-only calendar events:

- not shown by default as Daily Items.
- optional read-only overlay can remain in legacy Calendar screen during transition.

### 10.4 Details screen

Sections:

1. title and status.
2. completion toggle.
3. schedule:
   - start/end.
   - due.
   - all-day.
   - timezone.
4. reminders.
5. calendar sync:
   - enabled.
   - sync state.
   - selected calendar.
   - last sync.
   - error/retry.
6. description.
7. subtasks if migrated from task.
8. metadata/legacy source if debug mode.

### 10.5 Editor screen

Inputs:

- title.
- description.
- "Has time block" toggle.
- start date/time.
- end date/time.
- "Has due date" toggle.
- due date/time.
- all-day toggle.
- completable toggle.
- priority.
- color.
- recurrence.
- reminders.
- "Sync to system calendar" toggle.
- target calendar picker.
- due-only calendar representation option.

Validation:

- title cannot be blank.
- end cannot be before start.
- due-only sync needs all-day or timed representation.
- relative reminders require a target time.
- calendar sync requires permission and selected calendar.

---

## 11. Daily Item Filters

### 11.1 Filter implementation order

Do not implement legacy Tasks filtering first.

Order:

1. build Daily Item repository and list screen.
2. migrate tasks into Daily Items.
3. implement Daily Item range filters.
4. hide/de-emphasize legacy Tasks screen.

### 11.2 Required filters

`Today`:

- start/due inside today's local day.
- active by default.

`SurroundingSevenDays`:

- local date from `today - 7` to `today + 7`, inclusive.
- includes overdue active items before the range in a small "Overdue" group.

`FutureSevenDays`:

- today through today + 7.

`ThisWeek`:

- respects first day of week preference.

`ThisMonth`:

- current local calendar month.

`Overdue`:

- active completable items whose dueAt or startAt is before now.

`NoDate`:

- no startAt and no dueAt.

`Completed`:

- status completed, default sorted by completedAt descending.

`All`:

- active + completed if includeCompleted enabled.

`Custom`:

- date picker start/end.

### 11.3 Sorting

Default sorting:

```text
overdue active first
then startAt
then dueAt
then priority high -> low
then updatedAt descending
then title
```

Completed sorting:

```text
completedAt descending
```

No-date sorting:

```text
priority high -> low
updatedAt descending
title
```

---

## 12. Dashboard Customization

### 12.1 Dashboard panel model

```kotlin
data class DashboardPanel(
    val id: String,
    val type: DashboardPanelType,
    val enabled: Boolean,
    val displayOrder: Int,
    val size: DashboardPanelSize,
    val config: DashboardPanelConfig,
    val createdAtEpochMilli: Long,
    val updatedAtEpochMilli: Long
)
```

### 12.2 Panel types

```kotlin
enum class DashboardPanelType {
    QUICK_RECORD,
    DAILY_ITEMS,
    OVERDUE_ITEMS,
    PENDING_REMINDERS,
    TRACKING_SUMMARY,
    TRACKING_TRENDS,
    POMODORO,
    AI_ASSISTANT,
    CALENDAR_SYNC_STATUS
}
```

### 12.3 Panel size

```kotlin
enum class DashboardPanelSize {
    COMPACT,
    MEDIUM,
    LARGE
}
```

First version:

- size controls max content and card height.
- no grid span or drag.

### 12.4 Config JSON

Use kotlinx.serialization.

Examples:

```kotlin
@Serializable
sealed interface DashboardPanelConfig

@Serializable
data class DailyItemsPanelConfig(
    val range: DailyItemRangePreset,
    val maxItems: Int = 5,
    val showCompleted: Boolean = false,
    val showSyncState: Boolean = true
) : DashboardPanelConfig

@Serializable
data class QuickRecordPanelConfig(
    val templateIds: List<String> = emptyList(),
    val maxTemplates: Int = 4
) : DashboardPanelConfig

@Serializable
data class TrackingSummaryPanelConfig(
    val dateRange: TrackingDashboardRange = TrackingDashboardRange.TODAY,
    val maxRows: Int = 4,
    val showSparkline: Boolean = true
) : DashboardPanelConfig
```

### 12.5 Dashboard edit screen

Route:

```text
Screen.DashboardEditScreen
```

UI:

- list of all panels.
- switch enable/disable.
- up/down buttons.
- configure button.
- reset to defaults.

No drag-and-drop in P9.

### 12.6 Panel configuration screens

At minimum:

- Daily Items panel:
  - range.
  - max item count.
  - include completed.
  - show sync state.
- Quick Record panel:
  - choose templates.
  - max template count.
- Tracking Summary panel:
  - date range.
  - show/hide sparkline.
- Pending Reminders:
  - max count.
  - include delivered? default false.

### 12.7 Dashboard rendering

Home screen should:

- load ordered enabled panels.
- render unknown panel types as hidden with a logged warning.
- tolerate malformed config by falling back to default config.
- never crash because of dashboard config.

---

## 13. Backup and Restore

### 13.1 JSON backup

Increment backup schema version.

Add:

```text
dailyItems
dailyItemCalendarSync
dashboardPanels
```

Rules:

- do not export API keys.
- do not export keystore ciphertext.
- do not export transient sync errors unless useful as user-visible status.
- system calendar IDs may be exported, but restore must treat them as stale until re-linked.

### 13.2 Restore

On restore:

- import Daily Items.
- import dashboard panels.
- import reminders.
- set calendar sync state to `UNLINKED` or `DIRTY_RELINK_REQUIRED` if provider IDs are not valid on this device.
- never write to Calendar Provider during restore without explicit user action.

### 13.3 Restore preview

Preview counts:

```text
Daily Items: N
Dashboard panels: N
Reminders: N
Calendar sync links: N stale links
```

---

## 14. Global Search

Search should include Daily Items.

After Daily Items is primary:

- Tasks search results should either be removed or mapped to Daily Items.
- Calendar Provider event search remains legacy/read-only unless synced to Daily Items.

Daily Item search result:

```text
icon: item kind/status
title
subtitle: date/time/due + sync state if relevant
tap: DailyItemDetailsScreen
```

---

## 15. Widgets

P9 minimum:

- existing tracking widget remains unchanged.
- any task widget or dashboard item count should use Daily Items once available.

Optional:

- create Daily Items widget later.

Do not block P9 on widget creation.

---

## 16. Testing Strategy

### 16.1 JVM tests

Add tests for:

- `DailyItem` validation.
- date range calculation.
- sorting/grouping.
- task priority mapping.
- recurrence mapping from task frequency.
- completion cancels reminders.
- sync state transitions.
- dashboard config parsing.
- dashboard ordering.
- malformed dashboard config fallback.

### 16.2 Room instrumentation tests

Add tests for:

- `MIGRATION_8_9` empty DB.
- task migration to daily item.
- completed task migration.
- task reminders retargeted to `DAILY_ITEM`.
- dashboard tables exist.
- sync mapping foreign key cascade.
- schema export.

### 16.3 Calendar Provider tests

Use fake repository for JVM tests:

- create provider event.
- update provider event.
- provider delete/unlink.
- permission denied -> sync failed.
- external drift -> `EXTERNAL_DRIFT`.

Use LDPlayer manual/ADB tests:

- actual provider write if calendar permission available.
- check event appears in system calendar provider query.

### 16.4 Compose/UI tests

Add targeted UI tests if current test infrastructure supports:

- Daily Item editor validation.
- range chips.
- completion checkbox.
- dashboard edit enable/disable.
- dashboard panel order.

### 16.5 LDPlayer regression

Use:

```powershell
powershell -File "tools/android/ldplayer.ps1" -Action start
```

Build:

```powershell
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" --no-problems-report :app:assembleDebug
```

Install:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5556 install -r "$env:LOCALAPPDATA\DailyFlow\build\app\outputs\apk\debug\app-debug.apk"
```

Scenarios:

1. upgrade from existing beta data.
2. verify migrated tasks appear as Daily Items.
3. verify migrated reminders fire and open Daily Item details.
4. create no-sync Daily Item.
5. create sync-enabled Daily Item.
6. modify sync-enabled Daily Item and verify provider update.
7. complete item and verify future reminders canceled.
8. select +/- 7 days filter.
9. edit Dashboard panels.
10. restart app and verify Dashboard config persists.
11. export/import backup and verify Daily Items restored.
12. logcat check for crash/fatal/ANR.

---

## 17. Implementation Tasks

### DF-1101 — Document state calibration and P9 UX spec

Goal:

- Align docs with actual beta/post-beta state.
- Record P9 decisions.

Files:

- `docs/DEVELOPMENT_PROGRESS.md`
- `doc_for_hermes/DEVELOPMENT_LOG.md`
- `doc_for_hermes/NEXT_DEVELOPMENT_PLAN.md`
- `doc_for_hermes/P9_UNIFIED_DAILY_ITEMS_PLAN.md`

Acceptance:

- docs no longer point agents to old DF-901..906 as next work.
- P9 plan exists.
- voice is explicitly evaluation-only.

Verification:

- `git diff --check`

### DF-1102 — Daily module skeleton and domain model

Goal:

- Add `daily` module or approved fallback package.
- Implement domain models and validation.

Production files:

```text
daily/src/main/.../domain/model/DailyItem.kt
daily/src/main/.../domain/model/DailyItemSchedule.kt
daily/src/main/.../domain/model/DailyItemFilter.kt
daily/src/main/.../domain/model/DailyItemRange.kt
daily/src/main/.../domain/validation/DailyItemValidator.kt
```

Tests:

```text
daily/src/test/.../DailyItemValidatorTest.kt
daily/src/test/.../DailyItemRangeTest.kt
```

Acceptance:

- no end-before-start items.
- completed state requires completedAt.
- no-date items are valid.
- +/- 7 day range is timezone safe.

### DF-1103 — Room schema v9 and repository

Goal:

- Add v9 tables and repository.

Files:

```text
core/database/entity/DailyItemEntity.kt
core/database/entity/DailyItemCalendarSyncEntity.kt
core/database/entity/DashboardPanelEntity.kt
core/database/dao/DailyItemDao.kt
core/database/dao/DashboardPanelDao.kt
core/database/migrations/RoomMigrations.kt
```

Acceptance:

- Room schema exports v9.
- migration tests pass.
- repository can observe/filter Daily Items.

### DF-1104 — Legacy Task migration

Goal:

- Existing tasks become Daily Items.

Implementation:

- SQL migration inserts tasks into daily_items.
- startup use case reconciles missing timezone and late legacy rows.
- task reminders retarget to `DAILY_ITEM`.

Acceptance:

- task title/description/priority/due/completed/subtasks preserved.
- old task rows remain.
- no duplicate Daily Items after repeated startup.
- reminders not duplicated.

### DF-1105 — Daily Item reminder target

Goal:

- Unified reminders support `DAILY_ITEM`.

Files:

```text
core/alarm/model/Reminder.kt
app/.../ReminderTargetTimeResolverImpl.kt
core/notification/.../ReminderReceiver.kt
core/notification/.../NotificationUtil.kt
```

Acceptance:

- Daily Item reminders schedule.
- complete action works from notification.
- completed/cancelled items do not keep future reminders.

### DF-1106 — One-way Calendar Provider sync

Goal:

- Daily Item can sync to system calendar.

Use cases:

```text
SyncDailyItemToCalendarUseCase
DisableDailyItemCalendarSyncUseCase
ReconcileDailyItemCalendarSyncUseCase
```

Acceptance:

- create provider event from item.
- update provider event from item.
- failed permission marks sync failed without losing item.
- external drift is detected, not silently imported.

### DF-1107 — Daily Items list screen

Goal:

- New unified list UI with filters.

UI:

- chips for Today, +/- 7 days, Future 7 days, This week, This month, Overdue, No date, Completed, All.
- groups and rows.
- completion checkbox.

Acceptance:

- migrated tasks appear.
- checkbox completes item.
- +/- 7 days filter works.
- no legacy Tasks filtering required.

### DF-1108 — Daily Item editor/details

Goal:

- Create/edit all item variants.

Acceptance:

- create task-like item.
- create event-like item.
- create due-only item.
- create no-date plan.
- attach multiple reminders.
- enable/disable calendar sync.
- validation errors are visible.

### DF-1109 — Daily Items calendar view

Goal:

- Month view shows Daily Items and selected-day detail.

Acceptance:

- timed items appear on date.
- due-only items appear on due date.
- completed items are visually distinct.
- selected-day detail opens item details.

### DF-1110 — Dashboard panel persistence

Goal:

- Add Dashboard panel repository and defaults.

Acceptance:

- default panels inserted once.
- config survives restart.
- invalid config falls back safely.

### DF-1111 — Dashboard edit UI

Goal:

- User can edit Home panels without drag/drop.

Acceptance:

- enable/disable panel.
- move panel up/down.
- configure Daily Items panel range and max rows.
- configure Quick Record templates.
- reset defaults.

### DF-1112 — Home dashboard rendering

Goal:

- Home reads panel configuration.

Acceptance:

- Custom Tracking panel can be prominent.
- Daily Items panel can show Today or +/- 7 days.
- hidden panels disappear.
- order persists.

### DF-1113 — Navigation restructuring

Goal:

- Main navigation emphasizes Items and Records.

Acceptance:

- bottom nav includes Items.
- old Tasks/Calendar entry is de-emphasized or legacy.
- deep links route reminders to Daily Item details.

### DF-1114 — Backup/restore Daily Items and dashboard

Goal:

- JSON backup includes new data.

Acceptance:

- backup round trip preserves Daily Items.
- dashboard config preserved.
- calendar sync links restored as stale/unlinked unless reconnected.

### DF-1115 — Search and global integration

Goal:

- Global search and Today/overview use Daily Items.

Acceptance:

- search results open Daily Item details.
- old task search is not duplicated.
- reminders card links to Daily Items.

### DF-1116 — LDPlayer regression and P9 beta report

Goal:

- Verify P9 end to end.

Artifacts:

```text
doc_for_hermes/P9_BETA_REGRESSION_REPORT_<date>.md
```

Acceptance:

- all P9 LDPlayer scenarios completed.
- logcat has no crash/fatal/ANR.
- debug build and release build pass.

---

## 18. Voice Recognition Evaluation Appendix

Voice is not part of P9 implementation.

This section exists only to preserve options for a future spike.

### 18.1 Option A — System input method voice

Description:

- User taps a microphone on the keyboard/input method.
- Daily Flow only receives text.

Pros:

- almost no app code.
- no model bundled.
- minimal privacy responsibility.
- works with current text fields.

Cons:

- not truly built into Daily Flow.
- depends on input method.
- UX varies heavily.
- app cannot own recognition UI.

Good for:

- immediate workaround.

Not good for:

- a consistent built-in voice feature.

### 18.2 Option B — Android `SpeechRecognizer` / `RecognizerIntent`

References:

- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/reference/android/speech/RecognizerIntent

Pros:

- official Android API.
- can be launched from an in-app microphone button.
- supports partial results on capable services.
- no bundled model.

Cons:

- availability depends on recognition service on the device.
- offline behavior is device/service dependent.
- Chinese mainland devices may not have reliable Google recognition service.
- permission and lifecycle handling add complexity.

Good for:

- low-cost in-app prototype on devices with a good recognizer service.

Not good for:

- guaranteed offline recognition.

### 18.3 Option C — Cloud STT

Candidate categories:

- OpenAI speech-to-text / transcription APIs.
- Google Cloud Speech-to-Text.
- domestic vendors such as iFlytek, Baidu, Alibaba, Volcano Engine.

References:

- https://platform.openai.com/docs/guides/speech-to-text
- https://docs.cloud.google.com/speech-to-text/docs/v1/transcribe-streaming-audio
- https://docs.cloud.google.com/speech-to-text/docs/v1/data-usage-faq

Pros:

- usually best accuracy.
- good Chinese punctuation and long-form transcription possible depending on provider.
- can support streaming and post-processing.

Cons:

- network required.
- cost.
- user audio leaves device.
- API keys and provider credentials must be protected.
- privacy disclosures and explicit consent are required.
- F-Droid review may be harder if cloud SDKs or proprietary dependencies are bundled.

Good for:

- optional provider-based advanced feature.

Not good for:

- default offline-first Daily Flow experience.

### 18.4 Option D — Local Whisper / whisper.cpp

Reference:

- https://github.com/ggml-org/whisper.cpp

Pros:

- offline.
- privacy friendly.
- strong multilingual recognition.
- no cloud account required.

Cons:

- model distribution is large.
- CPU/battery/thermal cost.
- native library integration.
- model download/storage UI required.
- low-end phones may be slow.
- release packaging and F-Droid metadata need careful handling.

Good for:

- future offline voice premium/advanced mode.

Not good for:

- quick P9 UX restructuring.

### 18.5 Option E — Vosk

Reference:

- https://alphacephei.com/vosk/
- https://alphacephei.com/vosk/models

Pros:

- offline.
- relatively small models compared with Whisper.
- Android usage is established.
- streaming recognition possible.

Cons:

- Chinese accuracy needs real-device testing.
- model quality varies.
- still adds binary/model management complexity.

Good for:

- lightweight offline spike.

Not good for:

- high-quality dictation without careful model evaluation.

### 18.6 Option F — sherpa-onnx

Reference:

- https://k2-fsa.github.io/sherpa/onnx/index.html

Pros:

- offline.
- supports Android/iOS examples.
- model choices include streaming/non-streaming.
- ONNX ecosystem is flexible.

Cons:

- integration is more complex than system STT.
- model selection and packaging need research.
- app size and CPU use must be tested.

Good for:

- serious offline voice spike after P9.

Not good for:

- immediate implementation.

### 18.7 Recommended future voice path

After P9, do a small spike:

```text
VOICE-001 Define VoiceInputProvider abstraction
VOICE-002 Prototype Android SpeechRecognizer
VOICE-003 Prototype one local engine, Whisper or sherpa-onnx
VOICE-004 Compare accuracy, latency, APK/model size, privacy, battery
VOICE-005 Decide whether voice enters v1.1/v1.2
```

Do not merge any provider-specific voice implementation before the abstraction exists.

---

## 19. Rollback Strategy

P9 touches central workflows, so rollback must be possible.

Rules:

- keep old task rows.
- keep old calendar screens until P9 passes LDPlayer regression.
- do not delete old reminders.
- retargeting reminders from `TASK` to `DAILY_ITEM` is acceptable because Daily Item ID equals Task ID, but tests must prove notifications still open correctly.
- dashboard config can be reset to defaults.

If Daily Items UI fails late:

- keep v9 data tables.
- hide Daily Items nav entry.
- leave migrated data dormant.
- old Tasks can still read old task rows.

---

## 20. Definition of Done

P9 is complete only when:

- migrated tasks appear as Daily Items.
- users can create a Daily Flow internal item without touching system calendar.
- users can optionally sync a Daily Item to system calendar.
- app changes sync to system calendar.
- system calendar does not silently overwrite app data.
- users can complete items in app.
- completing items cancels future reminders.
- users can view Daily Items by Today and +/- 7 days.
- Home Dashboard can be edited without drag/drop.
- Custom Tracking quick record can be made prominent on Home.
- backup/restore includes Daily Items and Dashboard config.
- LDPlayer regression has no crash/fatal/ANR.
- release build succeeds.
- documentation is updated with the actual completed behavior.
