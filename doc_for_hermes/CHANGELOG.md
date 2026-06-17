# Daily Flow Changelog

## v0.1.0-beta — P8 Release Preparation (2026-06-17)

### Overview

Daily Flow v0.1.0-beta is the first public beta release, encompassing all
features developed from P0 through P7. It is a comprehensive personal
productivity app with tracking, tasks, calendar, AI assistant, backup, and
widget capabilities.

---

### P0 — Project Foundation

- **DF-001**: Brand migration from "My Brain" to "Daily Flow" — application
  ID `com.dailyflow.app`, new icons, theme, and naming.
- **DF-002**: Four-tab bottom navigation — Tasks, Calendar, Quick Record,
  Settings.
- **DF-003**: Gradle 8.13, Kotlin 2.3.10, AGP 8.13.2, Compose BOM 2026.02.01.
- **DF-004**: Project structure — multi-module architecture with clean
  separation (data, domain, presentation layers).
- **DF-005**: Brand identity — unified theme, Material 3, custom color scheme.

### P1 — Tracking & Quick Record

- **DF-101**: Six-table tracking data model — trackers, options, sessions,
  data points, templates, fields.
- **DF-102**: Room database with v8 migration, repository pattern.
- **DF-103**: Eight field types — number, text, boolean, duration, rating,
  choice, time, datetime, with validation.
- **DF-104**: Built-in templates — fitness, mood, habit tracking.
- **DF-105**: Single dynamic field entry — renders all field types from one
  composable.
- **DF-106**: Default/recent/frequent suggestions, counter increment,
  text history.
- **DF-107**: Template management — create, edit, copy, reorder, pin, archive.
- **DF-108**: Quick Record page — all template fields on one page, session
  notes, occurrence time, suggestions.
- **DF-109**: History page — filter by template/date, view/edit/delete sessions,
  undo delete.
- **DF-110**: Dashboard overview — quick-access common templates, today's
  counts, template summaries.

### P2 — Calendar & Widget

- **DF-201**: Calendar month view — date cells with event dots and local
  record indicators.
- **DF-202**: Day/Week/Month timezone-safe binning — sum, count, average,
  min, max, moving window aggregates.
- **DF-203**: Calendar Provider integration — sync with system calendar,
  new event creation.
- **DF-204**: Custom Records space — entry point for tracking without
  calendar context.
- **DF-205**: Desktop widget — up to 4 pinned templates, empty state,
  deep-link to Quick Record.
- **DF-206**: Widget auto-refresh on template changes.
- **DF-207**: Calendar permissions — graceful degradation when calendar
  access denied.

### P3 — Statistics & CSV

- **DF-301**: Statistics queries — daily summaries, weekly/monthly sequences,
  option distributions, current/longest streaks.
- **DF-302**: Empty-bin filling, disabled tracker history, DST handling,
  custom day boundaries, cross-month/year support.
- **DF-303**: AndroidPlot integration — line, bar, and pie charts via
  Compose `AndroidView`.
- **DF-304**: Stats page — 365-day, ~52-week, 12-month views with tracker,
  aggregation, and chart type selectors.
- **DF-305**: Background computation — stats calculated on background
  dispatcher with cancellation support.
- **DF-306**: CSV v1 — single-file multi-record format with UTF-8 BOM,
  ISO 8601 dates, import/export with preview validation, atomic transactions.

### P4 — Unified Reminders

- **DF-401**: Unified `reminders` table and `ReminderRepository` with SQLite
  backend.
- **DF-402**: Reminder scheduling, cancellation, recalculation, recovery,
  expiry cleanup, single-fire state machine.
- **DF-403**: Notification channels — task, calendar, record prompt channels
  with deep-link navigation.
- **DF-405**: Task multi-reminder migration — old alarm system → unified
  reminders, backwards compatible.
- **DF-406**: Calendar event multi-reminders — Provider event ID association,
  time-change recalculation, dangling target handling.
- **DF-407**: Record prompts — daily/weekday reminder at fixed time per
  template, auto-cancel on template deactivation.
- **DF-408**: Multi-reminder editor composable — quick presets (on-time,
  5/15/30 min, 1 hour, 1 day before), custom absolute time, duplicate/past
  time prevention.

### P5 — AI Proposal Confirmation

- **DF-501**: DeepSeek provider integration — structured tool-calling
  contract tests.
- **DF-502**: `ActionProposal` sealed model — 6 proposal types:
  CreateTask, CreateCalendarEvent, CreateDiaryEntry, CreateRecordSession,
  CompleteTask, ClarificationRequest.
- **DF-503**: Write-tools converted to "proposal-only" — AI suggests actions
  without direct database writes.
- **DF-504**: Tracking AI tools — getRecordTemplates, proposeCreateRecordSession,
  searchRecordSessions.
- **DF-505**: `DateAmbiguityResolver` — relative days, weekdays, time points,
  completeness checks with device timezone.
- **DF-506**: `ConfirmationCard` composable — 5 card types with edit/confirm/
  cancel interactions.
- **DF-507**: `ProposalExecutor` — single-use proposal IDs, post-confirm
  revalidation, atomic writes.
- **DF-508**: `AiLogSanitizer` — API key redaction, request body truncation,
  payload previews.
- **DF-509**: `summarizeStatistics` — pre-aggregated stats sent as compact
  summary to AI, no raw data upload.

### P6 — Security, Backup & Privacy

- **DF-601**: App lock — device credentials/biometrics, background timeout,
  task thumbnail blocking, reminder-friendly.
- **DF-604**: Extended app lock audit — existing infrastructure verified.
- **DF-605**: JSON backup v2 — includes `reminders` table, `schemaVersion`,
  excludes API keys, keystore ciphertext, temporary proposals.
- **DF-606**: Restore preflight and atomic restore — version validation,
  count preview, transaction rollback on failure.
- **DF-607**: Release network hardening — cleartext HTTP prohibition,
  exported component audit, no ads/tracking/closed-source analytics SDKs.

### P7 — Integration & Polish

- **DF-701**: Unified Today view — `PendingRemindersCard` on dashboard with
  target-type icons and navigation.
- **DF-702**: Global search — cross-content-type search (tasks, events,
  diary, notes, tracking sessions), grouped results, type icons.
- **DF-703**: Strings/theming/accessibility — 19 new string resources,
  content descriptions, touch targets, color signals audit.
- **DF-704**: Performance audit — main-thread blocking analysis, N+1 query
  detection, index recommendations.
- **DF-705**: Process death audit — SavedStateHandle gap analysis across
  24 ViewModels.
- **DF-706**: Regression test suite — 136+ tests verified passing across
  core modules.

---

### P8 — Release Preparation (Current)

- **DF-801**: Dependency & license audit — all 17 direct dependencies
  confirmed Apache 2.0 or MIT, F-Droid and GPLv3 compatible.
- **DF-802**: Release signing configuration — env-var and local.properties
  based, no hardcoded paths.
- **DF-803**: Changelog and release documentation.
- **DF-804**: (Pending) GitHub release creation.
- **DF-805**: (Pending) F-Droid metadata preparation.
- **DF-806**: (Pending) Beta distribution.

---

### Known Issues & Limitations

- DF-012: AI provider contract tests not yet complete.
- DF-005: Brand-change side-by-side install verification pending.
- DF-006: CI remote runner verification pending.
- Launcher3 (LDPlayer) does not support `requestPinAppWidget` — needs
  verification on real device launcher.
- Physical device validation needed for reminders, keystore, and
  biometric integration.

---

*Version 0.1.0-beta — Daily Flow P8 release preparation.*
